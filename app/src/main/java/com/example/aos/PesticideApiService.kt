package com.example.aos

import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * 농사로 농약등록현황 OpenAPI (pesticideRegStatus/pesticideRegStatusList)
 *
 * 주의:
 * - API의 sText/sType 파라미터는 실제 필터링이 동작하지 않는다 (echo만 됨).
 *   따라서 페이지네이션으로 받아서 클라이언트에서 필터링한다.
 * - 데이터가 작물명 가나다 순으로 정렬되어 있어, 후반부 작물(예: 파프리카)은
 *   여러 페이지를 돌아야 한다.
 * - 매칭 결과가 [maxResults]에 도달하면 즉시 중단해 응답 시간을 줄인다.
 *
 * ※ 반드시 백그라운드 스레드(Dispatchers.IO)에서 호출할 것.
 */
object PesticideApiService {

    private const val URL =
        "http://api.nongsaro.go.kr/service/pesticideRegStatus/pesticideRegStatusList"

    private const val PAGE_SIZE = 5000
    private const val MAX_PAGES = 30

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun searchByDisease(diseaseName: String, maxResults: Int = 100): List<Pesticide> {
        val keyword = diseaseName.trim()
        if (keyword.isEmpty()) return emptyList()
        return search(maxResults) { it.applcDbyhs.contains(keyword) }
    }

    fun searchByCrop(cropName: String, maxResults: Int = 300): List<Pesticide> {
        val name = cropName.trim()
        if (name.isEmpty()) return emptyList()
        return search(maxResults) { it.cropsNm == name }
    }

    private fun search(
        maxResults: Int,
        predicate: (Pesticide) -> Boolean
    ): List<Pesticide> {

        val results = mutableListOf<Pesticide>()
        var pageNo = 1

        while (pageNo <= MAX_PAGES) {

            val items = try {
                fetchPage(pageNo) ?: break
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }

            if (items.isEmpty()) break

            items.filter(predicate).forEach { results.add(it) }

            if (results.size >= maxResults) break
            if (items.size < PAGE_SIZE) break  // 마지막 페이지

            pageNo++
        }

        return results
    }

    private fun fetchPage(pageNo: Int): List<Pesticide>? {

        val apiKey = BuildConfig.NONGSARO_PESTICIDE_API_KEY

        val url = "$URL?apiKey=$apiKey" +
                "&pageNo=$pageNo" +
                "&numOfRows=$PAGE_SIZE"

        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        return parsePage(body)
    }

    private fun parsePage(xml: String): List<Pesticide> {

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val items = mutableListOf<Pesticide>()
        var inItem = false
        var currentTag: String? = null
        val fields = mutableMapOf<String, String>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "item") {
                        inItem = true
                        fields.clear()
                    } else if (inItem) {
                        currentTag = parser.name
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem && currentTag != null) {
                        val text = parser.text ?: ""
                        if (text.isNotEmpty()) {
                            fields[currentTag!!] =
                                (fields[currentTag!!] ?: "") + text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        items.add(
                            Pesticide(
                                cropsNm = fields["cropsNm"]?.trim() ?: "",
                                applcDbyhs = fields["applcDbyhs"]?.trim() ?: "",
                                brandNm = fields["brandNm"]?.trim() ?: "",
                                prdlstNm = fields["prdlstNm"]?.trim() ?: "",
                                prpos = fields["prpos"]?.trim() ?: "",
                                cmpnyNm = fields["cmpnyNm"]?.trim() ?: "",
                                actnNm = fields["actnNm"]?.trim() ?: "",
                                dcsnAt = fields["dcsnAt"]?.trim() ?: ""
                            )
                        )
                        inItem = false
                        fields.clear()
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        return items
    }
}
