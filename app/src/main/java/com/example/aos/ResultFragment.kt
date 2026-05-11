package com.example.aos

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultFragment : Fragment() {

    companion object {
        private const val ARG_IMAGE_URI  = "imageUri"
        private const val ARG_CROP_NAME  = "cropName"
        private const val ARG_DIAG_TYPE  = "diagType"
        private const val ARG_LABEL      = "label"
        private const val ARG_CONFIDENCE = "confidence"
        private const val ARG_SICK_KEY   = "sickKey"

        fun newInstance(
            imageUri: String?,
            cropName: String,
            diagType: String,
            label: String,
            confidence: Int,
            sickKey: String
        ): ResultFragment {
            val fragment = ResultFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_IMAGE_URI,  imageUri)
                putString(ARG_CROP_NAME,  cropName)
                putString(ARG_DIAG_TYPE,  diagType)
                putString(ARG_LABEL,      label)
                putInt(ARG_CONFIDENCE,    confidence)
                putString(ARG_SICK_KEY,   sickKey)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_result, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUri   = arguments?.getString(ARG_IMAGE_URI)
        val cropName   = arguments?.getString(ARG_CROP_NAME) ?: ""
        val diagType   = arguments?.getString(ARG_DIAG_TYPE) ?: "UNKNOWN"
        val label      = arguments?.getString(ARG_LABEL) ?: ""
        val confidence = arguments?.getInt(ARG_CONFIDENCE) ?: 0
        val sickKey    = arguments?.getString(ARG_SICK_KEY) ?: ""

        // 로고 투톤
        val logoText = view.findViewById<TextView>(R.id.logoTop)
        val logoString = "농장의 파머씨"
        val spannable = SpannableString(logoString)
        val normalGreen = ContextCompat.getColor(requireContext(), R.color.primary_light)
        val boldGreen   = ContextCompat.getColor(requireContext(), R.color.primary_green)
        spannable.setSpan(ForegroundColorSpan(normalGreen), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(boldGreen),   4, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(normalGreen), 6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        logoText.text = spannable

        // 촬영 이미지
        val ivCaptured = view.findViewById<ImageView>(R.id.resultImageView)
        if (imageUri != null) {
            try {
                ivCaptured.setImageURI(Uri.parse(imageUri))
            } catch (e: Exception) {
                ivCaptured.setBackgroundColor(Color.parseColor("#C8E6C9"))
            }
        }

        // 신뢰도
        view.findViewById<TextView>(R.id.confidenceTextView).text = "$confidence%"

        // 스와이프 안내: DISEASE일 때만 표시
        view.findViewById<LinearLayout>(R.id.detailButtonLayout).visibility =
            if (diagType == "DISEASE") View.VISIBLE else View.GONE

        val tvDiseaseName = view.findViewById<TextView>(R.id.diseaseNameTextView)
        val tvLabel       = view.findViewById<TextView>(R.id.textLabel)
        val tvConfidence  = view.findViewById<TextView>(R.id.confidenceTextView)
        val tvDesc        = view.findViewById<TextView>(R.id.descriptionTextView)

        when (diagType) {
            "NORMAL" -> {
                tvDiseaseName.text = "정상"
                tvDiseaseName.setTextColor(Color.parseColor("#005DE8"))
                tvConfidence.setTextColor(Color.parseColor("#005DE8"))
                tvLabel.text = "적인 잎입니다"
                tvLabel.setTextColor(Color.parseColor("#000000"))
                tvDesc.text = "정상적인 생육입니다."
            }
            "DISEASE" -> {
                tvDiseaseName.text = label
                tvDiseaseName.setTextColor(Color.parseColor("#FFCC00"))
                tvConfidence.setTextColor(Color.parseColor("#FFCC00"))
                tvLabel.text = "이 의심돼요"
                tvLabel.setTextColor(Color.parseColor("#000000"))

                if (sickKey.isNotEmpty()) {
                    tvDesc.text = "불러오는 중..."
                    lifecycleScope.launch {
                        val detail = withContext(Dispatchers.IO) {
                            DiseaseApiService.getDetail(sickKey)
                        }
                        val raw = detail?.developmentCondition
                            ?.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
                            ?.replace(Regex("<[^>]+>"), "")
                            ?.replace("&nbsp;", " ")
                            ?.trim()
                        val firstSentence = raw
                            ?.split(".")
                            ?.firstOrNull { it.trim().length > 5 }
                            ?.trim()
                        tvDesc.text = if (firstSentence.isNullOrBlank()) "정보를 불러올 수 없습니다."
                        else "$firstSentence."
                    }
                } else {
                    tvDesc.text = "병해 정보를 불러올 수 없습니다."
                }
            }
            else -> {
                tvDiseaseName.text = ""
                tvConfidence.setTextColor(Color.parseColor("#C0B5BF"))
                tvLabel.text = "인식하지 못했어요"
                tvLabel.setTextColor(Color.parseColor("#000000"))
                tvDesc.text = "${cropName} 잎이 인식되지 않았습니다.\n잎을 정확히 촬영해주세요."
            }
        }

        // ── 처치 완료 오버레이 토글 ──
        val overlayTreated = view.findViewById<View>(R.id.overlayTreated)
        val tvTreatedLabel = view.findViewById<TextView>(R.id.tvTreatedLabel)
        val prefs = requireContext().getSharedPreferences("HistoryPrefs", Context.MODE_PRIVATE)
        val gson  = Gson()

        fun loadTreated(): Boolean {
            val json  = prefs.getString("history_items", "[]") ?: "[]"
            val items = gson.fromJson(json, Array<HistoryItem>::class.java)
            return items.find { it.imageUri == imageUri }?.isTreated ?: false
        }

        fun saveTreated(treated: Boolean) {
            val json  = prefs.getString("history_items", "[]") ?: "[]"
            val items = gson.fromJson(json, Array<HistoryItem>::class.java).toMutableList()
            val idx   = items.indexOfFirst { it.imageUri == imageUri }
            if (idx != -1) {
                items[idx] = items[idx].copy(isTreated = treated)
                prefs.edit().putString("history_items", gson.toJson(items)).apply()
            }
        }

        fun applyOverlay(treated: Boolean) {
            val vis = if (treated) View.VISIBLE else View.GONE
            overlayTreated.visibility = vis
            tvTreatedLabel.visibility = vis
        }

        // 초기 상태 반영
        applyOverlay(loadTreated())

        // 사진 클릭 → 토글
        ivCaptured.setOnClickListener {
            val next = !loadTreated()
            saveTreated(next)
            applyOverlay(next)
        }
    }
}