package com.example.aos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment

class CheckFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_check, container, false)

        val crops = listOf(
            view.findViewById<ImageButton>(R.id.btnCucumber)   to "오이",
            view.findViewById<ImageButton>(R.id.btnStrawberry) to "딸기",
            view.findViewById<ImageButton>(R.id.btnPaprika)    to "파프리카",
            view.findViewById<ImageButton>(R.id.btnGrape)      to "포도",
            view.findViewById<ImageButton>(R.id.btnPepper)     to "고추",
            view.findViewById<ImageButton>(R.id.btnTomato)     to "토마토"
        )

        crops.forEach { (btn, cropName) ->
            btn.setOnClickListener {
                // 작물별 진단 기록 목록으로 이동
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DiagnosisListFragment.newInstance(cropName))
                    .addToBackStack(null)
                    .commit()
            }
        }

        view.findViewById<ImageButton>(R.id.btnCamera).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CameraFragment())
                .commit()
        }

        return view
    }
}
