package com.example.aos

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiseasePesticideActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var diseaseName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disease_pesticide)

        diseaseName = intent.getStringExtra("searchKeyword") ?: "병해명"

        findViewById<TextView>(R.id.tvDiseasePesticideTitle).text =
            "“$diseaseName” 방제 정보"

        container = findViewById(R.id.diseasePesticideContainer)

        loadData()
    }

    private fun loadData() {

        showStatus("불러오는 중... (최대 30초)")

        lifecycleScope.launch {

            val items = withContext(Dispatchers.IO) {
                PesticideApiService.searchByDisease(diseaseName)
            }

            if (items.isEmpty()) {
                showStatus("관련 농약 정보를 찾지 못했어요.")
                return@launch
            }

            renderCards(groupByProduct(items))
        }
    }

    private fun showStatus(message: String) {
        container.removeAllViews()
        val tv = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(getColor(R.color.gray))
        }
        container.addView(tv)
    }

    // 같은 상표명+품목명으로 묶고 적용 작물·병해를 합침
    private fun groupByProduct(items: List<Pesticide>): List<GroupedProduct> {
        return items
            .groupBy { it.brandNm to it.prdlstNm }
            .map { (key, list) ->
                val first = list.first()
                val applied = list
                    .map { "${it.cropsNm}-${it.applcDbyhs}" }
                    .distinct()
                    .joinToString("    ")
                GroupedProduct(
                    brandNm = key.first,
                    prdlstNm = key.second,
                    prpos = first.prpos,
                    appliedCrops = applied
                )
            }
    }

    private fun renderCards(products: List<GroupedProduct>) {

        container.removeAllViews()

        products.forEach { p ->

            val view = layoutInflater.inflate(
                R.layout.item_disease_pesticide_card,
                container,
                false
            )

            view.findViewById<TextView>(R.id.tvDiseaseProductName).text =
                p.brandNm.ifBlank { "-" }
            view.findViewById<TextView>(R.id.tvDiseaseItemName).text =
                p.prdlstNm.ifBlank { "-" }
            view.findViewById<TextView>(R.id.tvDiseaseCategory).text =
                "구분:   ${p.prpos.ifBlank { "-" }}"
            view.findViewById<TextView>(R.id.tvAppliedCrops).text = p.appliedCrops

            container.addView(view)
        }
    }

    private data class GroupedProduct(
        val brandNm: String,
        val prdlstNm: String,
        val prpos: String,
        val appliedCrops: String
    )
}
