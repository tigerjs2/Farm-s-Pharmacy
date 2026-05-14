package com.example.aos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class GuideFragment : Fragment() {

    private var rootView: View? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_guide, container, false)
        rootView = view

        view.findViewById<Button>(R.id.btnMonthlyDisease).setOnClickListener {
            startActivity(Intent(requireContext(), MonthlyDiseaseActivity::class.java))
        }


        view.findViewById<Button>(R.id.btnToolGuide).setOnClickListener {
            startActivity(Intent(requireContext(), ToolGuideActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnPesticideGuide).setOnClickListener {
            startActivity(Intent(requireContext(), PesticideGuideActivity::class.java))
        }

        bindCurrentMonth(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        loadTodaySafety()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }

    private fun loadTodaySafety() {

        lifecycleScope.launch {

            val guide = withContext(Dispatchers.IO) {
                NongsaroApiService.getRandomToolGuide()
            }

            val view = rootView ?: return@launch
            guide ?: return@launch

            view.findViewById<TextView>(R.id.tvTodaySafetyTitle).text = guide.title

            val image = view.findViewById<ImageView>(R.id.ivTodaySafetyImage)
            if (guide.imageUrl.isNotBlank()) {
                Glide.with(this@GuideFragment).load(guide.imageUrl).into(image)
            }

            renderBullets(
                view.findViewById(R.id.todaySafetyBullets),
                splitBullets(guide.content)
            )
        }
    }

    private fun renderBullets(container: LinearLayout, bullets: List<String>) {
        container.removeAllViews()
        bullets.forEach { text ->
            val tv = TextView(requireContext()).apply {
                this.text = "· $text"
                textSize = 12f
                setTextColor(0xFF000000.toInt())
                typeface = resources.getFont(R.font.paperlogy_3light)
                setPadding(0, 8, 0, 0)
            }
            container.addView(tv)
        }
    }

    private fun splitBullets(content: String): List<String> {
        if (content.isBlank()) return emptyList()
        return content
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("</p\\s*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .split("\n")
            .map { it.trim().trimStart('·', '○', '•', '-', ' ') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun bindCurrentMonth(root: View) {

        val curr = LocalDate.now().monthValue
        val prev = if (curr == 1) 12 else curr - 1
        val next = if (curr == 12) 1 else curr + 1

        root.findViewById<TextView>(R.id.tvPrevMonth).text = "${prev}월"
        root.findViewById<TextView>(R.id.tvCurrMonth).text = "${curr}월"
        root.findViewById<TextView>(R.id.tvNextMonth).text = "${next}월"

        root.findViewById<TextView>(R.id.tvPrevDiseases).text =
            DiseaseCalendarRepository.get(requireContext(), prev)
                ?.common?.joinToString(", ") ?: "-"

        root.findViewById<TextView>(R.id.tvCurrDiseases).text =
            DiseaseCalendarRepository.get(requireContext(), curr)
                ?.common?.joinToString(", ") ?: "-"

        root.findViewById<TextView>(R.id.tvNextDiseases).text =
            DiseaseCalendarRepository.get(requireContext(), next)
                ?.common?.joinToString(", ") ?: "-"
    }
}
