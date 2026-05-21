package com.example.aos

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    /**
     * Firestore의 Diagnoses(현재 사용자) + Storage 동기화 결과를 바탕으로
     * 날짜별 병해 통계를 만든다. Storage에서 사라진 이미지는 자동 정리된다.
     *
     * 거리/작물/병해 종류 구분 없이 진단 1건 = 병해 1건으로 카운트한다.
     * (지도 페이지의 거리 기반 카운트는 추후 별도 구현)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun load(): Map<LocalDate, CalendarDayStat> {
        val diagnoses = DiagnosisRepository.syncAndLoadForCurrentUser()
        return computeStats(diagnoses)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun computeStats(diagnoses: List<Diagnosis>): Map<LocalDate, CalendarDayStat> {
        val zone = ZoneId.systemDefault()
        val result = linkedMapOf<LocalDate, MutableCalendarDayStat>()

        diagnoses.forEach { d ->
            if (d.diagType != "DISEASE") return@forEach

            val date = Instant.ofEpochMilli(d.timestampMillis).atZone(zone).toLocalDate()
            val stat = result.getOrPut(date) { MutableCalendarDayStat() }

            stat.total += 1
            if (d.isHandled) stat.treated += 1
            if (d.diseaseName.isNotBlank()) stat.diseaseNames.add(d.diseaseName)
        }

        return result.mapValues { (_, value) ->
            CalendarDayStat(
                total = value.total,
                treated = value.treated,
                diseaseNames = value.diseaseNames.toSet()
            )
        }
    }

    private data class MutableCalendarDayStat(
        var total: Int = 0,
        var treated: Int = 0,
        val diseaseNames: MutableSet<String> = linkedSetOf()
    )
}
