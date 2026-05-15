package com.example.aos

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ToolGuideDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOOL_NAME = "toolName"
        const val EXTRA_FOCUS_CNTNTS_NO = "focusCntntsNo"
        const val EXTRA_FALLBACK_CNTNTS_NO = "fallbackCntntsNo"
        const val EXTRA_FALLBACK_TITLE = "fallbackTitle"
        const val EXTRA_FALLBACK_CONTENT = "fallbackContent"
        const val EXTRA_FALLBACK_KNMC_NM = "fallbackKnmcNm"
        const val EXTRA_FALLBACK_SAFEACDNT_SE_NM = "fallbackSafeacdntSeNm"
        const val EXTRA_FALLBACK_IMAGE_URL = "fallbackImageUrl"
    }

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool_guide_detail)

        val fallbackGuide = readFallbackGuide()
        val toolName = intent.getStringExtra(EXTRA_TOOL_NAME)
            ?: fallbackGuide?.knmcNm
            ?: "방제기"
        val focusCntntsNo = intent.getStringExtra(EXTRA_FOCUS_CNTNTS_NO) ?: ""

        findViewById<TextView>(R.id.tvToolTitle).text = toolName
        container = findViewById(R.id.cardContainer)

        loadGuides(
            toolName = toolName,
            focusCntntsNo = focusCntntsNo,
            fallbackGuide = fallbackGuide
        )
    }

    private fun loadGuides(
        toolName: String,
        focusCntntsNo: String,
        fallbackGuide: ToolGuide?
    ) {
        showStatus("불러오는 중...")

        lifecycleScope.launch {
            val guides = withContext(Dispatchers.IO) {
                NongsaroApiService.getToolGuides(toolName)
            }

            val displayGuides = mergeGuides(
                guides = guides,
                fallbackGuide = fallbackGuide,
                focusCntntsNo = focusCntntsNo
            )

            container.removeAllViews()

            if (displayGuides.isEmpty()) {
                showStatus("관련 안전 지침을 찾지 못했어요.")
                return@launch
            }

            renderCards(displayGuides)
        }
    }

    private fun mergeGuides(
        guides: List<ToolGuide>,
        fallbackGuide: ToolGuide?,
        focusCntntsNo: String
    ): List<ToolGuide> {
        val merged = mutableListOf<ToolGuide>()

        if (fallbackGuide != null && guides.none { it.cntntsNo == fallbackGuide.cntntsNo }) {
            merged.add(fallbackGuide)
        }

        merged.addAll(guides)

        if (focusCntntsNo.isBlank()) return merged

        return merged.sortedWith(
            compareBy<ToolGuide> { if (it.cntntsNo == focusCntntsNo) 0 else 1 }
        )
    }

    private fun renderCards(guides: List<ToolGuide>) {
        guides.forEach { guide ->
            val cardView = layoutInflater.inflate(
                R.layout.item_tool_safety_card,
                container,
                false
            )

            cardView.findViewById<TextView>(R.id.tvCardTitle).text =
                guide.title.ifBlank { guide.safeacdntSeNm.ifBlank { "안전 지침" } }

            val ivImage = cardView.findViewById<ImageView>(R.id.ivCardImage)
            if (guide.imageUrl.isNotBlank()) {
                ivImage.visibility = View.VISIBLE
                Glide.with(this)
                    .load(guide.imageUrl)
                    .fitCenter()
                    .into(ivImage)
            } else {
                ivImage.visibility = View.GONE
                ivImage.setImageDrawable(null)
            }

            val bulletContainer = cardView.findViewById<LinearLayout>(R.id.bulletContainer)
            bulletContainer.removeAllViews()

            val bullets = TodaySafetyRepository
                .splitBullets(guide.content)
                .ifEmpty { listOf("안전 지침 내용을 확인해주세요.") }

            bullets.forEach { text ->
                val bullet = TextView(this).apply {
                    this.text = "· $text"
                    textSize = 13f
                    setTextColor(getColor(R.color.black))
                    typeface = ResourcesCompat.getFont(this@ToolGuideDetailActivity, R.font.paperlogy_3light)
                    setPadding(0, 8, 0, 0)
                }
                bulletContainer.addView(bullet)
            }

            container.addView(cardView)
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

    private fun readFallbackGuide(): ToolGuide? {
        val title = intent.getStringExtra(EXTRA_FALLBACK_TITLE) ?: ""
        val content = intent.getStringExtra(EXTRA_FALLBACK_CONTENT) ?: ""
        val imageUrl = intent.getStringExtra(EXTRA_FALLBACK_IMAGE_URL) ?: ""

        if (title.isBlank() && content.isBlank() && imageUrl.isBlank()) return null

        return ToolGuide(
            cntntsNo = intent.getStringExtra(EXTRA_FALLBACK_CNTNTS_NO) ?: "",
            title = title,
            content = content,
            knmcNm = intent.getStringExtra(EXTRA_FALLBACK_KNMC_NM) ?: "",
            safeacdntSeNm = intent.getStringExtra(EXTRA_FALLBACK_SAFEACDNT_SE_NM) ?: "",
            imageUrl = imageUrl
        )
    }
}
