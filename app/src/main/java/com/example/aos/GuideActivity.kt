package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class GuideActivity : AppCompatActivity() {

    private lateinit var guideImage: ImageView

    private val guideImages = intArrayOf(
        R.drawable.guide_1,
        R.drawable.guide_2,
        R.drawable.guide_3,
        R.drawable.guide_4,
        R.drawable.guide_5
    )

    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        guideImage = findViewById(R.id.guideImage)

        guideImage.setImageResource(guideImages[currentIndex])

        guideImage.setOnClickListener {
            if (currentIndex < guideImages.lastIndex) {
                currentIndex++
                guideImage.setImageResource(guideImages[currentIndex])
            } else {
                goToMain()
            }
        }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}