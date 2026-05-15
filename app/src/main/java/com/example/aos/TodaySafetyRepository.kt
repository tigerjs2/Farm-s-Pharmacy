package com.example.aos

import android.content.Context
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 홈/가이드 화면의 "오늘의 안전" 카드를 하루 동안 같은 내용으로 공유한다.
 *
 * NongsaroApiService.getRandomToolGuide()는 호출할 때마다 랜덤 항목이 달라질 수 있으므로,
 * 오늘 날짜 기준으로 한 번 뽑은 ToolGuide를 SharedPreferences에 저장해 둔다.
 */
object TodaySafetyRepository {

    private const val PREF_NAME = "TodaySafetyPrefs"
    private const val KEY_DATE = "today_safety_date"
    private const val KEY_GUIDE_JSON = "today_safety_guide_json"

    private val gson = Gson()

    private var cachedDate: String? = null
    private var cachedGuide: ToolGuide? = null

    @Synchronized
    fun getTodaySafety(context: Context): ToolGuide? {
        val today = todayKey()

        if (cachedDate == today && cachedGuide != null) {
            return cachedGuide
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_DATE, null)
        val savedJson = prefs.getString(KEY_GUIDE_JSON, null)

        if (savedDate == today && !savedJson.isNullOrBlank()) {
            val savedGuide = runCatching {
                gson.fromJson(savedJson, ToolGuide::class.java)
            }.getOrNull()

            if (savedGuide != null && savedGuide.title.isNotBlank()) {
                cachedDate = today
                cachedGuide = savedGuide
                return savedGuide
            }
        }

        val newGuide = NongsaroApiService.getRandomToolGuide() ?: return null

        cachedDate = today
        cachedGuide = newGuide

        prefs.edit()
            .putString(KEY_DATE, today)
            .putString(KEY_GUIDE_JSON, gson.toJson(newGuide))
            .apply()

        return newGuide
    }

    fun toolNameOf(guide: ToolGuide): String {
        return guide.knmcNm
            .ifBlank { guide.safeacdntSeNm }
            .ifBlank { "방제기" }
    }

    fun splitBullets(content: String): List<String> {
        if (content.isBlank()) return emptyList()

        return content
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("</p\\s*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .split("\n")
            .map { it.trim().trimStart('·', '○', '•', '-', ' ') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
