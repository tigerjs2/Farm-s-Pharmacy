package com.example.aos

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailFragment : Fragment() {

    companion object {
        private const val ARG_CROP_NAME    = "cropName"
        private const val ARG_DISEASE_NAME = "diseaseName"
        private const val ARG_SICK_KEY     = "sickKey"

        fun newInstance(cropName: String, diseaseName: String, sickKey: String): DetailFragment {
            val fragment = DetailFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_CROP_NAME,    cropName)
                putString(ARG_DISEASE_NAME, diseaseName)
                putString(ARG_SICK_KEY,     sickKey)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sickKey = arguments?.getString(ARG_SICK_KEY) ?: ""

        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        scrollView.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        // 투톤 타이틀
        applySymptomTitle(view)
        applyActionTitle(view)

        val symptomContainer = view.findViewById<LinearLayout>(R.id.symptomContainer)
        val actionContainer  = view.findViewById<LinearLayout>(R.id.actionContainer)

        if (sickKey.isNotEmpty()) {
            // 로딩 플레이스홀더
            symptomContainer.addView(makeBulletItem("불러오는 중..."))
            actionContainer.addView(makeBulletItem("불러오는 중..."))

            lifecycleScope.launch {
                val detail = withContext(Dispatchers.IO) {
                    DiseaseApiService.getDetail(sickKey)
                }

                symptomContainer.removeAllViews()
                actionContainer.removeAllViews()

                if (detail != null) {
                    // 병피해 사진 로드 (최대 2장)
                    val ivSymptom1 = view.findViewById<ImageView>(R.id.ivSymptom1)
                    val ivSymptom2 = view.findViewById<ImageView>(R.id.ivSymptom2)
                    if (detail.imageUrls.isNotEmpty()) {
                        Glide.with(requireContext()).load(detail.imageUrls[0]).centerCrop().into(ivSymptom1)
                    }
                    if (detail.imageUrls.size >= 2) {
                        Glide.with(requireContext()).load(detail.imageUrls[1]).centerCrop().into(ivSymptom2)
                    }

                    // 증상: HTML 제거 후 파싱
                    parseItems(detail.symptoms).forEach {
                        symptomContainer.addView(makeBulletItem(it))
                    }

                    // 대처법: preventionMethod + chemicalPrvnbeMth 합산
                    val treatmentItems = mutableListOf<String>()
                    treatmentItems += parseItems(detail.preventionMethod)
                    if (detail.chemicalPrvnbeMth.isNotBlank()) {
                        treatmentItems += parseItems(detail.chemicalPrvnbeMth)
                    }
                    treatmentItems.forEach { actionContainer.addView(makeBulletItem(it)) }
                } else {
                    symptomContainer.addView(makeBulletItem("증상 정보를 불러올 수 없습니다."))
                    actionContainer.addView(makeBulletItem("대처법 정보를 불러올 수 없습니다."))
                }
            }
        } else {
            symptomContainer.addView(makeBulletItem("병해 정보를 불러올 수 없습니다."))
            actionContainer.addView(makeBulletItem("병해 정보를 불러올 수 없습니다."))
        }
    }

    /**
     * API 응답 문자열을 bullet 항목 리스트로 파싱
     * 1. HTML 태그 제거 (<br> → \n, 나머지 태그 제거)
     * 2. 줄바꿈 또는 마침표 기준으로 분리
     * 3. 빈 항목 및 너무 짧은 항목 제거
     */
    private fun parseItems(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()

        // HTML 정리
        val cleaned = raw
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<p\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .trim()

        // 줄바꿈으로 먼저 분리, 없으면 마침표로 분리
        val byNewline = cleaned.split("\n", "\r\n").map { it.trim() }.filter { it.length > 3 }
        if (byNewline.size > 1) return byNewline

        return cleaned.split(".")
            .map { it.trim() }
            .filter { it.length > 3 }
            .map { if (!it.endsWith(".")) "$it." else it }
    }

    private fun applySymptomTitle(view: View) {
        val tv = view.findViewById<TextView>(R.id.symptomTitle)
        val text = "이런 증상을 보여요"
        val spannable = SpannableString(text)
        val darkGreen  = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val lightGreen = ContextCompat.getColor(requireContext(), R.color.black)
        spannable.setSpan(ForegroundColorSpan(darkGreen),  0, 3,  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(lightGreen), 3, 5,  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(darkGreen),  5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = spannable
    }

    private fun applyActionTitle(view: View) {
        val tv = view.findViewById<TextView>(R.id.actionTitle)
        val text = "이렇게 대처해요"
        val spannable = SpannableString(text)
        val darkGreen  = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val lightGreen = ContextCompat.getColor(requireContext(), R.color.black)
        spannable.setSpan(ForegroundColorSpan(darkGreen),  0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(lightGreen), 4, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(darkGreen),  6, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = spannable
    }

    private fun makeBulletItem(text: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dpToPx() }
            layoutParams = params

            // bullet
            addView(TextView(requireContext()).apply {
                this.text = "•"
                textSize = 22f
                setTextColor(Color.BLACK)
                setPadding(0, 0, 16, 0)
                typeface = resources.getFont(R.font.paperlogy_3light)
            })

            // 내용
            addView(TextView(requireContext()).apply {
                this.text = text
                textSize = 22f
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                typeface = resources.getFont(R.font.paperlogy_3light)
            })
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}