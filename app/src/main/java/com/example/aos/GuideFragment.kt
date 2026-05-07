package com.example.aos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class GuideFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_guide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnMonthlyDisease).setOnClickListener {
            startActivity(Intent(requireContext(), MonthlyDiseaseActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnToolGuide).setOnClickListener {
            startActivity(Intent(requireContext(), ToolGuideActivity::class.java))
        }

        view.findViewById<Button>(R.id.btnPesticideGuide).setOnClickListener {
            startActivity(Intent(requireContext(), PesticideGuideActivity::class.java))
        }
    }
}