package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        mAuth = FirebaseAuth.getInstance()

        signUpButton.setOnClickListener {

            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()

            if (password.length < 6) {
                Toast.makeText(this, "비밀번호는 6자리 이상", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일/비밀번호 입력", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val confirmPassword = findViewById<EditText>(R.id.confirmPasswordEditText).text.toString()

            if (password != confirmPassword) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        setupLoginText(loginText)
    }

    private fun setupLoginText(textView: TextView) {
        val fullText = "이미 계정이 있으신가요? 로그인"
        val spannable = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                finish()
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@SignUpActivity, R.color.primary_green)
                ds.isUnderlineText = false
            }
        }

        val start = fullText.indexOf("로그인")
        spannable.setSpan(clickableSpan, start, start + "로그인".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}