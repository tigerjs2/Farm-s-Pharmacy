package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var tvProfileName: TextView
    private lateinit var ivProfileImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        mAuth = FirebaseAuth.getInstance()

        tvProfileName = findViewById(R.id.tvProfileName)
        ivProfileImage = findViewById(R.id.ivProfileImage)

        loadUserProfile()

        findViewById<TextView>(R.id.tvMyInfo).setOnClickListener {
            startActivity(Intent(this, MyInfoActivity::class.java))
        }

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            mAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = mAuth.currentUser?.uid ?: return

        Firebase.firestore
            .collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: ""
                tvProfileName.text = name

                val imageUrl = document.getString("profileImageUrl")
                val updatedAt = document.getTimestamp("profileImageUpdatedAt")
                    ?.toDate()
                    ?.time
                    ?: 0L

                if (!imageUrl.isNullOrBlank()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .signature(ObjectKey("${imageUrl}_${updatedAt}"))
                        .into(ivProfileImage)
                } else {
                    ivProfileImage.setImageResource(R.drawable.ic_profile)
                }
            }
            .addOnFailureListener {
                tvProfileName.text = ""
                ivProfileImage.setImageResource(R.drawable.ic_profile)
            }
    }
}