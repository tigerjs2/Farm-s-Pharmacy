package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupText = findViewById<TextView>(R.id.signupText)

        mAuth = FirebaseAuth.getInstance()

        loginButton.setOnClickListener {

            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일/비밀번호 입력", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // 회원가입 텍스트 클릭
        setupSignupText(signupText)
    }

    private fun setupSignupText(textView: TextView) {
        val fullText = "계정이 없으신가요? 회원가입"
        val spannable = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@LoginActivity, R.color.primary_green)
                ds.isUnderlineText = false
            }
        }

        val start = fullText.indexOf("회원가입")
        spannable.setSpan(clickableSpan, start, start + "회원가입".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}