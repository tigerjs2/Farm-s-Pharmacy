package com.example.aos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment

class CameraFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        val crops = listOf(
            view.findViewById<ImageButton>(R.id.btnCucumber) to "오이",
            view.findViewById<ImageButton>(R.id.btnStrawberry) to "딸기",
            view.findViewById<ImageButton>(R.id.btnPaprika) to "파프리카",
            view.findViewById<ImageButton>(R.id.btnGrape) to "포도",
            view.findViewById<ImageButton>(R.id.btnPepper) to "고추",
            view.findViewById<ImageButton>(R.id.btnTomato) to "토마토"
        )

        crops.forEach { (btn, cropName) ->
            btn.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, PhotoFragment.newInstance(cropName))
                    .addToBackStack(null)
                    .commit()
            }
        }

        view.findViewById<ImageButton>(R.id.btnCheckRecord).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CheckFragment())
                .commit()
        }
        return view
    }
}