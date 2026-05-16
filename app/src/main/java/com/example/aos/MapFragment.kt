package com.example.aos

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MapFragment : Fragment() {

    private enum class Region(val label: String) {
        GYEONGGI("경기"),
        GANGWON("강원"),
        CHUNGCHEONG("충청"),
        JEOLLA("전라"),
        GYEONGSANG("경상"),
        JEJU("제주")
    }

    // 지도 지역 fill 컬러
    private val fillGreen = Color.parseColor("#C1DD8F")   // 0건
    private val fillYellow = Color.parseColor("#FFBB34")  // 1~5건
    private val fillOrange = Color.parseColor("#FF7E34")  // 6건 이상

    // 지역명 텍스트 컬러
    private val textGreen = Color.parseColor("#9FC45C")   // 0건
    private val textYellow = Color.parseColor("#F6AB15")  // 1~5건
    private val textOrange = Color.parseColor("#F26818")  // 6건 이상

    /*
     * true: 샘플 데이터로 테스트
     * false: Firestore Diagnoses 데이터로 집계
     */
    private val useSampleData = true

    private var selectedCrop = "전체"
    private var selectedDisease = "전체"

    private lateinit var tvCrop: TextView
    private lateinit var tvDisease: TextView
    private lateinit var llDiseaseSelector: LinearLayout

    private lateinit var tvTotalCount: TextView
    private lateinit var tvDamage1: TextView
    private lateinit var tvDamage2: TextView

    private lateinit var regionViews: Map<Region, ImageView>
    private lateinit var labelViews: Map<Region, TextView>
    private lateinit var countViews: Map<Region, TextView>

    private val crops = listOf(
        "전체",
        "고추",
        "딸기",
        "오이",
        "토마토",
        "파프리카",
        "포도"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews(view)
        setupSelectors()
        updateSelectionUi()
        loadStats()
    }

    private fun bindViews(view: View) {
        tvCrop = view.findViewById(R.id.tvCrop)
        tvDisease = view.findViewById(R.id.tvDisease)
        llDiseaseSelector = view.findViewById(R.id.llDiseaseSelector)

        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvDamage1 = view.findViewById(R.id.tvDamage1)
        tvDamage2 = view.findViewById(R.id.tvDamage2)

        regionViews = mapOf(
            Region.GYEONGGI to view.findViewById(R.id.ivRegionGyeonggi),
            Region.GANGWON to view.findViewById(R.id.ivRegionGangwon),
            Region.CHUNGCHEONG to view.findViewById(R.id.ivRegionChungcheong),
            Region.JEOLLA to view.findViewById(R.id.ivRegionJeolla),
            Region.GYEONGSANG to view.findViewById(R.id.ivRegionGyeongsang),
            Region.JEJU to view.findViewById(R.id.ivRegionJeju)
        )

        labelViews = mapOf(
            Region.GYEONGGI to view.findViewById(R.id.tvLabelGyeonggi),
            Region.GANGWON to view.findViewById(R.id.tvLabelGangwon),
            Region.CHUNGCHEONG to view.findViewById(R.id.tvLabelChungcheong),
            Region.JEOLLA to view.findViewById(R.id.tvLabelJeolla),
            Region.GYEONGSANG to view.findViewById(R.id.tvLabelGyeongsang),
            Region.JEJU to view.findViewById(R.id.tvLabelJeju)
        )

        countViews = mapOf(
            Region.GYEONGGI to view.findViewById(R.id.tvGyeonggiCount),
            Region.GANGWON to view.findViewById(R.id.tvGangwonCount),
            Region.CHUNGCHEONG to view.findViewById(R.id.tvChungcheongCount),
            Region.JEOLLA to view.findViewById(R.id.tvJeollaCount),
            Region.GYEONGSANG to view.findViewById(R.id.tvGyeongsangCount),
            Region.JEJU to view.findViewById(R.id.tvJejuCount)
        )
    }

    private fun setupSelectors() {
        requireView().findViewById<LinearLayout>(R.id.llCropSelector).setOnClickListener {
            showPicker(
                title = "작물",
                items = crops,
                selected = selectedCrop
            ) { crop ->
                selectedCrop = crop
                selectedDisease = "전체"

                updateSelectionUi()
                loadStats()
            }
        }

        llDiseaseSelector.setOnClickListener {
            showPicker(
                title = "질병",
                items = diseasesForCrop(selectedCrop),
                selected = selectedDisease
            ) { disease ->
                selectedDisease = disease

                updateSelectionUi()
                loadStats()
            }
        }
    }

    private fun updateSelectionUi() {
        tvCrop.text = selectedCrop
        tvDisease.text = selectedDisease

        llDiseaseSelector.visibility =
            if (selectedCrop == "전체") View.GONE else View.VISIBLE
    }

    private fun diseasesForCrop(crop: String): List<String> {
        if (crop == "전체") return listOf("전체")

        val diseases = HistoryActivity.CROP_DISEASES[crop] ?: emptyList()
        return listOf("전체") + diseases
    }

    private fun loadStats() {
        if (useSampleData) {
            applyStats(makeSampleStats())
        } else {
            loadStatsFromFirestore()
        }
    }

    private fun loadStatsFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid.isNullOrBlank()) {
            applyStats(emptyStats())
            return
        }

        var query: Query = FirebaseFirestore.getInstance()
            .collection("Diagnoses")
            .whereEqualTo("userId", uid)

        if (selectedCrop != "전체") {
            query = query.whereEqualTo("cropType", selectedCrop)
        }

        if (selectedDisease != "전체") {
            query = query.whereEqualTo("diseaseName", selectedDisease)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val stats = emptyStats()

                snapshot.documents.forEach { doc ->
                    val addressDo = doc.getString("addressDo") ?: ""
                    val region = addressToRegion(addressDo)

                    if (region != null) {
                        stats[region] = (stats[region] ?: 0) + 1
                    }
                }

                applyStats(stats)
            }
            .addOnFailureListener {
                applyStats(emptyStats())
            }
    }

    private fun applyStats(rawStats: Map<Region, Int>) {
        val stats = emptyStats()
        stats.putAll(rawStats)

        val total = stats.values.sum()
        tvTotalCount.text = total.toString()

        Region.values().forEach { region ->
            val count = stats[region] ?: 0

            // 지도 지역 색
            regionViews[region]?.imageTintList =
                ColorStateList.valueOf(fillColorForCount(count))

            // 지도 지역명 텍스트 색
            labelViews[region]?.setTextColor(textColorForCount(count))

            // 하단 숫자
            countViews[region]?.text = count.toString()
        }

        renderDamageText(stats)
    }

    private fun fillColorForCount(count: Int): Int {
        return when {
            count == 0 -> fillGreen
            count in 1..5 -> fillYellow
            else -> fillOrange
        }
    }

    private fun textColorForCount(count: Int): Int {
        return when {
            count == 0 -> textGreen      // #9FC45C
            count in 1..5 -> textYellow  // #F6AB15
            else -> textOrange           // #F26818
        }
    }

    private fun renderDamageText(stats: Map<Region, Int>) {
        val sorted = stats
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }

        if (sorted.isEmpty()) {
            tvDamage1.text = "·질병 피해 없습니다!"
            tvDamage2.visibility = View.GONE
            return
        }

        val cropText = if (selectedCrop == "전체") "작물" else selectedCrop
        val diseaseText = if (selectedDisease == "전체") "병해" else selectedDisease

        val first = sorted[0]
        tvDamage1.text =
            "·${first.first.label}도 인근 ${cropText} ${diseaseText} 피해 ${damageWord(first.second)}"

        if (sorted.size >= 2) {
            val second = sorted[1]
            tvDamage2.text =
                "·${second.first.label}도 ${cropText} ${diseaseText} ${second.second}건 발생"
            tvDamage2.visibility = View.VISIBLE
        } else {
            tvDamage2.visibility = View.GONE
        }
    }

    private fun damageWord(count: Int): String {
        return if (count > 5) "급증" else "발생"
    }

    private fun emptyStats(): MutableMap<Region, Int> {
        return Region.values().associateWith { 0 }.toMutableMap()
    }

    private fun makeSampleStats(): Map<Region, Int> {
        val stats = emptyStats()

        if (selectedCrop == "파프리카" && selectedDisease == "흰가루병") {
            stats[Region.GYEONGGI] = 2
            stats[Region.GANGWON] = 6
            stats[Region.CHUNGCHEONG] = 7
            stats[Region.JEOLLA] = 3
            stats[Region.GYEONGSANG] = 0
            stats[Region.JEJU] = 0
        }

        return stats
    }

    private fun addressToRegion(addressDo: String): Region? {
        val text = addressDo.trim()

        return when {
            text.contains("서울") ||
                    text.contains("인천") ||
                    text.contains("경기") -> Region.GYEONGGI

            text.contains("강원") -> Region.GANGWON

            text.contains("충북") ||
                    text.contains("충남") ||
                    text.contains("충청") ||
                    text.contains("대전") ||
                    text.contains("세종") -> Region.CHUNGCHEONG

            text.contains("전북") ||
                    text.contains("전남") ||
                    text.contains("전라") ||
                    text.contains("광주") -> Region.JEOLLA

            text.contains("경북") ||
                    text.contains("경남") ||
                    text.contains("경상") ||
                    text.contains("대구") ||
                    text.contains("부산") ||
                    text.contains("울산") -> Region.GYEONGSANG

            text.contains("제주") -> Region.JEJU

            else -> null
        }
    }

    private fun showPicker(
        title: String,
        items: List<String>,
        selected: String,
        onPick: (String) -> Unit
    ) {
        val dialog = BottomSheetDialog(requireContext())

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dp(), 20.dp(), 22.dp(), 22.dp())
        }

        container.addView(
            TextView(requireContext()).apply {
                text = title
                textSize = 15f
                setTextColor(Color.parseColor("#B8A9B7"))
                typeface = resources.getFont(R.font.paperlogy_3light)
                setPadding(0, 0, 0, 12.dp())
            }
        )

        items.forEach { item ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12.dp(), 0, 12.dp())
                setOnClickListener {
                    onPick(item)
                    dialog.dismiss()
                }
            }

            row.addView(
                TextView(requireContext()).apply {
                    text = item
                    textSize = 19f
                    setTextColor(Color.BLACK)
                    typeface = resources.getFont(R.font.paperlogy_4regular)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
            )

            row.addView(
                TextView(requireContext()).apply {
                    text = if (item == selected) "✓" else ""
                    textSize = 25f
                    setTextColor(Color.BLACK)
                    typeface = resources.getFont(R.font.paperlogy_4regular)
                }
            )

            container.addView(row)
        }

        dialog.setContentView(container)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.background = null
        }

        dialog.show()
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density + 0.5f).toInt()
    }
}