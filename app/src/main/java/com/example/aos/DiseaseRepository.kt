package com.example.aos

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DiseaseRepository {

    private const val API_KEY  = "2026e15da9fdb78f73d9edaee0b7f516a7e7"   // ← 발급받은 키 입력
    private const val BASE_URL = "http://ncpms.rda.go.kr/npmsAPI/service/"

    private val service: DiseaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiseaseApiService::class.java)
    }

    /**
     * 외부에서 호출하는 메인 함수
     * cropName: 작물명 (예: "오이")
     * diseaseName: 모델이 반환한 병해명 (예: "노균병")
     */
    suspend fun getDiseaseInfo(cropName: String, diseaseName: String): DiseaseInfo? {
        return try {
            // ── 1단계: 병 검색 → sickKey 획득 ──────────────────
            val searchResponse = service.searchDisease(
                apiKey      = API_KEY,
                cropName    = cropName,
                sickNameKor = diseaseName
            )

            val sickKey = searchResponse.service.list
                ?.firstOrNull()
                ?.sickKey
                ?: return makeFallback(diseaseName)  // 결과 없으면 폴백

            // ── 2단계: 병 상세 → 증상·방제법 획득 ─────────────
            val detailResponse = service.getDiseaseDetail(
                apiKey  = API_KEY,
                sickKey = sickKey
            )

            val detail = detailResponse.service

            // 증상 문장 파싱 (마침표 기준 분리)
            val symptoms = parseToList(detail.symptoms)

            // 방제법 파싱 (일반 + 화학적 합치기)
            val generalTreatments  = parseToList(detail.preventionMethod)
            val chemicalTreatments = parseToList(detail.chemicalPrvnbeMth)
            val allTreatments      = (generalTreatments + chemicalTreatments).distinct()

            // 이미지 URL 수집
            val imageUrls = detail.imageList
                ?.mapNotNull { it.image }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            DiseaseInfo(
                diseaseName = detail.sickNameKor,
                briefDesc   = symptoms.firstOrNull() ?: "",
                symptoms    = symptoms,
                treatments  = allTreatments,
                imageUrls   = imageUrls
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 마침표 기준으로 문자열을 리스트로 분리 */
    private fun parseToList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(".")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { "$it." }
    }

    /** API 실패 시 최소한의 폴백 */
    private fun makeFallback(diseaseName: String) = DiseaseInfo(
        diseaseName = diseaseName,
        briefDesc   = "병해 정보를 불러오지 못했습니다.",
        symptoms    = listOf("병해 정보를 불러오지 못했습니다."),
        treatments  = listOf("전문가에게 문의하세요.")
    )
}