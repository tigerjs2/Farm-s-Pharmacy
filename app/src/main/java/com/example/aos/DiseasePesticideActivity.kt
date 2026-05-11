package com.example.aos

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DiseasePesticideActivity : AppCompatActivity() {

    data class DiseasePesticideInfo(
        val productName: String,
        val itemName: String,
        val category: String,
        val appliedCrops: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_pesticide)

        val diseaseName = intent.getStringExtra("searchKeyword") ?: "병해명"

        findViewById<TextView>(R.id.tvDiseasePesticideTitle).text =
            "“$diseaseName” 방제 정보"

        val container = findViewById<LinearLayout>(R.id.diseasePesticideContainer)

        addDummyCards(container, diseaseName)
    }

    private fun addDummyCards(container: LinearLayout, diseaseName: String) {
        container.removeAllViews()

        val dummyList = listOf(
            DiseasePesticideInfo(
                productName = "상표명",
                itemName = "품목명",
                category = "살균제  수화제(WP)",
                appliedCrops = "포도-$diseaseName     오이-$diseaseName     감자-$diseaseName"
            ),
            DiseasePesticideInfo(
                productName = "상표명",
                itemName = "품목명",
                category = "살균제  수화제(WP)",
                appliedCrops = "포도-$diseaseName     오이-$diseaseName     감자-$diseaseName"
            ),
            DiseasePesticideInfo(
                productName = "상표명",
                itemName = "품목명",
                category = "살균제  수화제(WP)",
                appliedCrops = "포도-$diseaseName     오이-$diseaseName     감자-$diseaseName"
            ),
            DiseasePesticideInfo(
                productName = "상표명",
                itemName = "품목명",
                category = "살균제  수화제(WP)",
                appliedCrops = "포도-$diseaseName     오이-$diseaseName     감자-$diseaseName"
            )
        )

        dummyList.forEach { item ->
            val view = layoutInflater.inflate(
                R.layout.item_disease_pesticide_card,
                container,
                false
            )

            view.findViewById<TextView>(R.id.tvDiseaseProductName).text = item.productName
            view.findViewById<TextView>(R.id.tvDiseaseItemName).text = item.itemName
            view.findViewById<TextView>(R.id.tvDiseaseCategory).text = "구분:   ${item.category}"
            view.findViewById<TextView>(R.id.tvAppliedCrops).text = item.appliedCrops

            container.addView(view)
        }
    }
}