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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupText = findViewById<TextView>(R.id.signupText)
        val titleText = findViewById<TextView>(R.id.titleText)

        setupTitleText(titleText)
        setupSignupText(signupText)

        loginButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupTitleText(textView: TextView) {
        val fullText = "농장의 파머씨"
        val spannable = SpannableString(fullText)

        val greenColor = ContextCompat.getColor(this, R.color.primary_green)
        val lightColor = ContextCompat.getColor(this, R.color.primary_light)

        val start = fullText.indexOf("파머")

        spannable.setSpan(ForegroundColorSpan(lightColor), 0, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(greenColor), start, start + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(lightColor), start + 2, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannable
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
                ds.color = ContextCompat.getColor(this@LoginActivity, R.color.primary_light)
                ds.isUnderlineText = false
            }
        }

        val start = fullText.indexOf("회원가입")
        spannable.setSpan(clickableSpan, start, start + "회원가입".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}