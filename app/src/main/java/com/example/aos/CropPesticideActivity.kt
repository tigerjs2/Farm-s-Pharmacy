package com.example.aos

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CropPesticideActivity : AppCompatActivity() {

    private val cropDiseaseMap = mapOf(
        "오이" to listOf("노균병", "흰가루병", "기타"),
        "딸기" to listOf("흰가루병", "잿빛곰팡이병", "기타"),
        "포도" to listOf("노균병", "흰가루병", "기타"),
        "파프리카" to listOf("흰가루병", "잿빛곰팡이병", "기타"),
        "고추" to listOf("흰가루병", "탄저병", "기타"),
        "토마토" to listOf("흰가루병", "잿빛곰팡이병", "기타")
    )

    private lateinit var cropName: String
    private lateinit var diseases: List<String>

    private lateinit var container1: LinearLayout
    private lateinit var container2: LinearLayout
    private lateinit var container3: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_pesticide)

        cropName = intent.getStringExtra("cropName") ?: "딸기"
        diseases = cropDiseaseMap[cropName] ?: listOf("노균병", "흰가루병", "기타")

        findViewById<TextView>(R.id.tvTitle).text = "“$cropName” 등록 농약 안내"

        findViewById<TextView>(R.id.tvDiseaseName1).text = diseases[0]
        findViewById<TextView>(R.id.tvDiseaseName2).text = diseases[1]
        findViewById<TextView>(R.id.tvDiseaseName3).text = diseases[2]

        container1 = findViewById(R.id.pesticideContainer)
        container2 = findViewById(R.id.pesticideContainer2)
        container3 = findViewById(R.id.pesticideContainer3)

        setupAccordion(
            header = findViewById(R.id.diseaseHeader),
            contentArea = findViewById(R.id.diseaseContentArea),
            arrow = findViewById(R.id.tvArrow),
            defaultExpanded = true
        )

        setupAccordion(
            header = findViewById(R.id.diseaseHeader2),
            contentArea = findViewById(R.id.diseaseContentArea2),
            arrow = findViewById(R.id.tvArrow2),
            defaultExpanded = false
        )

        setupAccordion(
            header = findViewById(R.id.diseaseHeader3),
            contentArea = findViewById(R.id.diseaseContentArea3),
            arrow = findViewById(R.id.tvArrow3),
            defaultExpanded = false
        )

        loadData()
    }

    private fun loadData() {

        showLoading(container1)
        showLoading(container2)
        showLoading(container3)

        lifecycleScope.launch {

            val items = withContext(Dispatchers.IO) {
                PesticideApiService.searchByCrop(cropName)
            }

            val group1 = items.filter { it.applcDbyhs == diseases[0] }
            val group2 = items.filter { it.applcDbyhs == diseases[1] }
            val group3 = items.filter {
                it.applcDbyhs != diseases[0] && it.applcDbyhs != diseases[1]
            }

            renderInto(container1, group1)
            renderInto(container2, group2)
            renderInto(container3, group3)
        }
    }

    private fun renderInto(container: LinearLayout, items: List<Pesticide>) {

        container.removeAllViews()

        if (items.isEmpty()) {
            val tv = TextView(this).apply {
                text = "관련 농약 정보가 없어요."
                textSize = 12f
                setTextColor(getColor(R.color.gray))
                setPadding(14, 8, 14, 8)
            }
            container.addView(tv)
            return
        }

        // 같은 상표명+품목명으로 묶기
        val grouped = items
            .groupBy { it.brandNm to it.prdlstNm }
            .map { (_, list) -> list.first() }

        grouped.forEach { item ->
            val view = layoutInflater.inflate(
                R.layout.item_pesticide_card,
                container,
                false
            )

            view.findViewById<TextView>(R.id.tvProductName).text =
                item.brandNm.ifBlank { "-" }
            view.findViewById<TextView>(R.id.tvCategory).text =
                "구분: ${item.prpos.ifBlank { "-" }}"
            view.findViewById<TextView>(R.id.tvUsage).text =
                "회사: ${item.cmpnyNm.ifBlank { "-" }}"
            view.findViewById<TextView>(R.id.tvStandard).text =
                "기준: ${item.dcsnAt.ifBlank { "-" }}"
            view.findViewById<TextView>(R.id.tvInterval).text =
                "작용기작: ${item.actnNm.ifBlank { "-" }}"
            view.findViewById<TextView>(R.id.tvTiming).text =
                item.prdlstNm

            container.addView(view)
        }
    }

    private fun showLoading(container: LinearLayout) {
        container.removeAllViews()
        val tv = TextView(this).apply {
            text = "불러오는 중..."
            textSize = 12f
            setTextColor(getColor(R.color.gray))
            setPadding(14, 8, 14, 8)
        }
        container.addView(tv)
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
}
