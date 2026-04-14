package com.example.aos

import retrofit2.http.GET
import retrofit2.http.Query

interface DiseaseApiService {

    /**
     * [1단계] 병 검색 API (SVC01)
     * cropName + sickNameKor 로 검색 → sickKey 획득
     */
    @GET("service")
    suspend fun searchDisease(
        @Query("apiKey")       apiKey: String,
        @Query("serviceCode")  serviceCode: String = "SVC01",
        @Query("serviceType")  serviceType: String = "AA003",  // JSON 타입
        @Query("cropName")     cropName: String,
        @Query("sickNameKor")  sickNameKor: String,
        @Query("displayCount") displayCount: Int = 10,
        @Query("startPoint")   startPoint: Int = 1
    ): DiseaseSearchResponse

    /**
     * [2단계] 병 상세 API (SVC05)
     * 1단계에서 받은 sickKey로 증상·방제법 조회
     */
    @GET("service")
    suspend fun getDiseaseDetail(
        @Query("apiKey")      apiKey: String,
        @Query("serviceCode") serviceCode: String = "SVC05",
        @Query("serviceType") serviceType: String = "AA003",
        @Query("sickKey")     sickKey: Int
    ): DiseaseDetailResponse
}