package com.example.aos

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * assets/disease_calendar.json 을 읽어서 월별 병해 데이터를 제공한다.
 * 첫 호출 시 한 번만 파싱하고 메모리에 보관 (앱 생명주기 동안).
 */
object DiseaseCalendarRepository {

    data class MonthInfo(
        val month: Int,
        val season: String,
        val common: List<String>,
        val crops: List<CropDiseases>
    )

    data class CropDiseases(
        val cropName: String,
        val diseases: List<String>,
        val note: String
    )

    private var cache: Map<Int, MonthInfo>? = null

    fun get(context: Context, month: Int): MonthInfo? =
        load(context)[month]

    fun all(context: Context): List<MonthInfo> =
        (1..12).mapNotNull { load(context)[it] }

    private fun load(context: Context): Map<Int, MonthInfo> {

        cache?.let { return it }

        val json = context.assets.open("disease_calendar.json")
            .bufferedReader()
            .use { it.readText() }

        val root = Gson().fromJson(json, JsonObject::class.java)
        val calendar = root.getAsJsonObject("calendar")

        val map = mutableMapOf<Int, MonthInfo>()

        (1..12).forEach { m ->

            val monthObj = calendar.getAsJsonObject(m.toString()) ?: return@forEach

            val common = monthObj.getAsJsonArray("common")
                ?.map { it.asString } ?: emptyList()

            val season = monthObj.get("season")?.asString ?: ""

            val cropsObj = monthObj.getAsJsonObject("crops")
            val crops = cropsObj?.entrySet()?.map { entry ->
                val obj = entry.value.asJsonObject
                CropDiseases(
                    cropName = entry.key,
                    diseases = obj.getAsJsonArray("diseases")
                        ?.map { it.asString } ?: emptyList(),
                    note = obj.get("note")?.asString ?: ""
                )
            } ?: emptyList()

            map[m] = MonthInfo(
                month = m,
                season = season,
                common = common,
                crops = crops
            )
        }

        cache = map
        return map
    }
}
