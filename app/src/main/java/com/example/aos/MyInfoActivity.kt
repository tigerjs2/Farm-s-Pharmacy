package com.example.aos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class MyInfoActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var uid: String = ""
    private lateinit var ivProfileImage: ImageView
    private lateinit var etName: EditText

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            ivProfileImage.setImageURI(it)

            val storageRef = Firebase.storage.reference
                .child("profile_images/$uid.jpg")

            storageRef.putFile(it)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        Firebase.firestore.collection("Users").document(uid)
                            .update("profileImageUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                Toast.makeText(this, "프로필 사진이 변경되었습니다", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "업로드 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_info)

        mAuth = FirebaseAuth.getInstance()
        uid = mAuth.currentUser?.uid ?: return
        ivProfileImage = findViewById(R.id.ivProfileImage)
        etName = findViewById(R.id.etName)

        // 이메일 표시
        findViewById<TextView>(R.id.tvEmail).text = mAuth.currentUser?.email ?: ""

        // Firestore에서 이름 + 프로필 이미지 불러오기
        Firebase.firestore.collection("Users").document(uid).get()
            .addOnSuccessListener { document ->
                etName.setText(document.getString("name") ?: "")

                val imageUrl = document.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(imageUrl).circleCrop().into(ivProfileImage)
                }
            }

        setupNameEdit()

        // 프로필 사진 변경
        findViewById<TextView>(R.id.tvChangePhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 비밀번호 변경
        findViewById<TextView>(R.id.tvChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun setupNameEdit() {
        etName.isFocusableInTouchMode = false

        etName.setOnClickListener {
            etName.isFocusableInTouchMode = true
            etName.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etName, InputMethodManager.SHOW_IMPLICIT)
        }

        etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                saveName()
                true
            } else false
        }

        etName.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                event.action == android.view.KeyEvent.ACTION_DOWN) {
                saveName()
                true
            } else false
        }
    }

    private fun saveName() {
        val newName = etName.text.toString()
        Firebase.firestore.collection("Users").document(uid)
            .update("name", newName)
            .addOnSuccessListener {
                Toast.makeText(this, "이름이 변경되었습니다", Toast.LENGTH_SHORT).show()
                etName.isFocusableInTouchMode = false
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etName.windowToken, 0)
            }
    }

    private fun showChangePasswordDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val etCurrent = view.findViewById<EditText>(R.id.etCurrentPassword)
        val etNew = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmPassword)
        val btnChange = view.findViewById<TextView>(R.id.btnChange)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnChange.setOnClickListener {
            val current = etCurrent.text.toString()
            val new = etNew.text.toString()
            val confirm = etConfirm.text.toString()

            if (new != confirm) {
                Toast.makeText(this, "새 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = mAuth.currentUser ?: return@setOnClickListener
            val credential = EmailAuthProvider.getCredential(user.email!!, current)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(new)
                        .addOnSuccessListener {
                            Toast.makeText(this, "비밀번호가 변경되었습니다", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "변경 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "현재 비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }
}