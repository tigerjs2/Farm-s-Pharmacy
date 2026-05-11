package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PesticideGuideActivity : AppCompatActivity() {

    private val crops = listOf("오이", "딸기", "포도", "파프리카", "고추", "토마토")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pesticide_guide)

        val cropButtons = listOf(
            R.id.btnCucumber to "오이",
            R.id.btnStrawberry to "딸기",
            R.id.btnGrape to "포도",
            R.id.btnPaprika to "파프리카",
            R.id.btnPepper to "고추",
            R.id.btnTomato to "토마토"
        )

        cropButtons.forEach { (buttonId, cropName) ->
            findViewById<LinearLayout>(buttonId).setOnClickListener {
                val intent = Intent(this, CropPesticideActivity::class.java).apply {
                    putExtra("mode", "crop")
                    putExtra("cropName", cropName)
                }
                startActivity(intent)
            }
        }

        val searchEditText = findViewById<EditText>(R.id.etDiseaseSearch)
        val searchButton = findViewById<ImageView>(R.id.btnSearchDisease)

        fun goSearchResult() {
            val keyword = searchEditText.text.toString().trim()

            if (keyword.isEmpty()) {
                Toast.makeText(this, "병해명을 입력해주세요", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(this, DiseasePesticideActivity::class.java).apply {
                putExtra("searchKeyword", keyword)
            }

            startActivity(intent)
        }

        searchButton.setOnClickListener {
            goSearchResult()
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                goSearchResult()
                true
            } else {
                false
            }
        }
    }
}