package com.example.aos

import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 한 아이템이 '하루'가 아니라 '일주일'이다.
 * 그래서 좌우로 밀 때 7일 단위로만 넘어간다.
 */
@RequiresApi(Build.VERSION_CODES.O)
class WeekCalendarAdapter(
    private val weekStartDates: List<LocalDate>,
    selectedDate: LocalDate,
    private var stats: Map<LocalDate, CalendarDayStat>,
    private val onDateClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<WeekCalendarAdapter.WeekViewHolder>() {

    private var selectedDate: LocalDate = selectedDate

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val container = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return WeekViewHolder(container)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        holder.container.removeAllViews()

        val recyclerView = holder.container.parent as? RecyclerView
        val pageWidth = recyclerView?.let { it.width - it.paddingStart - it.paddingEnd } ?: 0
        if (pageWidth > 0) {
            holder.container.layoutParams = holder.container.layoutParams.apply {
                width = pageWidth
            }
        }

        val weekStart = weekStartDates[position]
        val inflater = LayoutInflater.from(holder.container.context)

        repeat(7) { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val dayView = inflater.inflate(
                R.layout.item_week_calendar_day,
                holder.container,
                false
            )

            dayView.layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )

            bindDay(dayView, date)
            holder.container.addView(dayView)
        }
    }

    private fun bindDay(dayView: View, date: LocalDate) {
        val context = dayView.context
        val isSelected = date == selectedDate

        val tvWeekDay = dayView.findViewById<TextView>(R.id.tvWeekDay)
        val vDayBackground = dayView.findViewById<View>(R.id.vDayBackground)
        val tvDayNumber = dayView.findViewById<TextView>(R.id.tvDayNumber)
        val ivDiseaseDot = dayView.findViewById<ImageView>(R.id.ivDiseaseDot)

        tvWeekDay.text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        tvDayNumber.text = date.dayOfMonth.toString()

        val selectedColor = ContextCompat.getColor(context, R.color.primary_green)
        val normalWeekColor = ContextCompat.getColor(context, R.color.gray)
        val normalDayColor = ContextCompat.getColor(context, R.color.black)
        val whiteColor = ContextCompat.getColor(context, android.R.color.white)

        vDayBackground.backgroundTintList =
            if (isSelected) ColorStateList.valueOf(selectedColor) else null

        tvWeekDay.setTextColor(if (isSelected) selectedColor else normalWeekColor)
        tvDayNumber.setTextColor(if (isSelected) whiteColor else normalDayColor)

        val total = stats[date]?.total ?: 0
        if (total <= 0) {
            ivDiseaseDot.visibility = View.GONE
        } else {
            ivDiseaseDot.setImageResource(
                if (total < 5) R.drawable.calendar_yellow_disease
                else R.drawable.calendar_disease
            )
            ivDiseaseDot.visibility = View.VISIBLE
        }

        dayView.setOnClickListener {
            onDateClick(date)
        }
    }

    override fun getItemCount(): Int = weekStartDates.size

    fun updateStats(newStats: Map<LocalDate, CalendarDayStat>) {
        stats = newStats
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: LocalDate) {
        if (selectedDate == date) return

        val oldSelectedDate = selectedDate
        selectedDate = date

        val oldWeekIndex = weekStartDates.indexOf(startOfWeek(oldSelectedDate))
        val newWeekIndex = weekStartDates.indexOf(startOfWeek(selectedDate))

        if (oldWeekIndex != -1) notifyItemChanged(oldWeekIndex)
        if (newWeekIndex != -1 && newWeekIndex != oldWeekIndex) notifyItemChanged(newWeekIndex)
    }

    fun weekIndexOf(date: LocalDate): Int = weekStartDates.indexOf(startOfWeek(date))

    fun dateAtSameWeekday(pageIndex: Int, baseDate: LocalDate): LocalDate {
        val safeIndex = pageIndex.coerceIn(weekStartDates.indices)
        val weekdayOffset = baseDate.dayOfWeek.value.toLong() - 1L // 월=0, 일=6
        return weekStartDates[safeIndex].plusDays(weekdayOffset)
    }

    private fun startOfWeek(date: LocalDate): LocalDate {
        return date.minusDays(date.dayOfWeek.value.toLong() - 1L)
    }

    class WeekViewHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container)
}
