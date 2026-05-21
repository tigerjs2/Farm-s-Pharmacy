package com.example.aos

import com.google.firebase.firestore.GeoPoint

/**
 * 진단 위치 더미 생성기.
 * 추후 GPS 권한 구현 시 이 파일은 제거하고 FusedLocationProvider로 대체한다.
 */
object RandomLocation {

    /** addressDo, lat 범위, lon 범위 (rough bounding box) */
    private data class RegionBox(
        val addressDo: String,
        val latMin: Double,
        val latMax: Double,
        val lonMin: Double,
        val lonMax: Double,
        val weight: Int
    )

    // 전남 일대를 더 자주 뽑되 다른 권역도 가끔 나오게 weight 부여.
    private val boxes = listOf(
        RegionBox("전남", 34.4, 35.5, 126.2, 127.4, weight = 5),
        RegionBox("전북", 35.4, 36.1, 126.5, 127.6, weight = 2),
        RegionBox("경기", 37.0, 37.9, 126.7, 127.5, weight = 2),
        RegionBox("강원", 37.5, 38.3, 127.6, 129.0, weight = 1),
        RegionBox("충남", 36.0, 37.0, 126.4, 127.4, weight = 1),
        RegionBox("충북", 36.3, 37.2, 127.4, 128.3, weight = 1),
        RegionBox("경남", 34.8, 35.7, 127.8, 129.2, weight = 1),
        RegionBox("경북", 35.7, 36.9, 128.0, 129.4, weight = 1),
        RegionBox("제주", 33.2, 33.6, 126.2, 126.9, weight = 1)
    )

    private val totalWeight = boxes.sumOf { it.weight }

    data class Random(val geoPoint: GeoPoint, val addressDo: String)

    fun next(): Random {
        var roll = kotlin.random.Random.nextInt(totalWeight)
        val box = boxes.first { b ->
            roll -= b.weight
            roll < 0
        }
        val lat = kotlin.random.Random.nextDouble(box.latMin, box.latMax)
        val lon = kotlin.random.Random.nextDouble(box.lonMin, box.lonMax)
        return Random(GeoPoint(lat, lon), box.addressDo)
    }
}
