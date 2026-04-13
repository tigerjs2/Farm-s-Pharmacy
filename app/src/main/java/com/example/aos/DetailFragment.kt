package com.example.aos

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.widget.ScrollView

class DetailFragment : Fragment() {

    companion object {
        private const val ARG_CROP_NAME = "cropName"

        fun newInstance(cropName: String): DetailFragment {
            val fragment = DetailFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_CROP_NAME, cropName)
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

        val cropName = arguments?.getString(ARG_CROP_NAME) ?: ""

        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        scrollView.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }


        // 투톤 타이틀
        applySymptomTitle(view)
        applyActionTitle(view)

        // TODO: API 연동 시 bindDummyData() 대신 API 응답값으로 교체
        bindDummyData(view, cropName)

        // TODO: API 연동 시 Glide로 이미지 로드
        // Glide.with(this).load(imageUrl1).into(view.findViewById(R.id.ivSymptom1))
        // Glide.with(this).load(imageUrl2).into(view.findViewById(R.id.ivSymptom2))
    }

    private fun applySymptomTitle(view: View) {
        val tv = view.findViewById<TextView>(R.id.symptomTitle)
        val text = "이런 증상을 보여요"
        val spannable = SpannableString(text)
        val darkGreen  = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val lightGreen = ContextCompat.getColor(requireContext(), R.color.black)
        spannable.setSpan(ForegroundColorSpan(darkGreen),  0, 3,  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // "이런"
        spannable.setSpan(ForegroundColorSpan(lightGreen), 3, 5,  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // "증상"
        spannable.setSpan(ForegroundColorSpan(darkGreen),  5, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // "을 보여요"
        tv.text = spannable
    }

    private fun applyActionTitle(view: View) {
        val tv = view.findViewById<TextView>(R.id.actionTitle)
        val text = "이렇게 대처해요"
        val spannable = SpannableString(text)
        val darkGreen  = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val lightGreen = ContextCompat.getColor(requireContext(), R.color.black)
        spannable.setSpan(ForegroundColorSpan(darkGreen),  0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // "이렇게"
        spannable.setSpan(ForegroundColorSpan(lightGreen), 4, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // "대처"
        spannable.setSpan(ForegroundColorSpan(darkGreen),  6, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // "해요"
        tv.text = spannable
    }

    private fun bindDummyData(view: View, cropName: String) {
        val symptoms = listOf(
            "주로 생육 중기 및 후기의 잎에 발생해요.",
            "초기에는 잎의 앞면에 퇴록된 작은 부정형 반점이 옅은 황색을 띠고, 잎 뒷면의 병반은 불분명해요.",
            "아랫 잎에서 먼저 발생되고, 위로 진전되는데 엽맥에 둘러싸인 병반들이 합쳐지면서 커지고 잎이 말라죽어요.",
            "병든 잎은 잘 떨어지고, 황갈색을 띠어요.",
            "환경이 적당하면 잎 뒷면에 이슬처럼 보이는 곰팡이가 다량 형성되어 회백색으로 보여요."
        )
        val treatments = listOf(
            "병든 잎은 조기에 제거하여 불에 태우거나 땅속 깊이 묻어요.",
            "포장을 청결히 하고 잎에 물방울이 장시간 맺혀 있지 않도록 관리해요.",
            "환기를 철저히 하고 토양이 과습하지 않도록 해요."
        )

        val symptomContainer = view.findViewById<LinearLayout>(R.id.symptomContainer)
        symptoms.forEach { symptomContainer.addView(makeBulletItem(it)) }

        val actionContainer = view.findViewById<LinearLayout>(R.id.actionContainer)
        treatments.forEach { actionContainer.addView(makeBulletItem(it)) }
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