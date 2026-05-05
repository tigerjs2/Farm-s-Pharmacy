package com.example.aos

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var navItems: List<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navCamera = findViewById<LinearLayout>(R.id.navCamera)
        val navCalendar = findViewById<LinearLayout>(R.id.navCalendar)
        val navGuide = findViewById<LinearLayout>(R.id.navGuide)
        val navMap = findViewById<LinearLayout>(R.id.navMap)

        navItems = listOf(navHome, navCamera, navCalendar, navGuide, navMap)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), navHome)
        }

        navHome.setOnClickListener { loadFragment(HomeFragment(), it as LinearLayout) }
        navCamera.setOnClickListener { loadFragment(CameraFragment(), it as LinearLayout) }
        navCalendar.setOnClickListener { loadFragment(CalendarFragment(), it as LinearLayout) }
        navGuide.setOnClickListener { loadFragment(GuideFragment(), it as LinearLayout) }
        navMap.setOnClickListener { loadFragment(MapFragment(), it as LinearLayout) }
    }

    private fun loadFragment(fragment: Fragment, selected: LinearLayout) {
        // 전부 초기화
        navItems.forEach { it.setBackgroundResource(0) }
        // 선택된 탭만 초록
        selected.setBackgroundResource(R.drawable.nav_item_selected)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}