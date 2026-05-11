package com.example.aos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class ToolGuideActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ToolWheelAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private val toolList = listOf(
        "도로주행 농기계",
        "축산용 농기계",
        "동력예취기",
        "방제기",
        "이앙기",
        "경운기 또는 관리기",
        "트랙터",
        "공통사항",
        "콤바인"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool_guide)

        recyclerView = findViewById(R.id.toolRecyclerView)

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        adapter = ToolWheelAdapter(toolList) { toolName ->
            val intent = Intent(this, ToolGuideDetailActivity::class.java)
            intent.putExtra("toolName", toolName)
            startActivity(intent)
        }

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        recyclerView.post {
            val padding = recyclerView.height / 2 - 40
            recyclerView.setPadding(0, padding, 0, padding)

            val middle = adapter.itemCount / 2
            val defaultRealPosition = toolList.indexOf("방제기")
            val defaultPosition = middle - (middle % toolList.size) + defaultRealPosition

            recyclerView.scrollToPosition(defaultPosition)
            updateSelectedItem(defaultPosition)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                super.onScrollStateChanged(rv, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManager)
                    val position = centerView?.let {
                        layoutManager.getPosition(it)
                    } ?: return

                    updateSelectedItem(position)
                }
            }
        })
    }

    private fun updateSelectedItem(position: Int) {
        val oldPosition = adapter.selectedPosition
        adapter.selectedPosition = position

        if (oldPosition != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(oldPosition)
        }

        adapter.notifyItemChanged(position)
    }
}