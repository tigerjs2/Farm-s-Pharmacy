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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        signUpButton.setOnClickListener {
            // 나중에 Firebase 연동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
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
                ds.color = ContextCompat.getColor(this@SignUpActivity, R.color.primary_light)
                ds.isUnderlineText = false
            }
        }

        val start = fullText.indexOf("로그인")
        spannable.setSpan(clickableSpan, start, start + "로그인".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}