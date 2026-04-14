package com.example.aos

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────
// [1단계] SVC01 검색 응답
// ─────────────────────────────────────────────
data class DiseaseSearchResponse(
    val service: SearchService
)

data class SearchService(
    val totalCount: Int,
    val list: List<SearchItem>?   // 결과 없으면 null
)

data class SearchItem(
    val cropName: String,
    val sickNameKor: String,
    val sickNameEng: String?,
    val thumbImg: String?,   // 썸네일 이미지 URL
    val oriImg: String?,     // 원본 이미지 URL
    val sickKey: Int         // ★ 2단계에서 사용할 키
)

// ─────────────────────────────────────────────
// [2단계] SVC05 상세 응답
// ─────────────────────────────────────────────
data class DiseaseDetailResponse(
    val service: DetailService
)

data class DetailService(
    val cropName: String,
    val sickNameKor: String,
    val sickNameEng: String?,
    val infectionRoute: String?,       // 전염경로
    val developmentCondition: String?, // 발생생태
    val symptoms: String?,             // 병 증상 ★
    val preventionMethod: String?,     // 방제방법 ★
    val biologyPrvnbeMth: String?,     // 생물학적방제방법
    val chemicalPrvnbeMth: String?,    // 화학적방제방법
    val imageList: List<DiseaseImage>? // 병피해 사진 목록
)

data class DiseaseImage(
    val image: String?,
    val imageTitle: String?
)

// ─────────────────────────────────────────────
// 앱 내부 모델 (기존 유지)
// ─────────────────────────────────────────────
data class DiseaseInfo(
    val diseaseName: String,
    val briefDesc: String,
    val symptoms: List<String>,
    val treatments: List<String>,
    val imageUrls: List<String> = emptyList()  // ← 이미지 URL 추가
)