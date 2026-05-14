package com.example.aos

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ToolGuideDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool_guide_detail)

        val toolName = intent.getStringExtra("toolName") ?: "방제기"

        findViewById<TextView>(R.id.tvToolTitle).text = toolName

        loadGuides(toolName)
    }

    private fun loadGuides(toolName: String) {

        val container = findViewById<LinearLayout>(R.id.cardContainer)

        // 로딩 placeholder
        container.removeAllViews()
        val loading = TextView(this).apply {
            text = "불러오는 중..."
            textSize = 14f
            setTextColor(getColor(R.color.gray))
        }
        container.addView(loading)

        lifecycleScope.launch {

            val guides = withContext(Dispatchers.IO) {
                NongsaroApiService.getToolGuides(toolName)
            }

            container.removeAllViews()

            if (guides.isEmpty()) {
                val empty = TextView(this@ToolGuideDetailActivity).apply {
                    text = "관련 안전 지침을 찾지 못했어요."
                    textSize = 14f
                    setTextColor(getColor(R.color.gray))
                }
                container.addView(empty)
                return@launch
            }

            renderCards(container, guides)
        }
    }

    private fun renderCards(container: LinearLayout, guides: List<ToolGuide>) {

        guides.forEach { guide ->

            val cardView = layoutInflater.inflate(
                R.layout.item_tool_safety_card,
                container,
                false
            )

            cardView.findViewById<TextView>(R.id.tvCardTitle).text =
                if (guide.title.isNotBlank()) guide.title else guide.safeacdntSeNm

            val ivImage = cardView.findViewById<ImageView>(R.id.ivCardImage)
            if (guide.imageUrl.isNotBlank()) {
                ivImage.visibility = View.VISIBLE
                Glide.with(this)
                    .load(guide.imageUrl)
                    .into(ivImage)
            } else {
                ivImage.visibility = View.GONE
            }

            val bulletContainer = cardView.findViewById<LinearLayout>(R.id.bulletContainer)
            bulletContainer.removeAllViews()

            extractBullets(guide.content).forEach { text ->
                val bullet = TextView(this).apply {
                    this.text = "· $text"
                    textSize = 13f
                    setTextColor(getColor(R.color.black))
                    typeface = resources.getFont(R.font.paperlogy_3light)
                    setPadding(0, 8, 0, 0)
                }
                bulletContainer.addView(bullet)
            }

            container.addView(cardView)
        }
    }

    // HTML 태그 제거 + 줄 단위 분할 (빈 줄 제거)
    private fun extractBullets(content: String): List<String> {

        if (content.isBlank()) return emptyList()

        val normalized = content
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("</p\\s*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")

        return normalized.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
