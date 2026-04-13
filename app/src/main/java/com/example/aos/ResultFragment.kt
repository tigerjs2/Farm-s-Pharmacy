package com.example.aos

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

class ResultFragment : Fragment() {

    companion object {
        private const val ARG_IMAGE_URI  = "imageUri"
        private const val ARG_CROP_NAME  = "cropName"
        private const val ARG_DIAG_TYPE  = "diagType"
        private const val ARG_LABEL      = "label"
        private const val ARG_CONFIDENCE = "confidence"

        fun newInstance(
            imageUri: String?,
            cropName: String,
            diagType: String,
            label: String,
            confidence: Int
        ): ResultFragment {
            val fragment = ResultFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_IMAGE_URI,  imageUri)
                putString(ARG_CROP_NAME,  cropName)
                putString(ARG_DIAG_TYPE,  diagType)
                putString(ARG_LABEL,      label)
                putInt(ARG_CONFIDENCE,    confidence)
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
                tvDesc.text = getDummyBriefDesc() // TODO: API 교체
            }
            else -> { // UNKNOWN
                tvDiseaseName.text = ""
                tvConfidence.setTextColor(Color.parseColor("#C0B5BF"))
                tvLabel.text = "인식하지 못했어요"
                tvLabel.setTextColor(Color.parseColor("#000000"))
                tvDesc.text = "${cropName} 잎이 인식되지 않았습니다.\n잎을 정확히 촬영해주세요."
            }
        }
    }

    // TODO: API 교체
    private fun getDummyBriefDesc() =
        "잎 표면에 처음에는 퇴록된 부정형 반점이 생기고, 감염부위가 담황색을 띱니다."
}