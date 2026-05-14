package com.example.aos

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * 홈/캘린더 날짜 UI에서 공통으로 쓰는 날짜별 병해 통계.
 * total은 전체 병해 건수, treated는 처치 완료 건수이다.
 * 화면의 "병해 n건"은 total을 보여주고, 처리 필요 바만 untreated 비율로 줄어든다.
 */
data class CalendarDayStat(
    val total: Int = 0,
    val treated: Int = 0,
    val diseaseNames: Set<String> = emptySet()
) {
    val untreated: Int
        get() = (total - treated).coerceAtLeast(0)
}

object CalendarHistoryStats {

    @RequiresApi(Build.VERSION_CODES.O)
    private val dotFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    fun load(context: Context, gson: Gson = Gson()): Map<LocalDate, CalendarDayStat> {
        val prefs = context.getSharedPreferences("HistoryPrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("history_items", "[]") ?: "[]"
        val type = object : TypeToken<List<HistoryItem>>() {}.type

        val items: List<HistoryItem> = try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val result = linkedMapOf<LocalDate, MutableCalendarDayStat>()

        items.forEach { item ->
            if (item.diagType != "DISEASE") return@forEach

            val date = parseHistoryDate(item.date) ?: return@forEach
            val stat = result.getOrPut(date) { MutableCalendarDayStat() }

            stat.total += 1
            if (item.isTreated) stat.treated += 1
            if (item.diseaseName.isNotBlank()) stat.diseaseNames.add(item.diseaseName)
        }

        return result.mapValues { (_, value) ->
            CalendarDayStat(
                total = value.total,
                treated = value.treated,
                diseaseNames = value.diseaseNames.toSet()
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseHistoryDate(rawDate: String): LocalDate? {
        val value = rawDate.trim()
        if (value.isEmpty()) return null

        return try {
            LocalDate.parse(value, dotFormatter)      // yyyy.MM.dd
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(value)               // yyyy-MM-dd 방어 처리
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private data class MutableCalendarDayStat(
        var total: Int = 0,
        var treated: Int = 0,
        val diseaseNames: MutableSet<String> = linkedSetOf()
    )
}
