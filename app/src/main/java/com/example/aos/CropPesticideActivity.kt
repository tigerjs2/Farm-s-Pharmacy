package com.example.aos

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CropPesticideActivity : AppCompatActivity() {

    data class PesticideInfo(
        val productName: String,
        val category: String,
        val usage: String,
        val standard: String,
        val interval: String,
        val timing: String
    )

    private val cropDiseaseMap = mapOf(
        "오이" to listOf("노균병", "흰가루병", "기타"),
        "딸기" to listOf("흰가루병", "잿빛곰팡이병", "기타"),
        "포도" to listOf("노균병", "흰가루병", "기타"),
        "파프리카" to listOf("흰가루병", "잿빛곰팡이병", "기타"),
        "고추" to listOf("흰가루병", "탄저병", "기타"),
        "토마토" to listOf("흰가루병", "잿빛곰팡이병", "기타")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_pesticide)

        val cropName = intent.getStringExtra("cropName") ?: "딸기"
        val diseases = cropDiseaseMap[cropName] ?: listOf("노균병", "흰가루병", "기타")

        findViewById<TextView>(R.id.tvTitle).text = "“$cropName” 등록 농약 안내"

        val header1 = findViewById<LinearLayout>(R.id.diseaseHeader)
        val header2 = findViewById<LinearLayout>(R.id.diseaseHeader2)
        val header3 = findViewById<LinearLayout>(R.id.diseaseHeader3)

        val contentArea1 = findViewById<LinearLayout>(R.id.diseaseContentArea)
        val contentArea2 = findViewById<LinearLayout>(R.id.diseaseContentArea2)
        val contentArea3 = findViewById<LinearLayout>(R.id.diseaseContentArea3)

        val container1 = findViewById<LinearLayout>(R.id.pesticideContainer)
        val container2 = findViewById<LinearLayout>(R.id.pesticideContainer2)
        val container3 = findViewById<LinearLayout>(R.id.pesticideContainer3)

        val arrow1 = findViewById<TextView>(R.id.tvArrow)
        val arrow2 = findViewById<TextView>(R.id.tvArrow2)
        val arrow3 = findViewById<TextView>(R.id.tvArrow3)

        val diseaseName1 = findViewById<TextView>(R.id.tvDiseaseName1)
        val diseaseName2 = findViewById<TextView>(R.id.tvDiseaseName2)
        val diseaseName3 = findViewById<TextView>(R.id.tvDiseaseName3)

        diseaseName1.text = diseases[0]
        diseaseName2.text = diseases[1]
        diseaseName3.text = diseases[2]

        addDummyCards(container1, diseases[0])
        addDummyCards(container2, diseases[1])
        addDummyCards(container3, diseases[2])

        setupAccordion(
            header = header1,
            contentArea = contentArea1,
            arrow = arrow1,
            defaultExpanded = true
        )

        setupAccordion(
            header = header2,
            contentArea = contentArea2,
            arrow = arrow2,
            defaultExpanded = false
        )

        setupAccordion(
            header = header3,
            contentArea = contentArea3,
            arrow = arrow3,
            defaultExpanded = false
        )
    }

    private fun setupAccordion(
        header: LinearLayout,
        contentArea: View,
        arrow: TextView,
        defaultExpanded: Boolean
    ) {
        var isExpanded = defaultExpanded

        contentArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
        arrow.text = if (isExpanded) "⌃" else "⌄"

        header.setOnClickListener {
            isExpanded = !isExpanded
            contentArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
            arrow.text = if (isExpanded) "⌃" else "⌄"
        }
    }

    private fun addDummyCards(container: LinearLayout, diseaseName: String) {
        container.removeAllViews()

        val dummyList = listOf(
            PesticideInfo(
                productName = "상표명",
                category = "살균제  수화제(WP)",
                usage = "사용 적기 및 방법",
                standard = "안전사용기준",
                interval = "발병 초기 살포",
                timing = "수확 3일 전"
            ),
            PesticideInfo(
                productName = "상표명",
                category = "살균제  수화제(WP)",
                usage = "사용 적기 및 방법",
                standard = "안전사용기준",
                interval = "발병 초기 살포",
                timing = "수확 3일 전"
            )
        )

        dummyList.forEach { item ->
            val view = layoutInflater.inflate(
                R.layout.item_pesticide_card,
                container,
                false
            )

            view.findViewById<TextView>(R.id.tvProductName).text = item.productName
            view.findViewById<TextView>(R.id.tvCategory).text = "구분: ${item.category}"
            view.findViewById<TextView>(R.id.tvUsage).text = item.usage
            view.findViewById<TextView>(R.id.tvStandard).text = item.standard
            view.findViewById<TextView>(R.id.tvInterval).text = item.interval
            view.findViewById<TextView>(R.id.tvTiming).text = item.timing

            container.addView(view)
        }
    }
}