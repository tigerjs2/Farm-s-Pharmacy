package com.example.aos

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MapDiseaseStatsRepository {

    enum class Region(val label: String) {
        GYEONGGI("경기"),
        GANGWON("강원"),
        CHUNGCHEONG("충청"),
        JEOLLA("전라"),
        GYEONGSANG("경상"),
        JEJU("제주")
    }

    data class Summary(
        val total: Int,
        val regionStats: Map<Region, Int>,
        val damageLines: List<String>
    )

    private const val DEDUP_RADIUS_METERS = 1000.0

    fun loadSummary(
        crop: String = "전체",
        disease: String = "전체",
        onSuccess: (Summary) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        var query: Query = FirebaseFirestore.getInstance()
            .collection("Diagnoses")

        if (crop != "전체") {
            query = query.whereEqualTo("cropType", crop)
        }

        if (disease != "전체") {
            query = query.whereEqualTo("diseaseName", disease)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val stats = aggregateWithDedup(snapshot.documents)
                onSuccess(buildSummary(stats, crop, disease))
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun emptyStats(): MutableMap<Region, Int> {
        return Region.values().associateWith { 0 }.toMutableMap()
    }

    fun emptySummary(
        crop: String = "전체",
        disease: String = "전체"
    ): Summary {
        return buildSummary(emptyStats(), crop, disease)
    }

    private fun buildSummary(
        stats: Map<Region, Int>,
        crop: String,
        disease: String
    ): Summary {
        val normalizedStats = emptyStats()
        normalizedStats.putAll(stats)

        val total = normalizedStats.values.sum()
        val damageLines = makeDamageLines(normalizedStats, crop, disease)

        return Summary(
            total = total,
            regionStats = normalizedStats,
            damageLines = damageLines
        )
    }

    /**
     * MapFragment와 동일하게
     * cropType + diseaseName 그룹별로 1km 이내 중복 진단을 1건으로 처리한다.
     */
    private fun aggregateWithDedup(
        docs: List<DocumentSnapshot>
    ): Map<Region, Int> {
        data class Entry(
            val region: Region,
            val location: GeoPoint?
        )

        val groups = mutableMapOf<Pair<String, String>, MutableList<Entry>>()

        docs.forEach { doc ->
            val crop = doc.getString("cropType") ?: return@forEach
            val disease = doc.getString("diseaseName") ?: return@forEach
            val region = addressToRegion(doc.getString("addressDo") ?: "") ?: return@forEach
            val location = doc.getGeoPoint("location")

            groups.getOrPut(crop to disease) { mutableListOf() }
                .add(Entry(region, location))
        }

        val stats = emptyStats()

        groups.values.forEach { entries ->
            val kept = mutableListOf<GeoPoint>()

            entries.forEach { entry ->
                val loc = entry.location

                val isDuplicate = loc != null && kept.any { existing ->
                    haversineMeters(existing, loc) < DEDUP_RADIUS_METERS
                }

                if (!isDuplicate) {
                    if (loc != null) kept.add(loc)
                    stats[entry.region] = (stats[entry.region] ?: 0) + 1
                }
            }
        }

        return stats
    }

    private fun makeDamageLines(
        stats: Map<Region, Int>,
        crop: String,
        disease: String
    ): List<String> {
        val sorted = stats
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }

        if (sorted.isEmpty()) {
            return listOf("질병 피해 없습니다!")
        }

        val cropText = if (crop == "전체") "작물" else crop
        val diseaseText = if (disease == "전체") "병해" else disease

        val lines = mutableListOf<String>()

        val first = sorted[0]
        lines += "${first.first.label}도 인근 ${cropText} ${diseaseText} ${damageWord(first.second)}"

        if (sorted.size >= 2) {
            val second = sorted[1]
            lines += "${second.first.label}도 ${cropText} ${diseaseText} ${second.second}건 발생"
        }

        return lines
    }

    private fun damageWord(count: Int): String {
        return if (count > 5) "급증" else "발생"
    }

    private fun addressToRegion(addressDo: String): Region? {
        val text = addressDo.trim()

        return when {
            text.contains("서울") ||
                    text.contains("인천") ||
                    text.contains("경기") -> Region.GYEONGGI

            text.contains("강원") -> Region.GANGWON

            text.contains("충북") ||
                    text.contains("충남") ||
                    text.contains("충청") ||
                    text.contains("대전") ||
                    text.contains("세종") -> Region.CHUNGCHEONG

            text.contains("전북") ||
                    text.contains("전남") ||
                    text.contains("전라") ||
                    text.contains("광주") -> Region.JEOLLA

            text.contains("경북") ||
                    text.contains("경남") ||
                    text.contains("경상") ||
                    text.contains("대구") ||
                    text.contains("부산") ||
                    text.contains("울산") -> Region.GYEONGSANG

            text.contains("제주") -> Region.JEJU

            else -> null
        }
    }

    private fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
        val earthRadius = 6371000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)

        val h = sin(dLat / 2).let { it * it } +
                cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }

        return 2 * earthRadius * atan2(sqrt(h), sqrt(1 - h))
    }
}