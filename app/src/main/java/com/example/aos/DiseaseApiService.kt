package com.example.aos

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 농촌진흥청 NCPMS 병해 API 서비스
 * SVC01: 병 검색 (cropName + sickNameKor → sickKey 획득)
 * SVC05: 병 상세정보 (sickKey → DiseaseDetail 획득)
 *
 * ※ 반드시 백그라운드 스레드(Dispatchers.IO)에서 호출할 것
 */
object DiseaseApiService {

    private const val BASE_URL = "http://ncpms.rda.go.kr/npmsAPI/service"
    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * SVC01: cropName + diseaseName으로 sickKey 조회
     * @return sickKey 문자열 (예: "D00001166"), 없으면 null
     */
    fun getSickKey(cropName: String, diseaseName: String): String? {
        val apiKey = BuildConfig.DISEASE_API_KEY
        val url = "$BASE_URL?apiKey=$apiKey" +
                "&serviceCode=SVC01" +
                "&serviceType=AA003:JSON" +
                "&cropName=${encode(cropName)}" +
                "&sickNameKor=${encode(diseaseName)}" +
                "&displayCount=1"

        return try {
            val response = execute(url) ?: return null
            val items = response
                .getAsJsonObject("service")
                ?.getAsJsonArray("list")
                ?: return null

            if (items.size() == 0) return null
            items[0].asJsonObject.get("sickKey")?.asString
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * SVC05: sickKey로 병 상세정보 조회
     * @return DiseaseDetail (실패 시 null)
     */
    fun getDetail(sickKey: String): DiseaseDetail? {
        val apiKey = BuildConfig.DISEASE_API_KEY
        val url = "$BASE_URL?apiKey=$apiKey" +
                "&serviceCode=SVC05" +
                "&serviceType=AA003:JSON" +
                "&sickKey=${encode(sickKey)}"

        return try {
            val response = execute(url) ?: return null
            val service = response.getAsJsonObject("service") ?: return null

            // imageList 파싱 (배열 또는 단일 객체 방어 처리)
            val imageUrls = mutableListOf<String>()
            try {
                val imageList = service.get("imageList")
                when {
                    imageList == null || imageList.isJsonNull -> Unit
                    imageList.isJsonArray -> {
                        imageList.asJsonArray.forEach { el ->
                            el.asJsonObject.get("image")?.asString?.let { imageUrls.add(it) }
                        }
                    }
                    imageList.isJsonObject -> {
                        imageList.asJsonObject.get("image")?.let { img ->
                            if (img.isJsonArray) img.asJsonArray.forEach { imageUrls.add(it.asString) }
                            else if (!img.isJsonNull) imageUrls.add(img.asString)
                        }
                    }
                }
            } catch (_: Exception) {}

            DiseaseDetail(
                sickNameKor          = service.getString("sickNameKor"),
                developmentCondition = service.getString("developmentCondition"),
                symptoms             = service.getString("symptoms"),
                preventionMethod     = service.getString("preventionMethod"),
                chemicalPrvnbeMth    = service.getString("chemicalPrvnbeMth"),
                imageUrls            = imageUrls
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ──────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────

    private fun execute(url: String): JsonObject? {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        return gson.fromJson(body, JsonObject::class.java)
    }

    private fun JsonObject.getString(key: String): String =
        this.get(key)?.let {
            if (it.isJsonNull) "" else it.asString
        } ?: ""

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
}