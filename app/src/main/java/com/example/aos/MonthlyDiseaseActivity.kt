package com.example.aos

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

class MonthlyDiseaseActivity : AppCompatActivity() {

    private lateinit var tvMonthlyCommonTab: TextView
    private lateinit var tvCropDetailTab: TextView
    private lateinit var monthlyScrollView: ScrollView
    private lateinit var cropScrollView: ScrollView
    private lateinit var monthDiseaseContainer: LinearLayout
    private lateinit var cropDiseaseContainer: LinearLayout

    private val paperFont by lazy {
        ResourcesCompat.getFont(this, R.font.paperlogy_3light)
    }

    private data class MonthCommonDisease(
        val month: Int,
        val season: String,
        val diseases: List<String>
    )

    private data class CropDiseaseRow(
        val cropName: String,
        val diseases: List<String>
    )

    private data class MonthlyCropDisease(
        val month: Int,
        val rows: List<CropDiseaseRow>
    )

    private val commonDiseaseData = listOf(
        MonthCommonDisease(1, "겨울", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(2, "겨울", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(3, "봄", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(4, "봄", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(5, "봄", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(6, "여름", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(7, "여름", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(8, "여름", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(9, "가을", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(10, "가을", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(11, "가을", listOf("잿빛곰팡이병", "노균병")),
        MonthCommonDisease(12, "겨울", listOf("잿빛곰팡이병", "노균병"))
    )

    private val sampleCropRows = listOf(
        CropDiseaseRow("딸기", listOf("잿빛곰팡이병", "흰가루병")),
        CropDiseaseRow("오이", listOf("잿빛곰팡이병", "흰가루병")),
        CropDiseaseRow("토마토", listOf("잿빛곰팡이병", "흰가루병")),
        CropDiseaseRow("고추", listOf("잿빛곰팡이병", "흰가루병")),
        CropDiseaseRow("파프리카", listOf("잿빛곰팡이병", "흰가루병")),
        CropDiseaseRow("포도", listOf("잿빛곰팡이병", "흰가루병"))
    )

    private val cropDiseaseData = (1..12).map {
        MonthlyCropDisease(it, sampleCropRows)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_disease)

        bindViews()
        buildMonthlyCards()
        buildCropAccordions()

        setMode(isMonthlyMode = true)
    }

    private fun bindViews() {
        tvMonthlyCommonTab = findViewById(R.id.tvMonthlyCommonTab)
        tvCropDetailTab = findViewById(R.id.tvCropDetailTab)
        monthlyScrollView = findViewById(R.id.monthlyScrollView)
        cropScrollView = findViewById(R.id.cropScrollView)
        monthDiseaseContainer = findViewById(R.id.monthDiseaseContainer)
        cropDiseaseContainer = findViewById(R.id.cropDiseaseContainer)

        tvMonthlyCommonTab.setOnClickListener {
            setMode(isMonthlyMode = true)
        }

        tvCropDetailTab.setOnClickListener {
            setMode(isMonthlyMode = false)
        }
    }

    private fun setMode(isMonthlyMode: Boolean) {
        monthlyScrollView.visibility = if (isMonthlyMode) View.VISIBLE else View.GONE
        cropScrollView.visibility = if (isMonthlyMode) View.GONE else View.VISIBLE

        tvMonthlyCommonTab.setBackgroundResource(
            if (isMonthlyMode) R.drawable.bg_monthly_tab_selected
            else R.drawable.bg_monthly_tab_unselected
        )
        tvMonthlyCommonTab.setTextColor(
            if (isMonthlyMode) Color.WHITE
            else Color.parseColor("#777777")
        )

        tvCropDetailTab.setBackgroundResource(
            if (isMonthlyMode) R.drawable.bg_monthly_tab_unselected
            else R.drawable.bg_monthly_tab_selected
        )
        tvCropDetailTab.setTextColor(
            if (isMonthlyMode) Color.parseColor("#777777")
            else Color.WHITE
        )
    }

    private fun buildMonthlyCards() {
        monthDiseaseContainer.removeAllViews()

        commonDiseaseData.chunked(2).forEach { rowItems ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            rowItems.forEachIndexed { index, item ->
                val card = createMonthCard(item)

                card.layoutParams = LinearLayout.LayoutParams(
                    0,
                    102.dp(),
                    1f
                ).apply {
                    val left = if (index == 0) 0 else 6.dp()
                    val right = if (index == 0) 6.dp() else 0
                    setMargins(left, 0, right, 12.dp())
                }

                rowLayout.addView(card)
            }

            if (rowItems.size == 1) {
                rowLayout.addView(Space(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        102.dp(),
                        1f
                    )
                })
            }

            monthDiseaseContainer.addView(rowLayout)
        }
    }

    private fun createMonthCard(info: MonthCommonDisease): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(17.dp(), 13.dp(), 13.dp(), 10.dp())
            background = ContextCompat.getDrawable(this@MonthlyDiseaseActivity, R.drawable.bg_month_disease_card)
            elevation = 1.5f.dpFloat()

            val header = LinearLayout(this@MonthlyDiseaseActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            header.addView(TextView(this@MonthlyDiseaseActivity).apply {
                text = "${info.month}월"
                applyPaper(sizeSp = 16f, color = Color.BLACK, bold = true)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })

            header.addView(TextView(this@MonthlyDiseaseActivity).apply {
                text = info.season
                applyPaper(sizeSp = 9f, color = Color.BLACK, bold = false)
            })

            addView(header)

            addView(View(this@MonthlyDiseaseActivity).apply {
                setBackgroundColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dp()
                ).apply {
                    topMargin = 8.dp()
                    bottomMargin = 7.dp()
                }
            })

            info.diseases.forEach { disease ->
                addView(TextView(this@MonthlyDiseaseActivity).apply {
                    text = "· $disease"
                    applyPaper(sizeSp = 10.5f, color = Color.BLACK, bold = false)
                    setPadding(0, 0, 0, 4.dp())
                })
            }
        }
    }

    private fun buildCropAccordions() {
        cropDiseaseContainer.removeAllViews()

        cropDiseaseData.forEachIndexed { index, item ->
            cropDiseaseContainer.addView(
                createAccordion(
                    info = item,
                    defaultOpen = index == 0
                )
            )
        }
    }

    private fun createAccordion(
        info: MonthlyCropDisease,
        defaultOpen: Boolean
    ): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@MonthlyDiseaseActivity, R.drawable.bg_month_accordion_card)
            elevation = 2f.dpFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dp()
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20.dp(), 0, 14.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                52.dp()
            )
        }

        val title = TextView(this).apply {
            text = "${info.month}월 주요 병해"
            applyPaper(sizeSp = 15.5f, color = Color.BLACK, bold = true)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val arrow = TextView(this).apply {
            text = if (defaultOpen) "⌃" else "⌄"
            applyPaper(sizeSp = 22f, color = Color.BLACK, bold = false)
            gravity = Gravity.CENTER
        }

        header.addView(title)
        header.addView(arrow)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (defaultOpen) View.VISIBLE else View.GONE
            setPadding(10.dp(), 0, 10.dp(), 14.dp())
        }

        content.addView(View(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dp()
            ).apply {
                bottomMargin = 10.dp()
            }
        })

        info.rows.forEach { row ->
            content.addView(createCropDiseaseRow(row))
        }

        header.setOnClickListener {
            val nextOpen = content.visibility != View.VISIBLE
            content.visibility = if (nextOpen) View.VISIBLE else View.GONE
            arrow.text = if (nextOpen) "⌃" else "⌄"
        }

        root.addView(header)
        root.addView(content)

        return root
    }

    private fun createCropDiseaseRow(row: CropDiseaseRow): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp(), 0, 14.dp(), 0)
            background = ContextCompat.getDrawable(this@MonthlyDiseaseActivity, R.drawable.bg_crop_disease_row)
            elevation = 1f.dpFloat()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                32.dp()
            ).apply {
                bottomMargin = 8.dp()
            }

            addView(TextView(this@MonthlyDiseaseActivity).apply {
                text = row.cropName
                applyPaper(sizeSp = 10f, color = Color.BLACK, bold = true)
                layoutParams = LinearLayout.LayoutParams(
                    76.dp(),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })

            addView(TextView(this@MonthlyDiseaseActivity).apply {
                text = row.diseases.joinToString("        ")
                applyPaper(sizeSp = 10f, color = Color.BLACK, bold = false)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })
        }
    }

    private fun TextView.applyPaper(
        sizeSp: Float,
        color: Int,
        bold: Boolean
    ) {
        textSize = sizeSp
        setTextColor(color)
        includeFontPadding = false
        typeface = Typeface.create(
            paperFont,
            if (bold) Typeface.BOLD else Typeface.NORMAL
        )
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun Float.dpFloat(): Float {
        return this * resources.displayMetrics.density
    }
}