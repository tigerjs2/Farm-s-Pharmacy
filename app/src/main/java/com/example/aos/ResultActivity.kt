package com.example.aos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class ResultActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        viewPager = findViewById(R.id.viewPager)

        val imageUri   = intent.getStringExtra("imageUri")
        val cropName   = intent.getStringExtra("cropName") ?: ""
        val diagType   = intent.getStringExtra("diagType") ?: "UNKNOWN"
        val label      = intent.getStringExtra("label") ?: ""
        val confidence = intent.getIntExtra("confidence", 0)
        val sickKey    = intent.getStringExtra("sickKey") ?: ""

        val adapter = ResultPagerAdapter(this, imageUri, cropName, diagType, label, confidence, sickKey)
        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL

        // DISEASE가 아니면 스와이프 막기
        viewPager.isUserInputEnabled = (diagType == "DISEASE")
    }

    private inner class ResultPagerAdapter(
        fa: FragmentActivity,
        private val imageUri: String?,
        private val cropName: String,
        private val diagType: String,
        private val label: String,
        private val confidence: Int,
        private val sickKey: String
    ) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int {
            return if (diagType == "DISEASE") 2 else 1
            // DISEASE → [ResultFragment, DetailFragment]
            // NORMAL/UNKNOWN → [ResultFragment] 만
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ResultFragment.newInstance(imageUri, cropName, diagType, label, confidence, sickKey)
                1 -> DetailFragment.newInstance(cropName, label, sickKey)
                else -> ResultFragment.newInstance(imageUri, cropName, diagType, label, confidence, sickKey)
            }
        }
    }
}