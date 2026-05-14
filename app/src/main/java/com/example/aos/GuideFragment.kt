package com.example.aos

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import java.time.LocalDate

class GuideFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_guide, container, false)

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
