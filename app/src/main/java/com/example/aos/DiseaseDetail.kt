package com.example.aos

data class DiseaseDetail(
    val sickNameKor: String,
    val developmentCondition: String,  // ResultFragment 간략 설명 (발생생태)
    val symptoms: String,              // DetailFragment 증상
    val preventionMethod: String,      // DetailFragment 대처법
    val chemicalPrvnbeMth: String,     // DetailFragment 화학적방제방법 (보조)
    val imageUrls: List<String>        // DetailFragment 병피해 사진 (최대 2장 표시)
)