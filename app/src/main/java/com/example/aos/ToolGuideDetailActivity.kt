package com.example.aos

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ToolGuideDetailActivity : AppCompatActivity() {

    data class SafetyCard(
        val title: String,
        val imageResId: Int,
        val bullets: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool_guide_detail)

        val toolName = intent.getStringExtra("toolName") ?: "방제기"

        findViewById<TextView>(R.id.tvToolTitle).text = toolName

        val container = findViewById<LinearLayout>(R.id.cardContainer)

        val dummyCards = getDummyCards(toolName)

        dummyCards.forEach { card ->
            val cardView = layoutInflater.inflate(
                R.layout.item_tool_safety_card,
                container,
                false
            )

            cardView.findViewById<TextView>(R.id.tvCardTitle).text = card.title
            cardView.findViewById<ImageView>(R.id.ivCardImage).setImageResource(card.imageResId)

            val bulletContainer = cardView.findViewById<LinearLayout>(R.id.bulletContainer)
            bulletContainer.removeAllViews()

            card.bullets.forEach { text ->
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

    private fun getDummyCards(toolName: String): List<SafetyCard> {
        return when (toolName) {
            "방제기" -> listOf(
                SafetyCard(
                    title = "과수원 농약살포시에는 SS기의 전방 진행상황에 주의합시다.",
                    imageResId = R.drawable.img_safety_ss,
                    bullets = listOf(
                        "과수원은 잘 관리하며 농작업 시 위험요소를 없앤다.",
                        "후방의 살포상태만 신경쓰지 말고 진행방향의 안전을 확인한다.",
                        "진행 중 길 가장자리에서 추락, 과수와 충돌, 가지에 걸리지 않도록 주의한다."
                    )
                ),
                SafetyCard(
                    title = "농약살포시에는 적절한 보호도구를 착용합시다.",
                    imageResId = R.drawable.img_safety_sprayer,
                    bullets = listOf(
                        "작업복은 피부를 노출시키지 않는 방제전용 작업복을 착용한다.",
                        "마스크, 장갑, 보호안경 등을 착용한다.",
                        "마스크 대신 손수건을 사용하는 것은 효과가 없다."
                    )
                ),
                SafetyCard(
                    title = "방제작업 전날은 피로회복을 위해 충분한 수면을 취합시다.",
                    imageResId = R.drawable.img_safety_sleep,
                    bullets = listOf(
                        "피로한 상태에서 농약살포 작업을 하지 않는다.",
                        "작업 중 어지러움이나 두통이 있으면 즉시 작업을 중단한다.",
                        "작업 후에는 깨끗하게 씻고 오염된 작업복은 분리 세탁한다."
                    )
                )
            )

            else -> listOf(
                SafetyCard(
                    title = "$toolName 사용 전 안전상태를 확인합시다.",
                    imageResId = R.drawable.img_safety_default,
                    bullets = listOf(
                        "작업 전 장비의 이상 여부를 확인한다.",
                        "작업 중 주변 사람과 충분한 거리를 유지한다.",
                        "사용 후에는 장비를 깨끗하게 정리한다."
                    )
                )
            )
        }
    }
}