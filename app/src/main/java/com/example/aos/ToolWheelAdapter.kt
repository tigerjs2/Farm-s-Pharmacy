package com.example.aos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ToolWheelAdapter(
    private val tools: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ToolWheelAdapter.ToolViewHolder>() {

    companion object {
        const val LOOP_COUNT = 1000
    }

    var selectedPosition: Int = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool_wheel, parent, false) as TextView
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val realPosition = position % tools.size
        val toolName = tools[realPosition]

        holder.textView.text = toolName

        if (position == selectedPosition) {
            holder.textView.textSize = 55f
            holder.textView.setTextColor(Color.parseColor("#323232"))
            holder.textView.typeface =
                holder.textView.resources.getFont(R.font.paperlogy_7bold)
        } else {
            holder.textView.textSize = 36f
            holder.textView.setTextColor(Color.parseColor("#6F6F6B"))
            holder.textView.typeface =
                holder.textView.resources.getFont(R.font.paperlogy_3light)
        }

        holder.textView.setOnClickListener {
            onClick(toolName)
        }
    }

    override fun getItemCount(): Int = tools.size * LOOP_COUNT

    class ToolViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}