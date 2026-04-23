package com.example.aos

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.View
import android.widget.RelativeLayout
import android.content.Intent

class HistoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CROP_NAME = "crop_name"

        val CROP_DISEASES = mapOf(
            "오이"    to listOf("노균병", "흰가루병", "기타"),
            "딸기"    to listOf("흰가루병", "기타"),
            "포도"    to listOf("노균병", "기타"),
            "고추"    to listOf("흰가루병", "기타"),
            "토마토"  to listOf("흰가루병", "잿빛곰팡이병", "기타"),
            "파프리카" to listOf("흰가루병", "기타")
        )

        val CROP_COLORS = mapOf(
            "오이"    to "#5F9340",
            "딸기"    to "#FA3650",
            "포도"    to "#5525BD",
            "고추"    to "#EE3D3D",
            "토마토"  to "#EE3D3D",
            "파프리카" to "#FBCC4A"
        )

        val CHIP_ON = mapOf(
            "노균병"    to R.drawable.chip_downy_on,
            "흰가루병"  to R.drawable.chip_powdery_on,
            "잿빛곰팡이병" to R.drawable.chip_graymold_on,
            "기타"     to R.drawable.chip_other_on
        )

        val CHIP_OFF = mapOf(
            "노균병"    to R.drawable.chip_downy_off,
            "흰가루병"  to R.drawable.chip_powdery_off,
            "잿빛곰팡이병" to R.drawable.chip_graymold_off,
            "기타"     to R.drawable.chip_other_off
        )
    }

    private lateinit var cropName: String
    private lateinit var historyAdapter: HistoryAdapter

    private var allItems: List<HistoryItem> = emptyList()
    private var isNewestFirst = true
    private val filterState = mutableMapOf<String, Boolean>()

    private val prefs by lazy { getSharedPreferences("HistoryPrefs", MODE_PRIVATE) }
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        cropName = intent.getStringExtra(EXTRA_CROP_NAME) ?: "오이"

        setupTitle()
        setupFilterChips()
        setupRecyclerView()
        setupSortButton()

        allItems = loadItemsByCrop(cropName)
        applyFilters()
    }

    private fun loadItemsByCrop(cropName: String): List<HistoryItem> {
        val json = prefs.getString("history_items", null) ?: return emptyList()
        if (!json.trimStart().startsWith("[")) {
            prefs.edit().remove("history_items").apply()
            return emptyList()
        }
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            val all: List<HistoryItem> = gson.fromJson(json, type)
            all.filter { it.cropName == cropName }
        } catch (e: Exception) {
            prefs.edit().remove("history_items").apply()
            emptyList()
        }
    }

    private fun setupTitle() {
        val color = android.graphics.Color.parseColor(CROP_COLORS[cropName] ?: "#4CAF50")
        findViewById<TextView>(R.id.titleText).apply {
            text = "나의 ${cropName}로그"
            setTextColor(color)
        }
    }

    private fun setupFilterChips() {
        val container = findViewById<LinearLayout>(R.id.filterChipContainer)
        container.removeAllViews()

        CROP_DISEASES[cropName]?.forEach { disease ->
            filterState[disease] = true

            val chip = ImageView(this).apply {
                setImageResource(CHIP_ON[disease]!!)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 16, 0) }
            }

            chip.setOnClickListener {
                val isOn = filterState[disease] ?: true
                filterState[disease] = !isOn
                chip.setImageResource(if (!isOn) CHIP_ON[disease]!! else CHIP_OFF[disease]!!)
                applyFilters()
            }

            container.addView(chip)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList()) { historyItem ->
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("imageUri",   historyItem.imageUri)
                putExtra("cropName",   historyItem.cropName)
                putExtra("diagType",   historyItem.diagType)
                putExtra("label",      historyItem.diseaseName)
                putExtra("confidence", historyItem.confidence)
                putExtra("sickKey",    historyItem.sickKey)
            }
            startActivity(intent)
        }

        val layoutManager = GridLayoutManager(this, 3).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (historyAdapter.getItemViewType(position) == HistoryAdapter.VIEW_TYPE_DATE) 3 else 1
            }
        }

        findViewById<RecyclerView>(R.id.historyRecyclerView).apply {
            this.layoutManager = layoutManager
            adapter = historyAdapter
        }
    }

    private fun setupSortButton() {
        val sortText = findViewById<TextView>(R.id.sortText)

        findViewById<LinearLayout>(R.id.sortButton).setOnClickListener {
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottom_sheet_sort, null)

            val checkNewest = view.findViewById<TextView>(R.id.checkNewest)
            val checkOldest = view.findViewById<TextView>(R.id.checkOldest)
            checkNewest.visibility = if (isNewestFirst) View.VISIBLE else View.GONE
            checkOldest.visibility = if (!isNewestFirst) View.VISIBLE else View.GONE

            view.findViewById<RelativeLayout>(R.id.sortNewest).setOnClickListener {
                isNewestFirst = true
                sortText.text = "최신순"
                applyFilters()
                dialog.dismiss()
            }
            view.findViewById<RelativeLayout>(R.id.sortOldest).setOnClickListener {
                isNewestFirst = false
                sortText.text = "오래된 순"
                applyFilters()
                dialog.dismiss()
            }

            dialog.setContentView(view)

            dialog.setOnShowListener {
                val bottomSheet = dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )
                bottomSheet?.background = null
            }
            dialog.show()
        }
    }

    private fun applyFilters() {
        val filtered = allItems.filter { item ->
            filterState.any { (disease, isOn) -> isOn && item.diseaseName.contains(disease) }
        }
        val sorted = if (isNewestFirst) {
            filtered.sortedWith(compareByDescending<HistoryItem> { it.date }.thenByDescending { it.id })
        } else {
            filtered.sortedWith(compareBy<HistoryItem> { it.date }.thenBy { it.id })
        }
        historyAdapter.updateData(sorted)
    }
}