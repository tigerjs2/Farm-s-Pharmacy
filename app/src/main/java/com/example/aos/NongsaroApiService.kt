package com.example.aos

import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * 농사로 OpenAPI
 * 농기구 사용 안전사고 정보: machineSafety/machineSafetyLst
 *
 * ※ 반드시 백그라운드 스레드(Dispatchers.IO)에서 호출할 것
 */
object NongsaroApiService {

    private const val TOOL_SAFETY_URL =
        "http://api.nongsaro.go.kr/service/machineSafety/machineSafetyLst"

    // 이미지 파일 경로의 베이스 도메인 (rtnFileCours가 상대경로로만 옴)
    private const val NONGSARO_FILE_BASE = "http://www.nongsaro.go.kr/"

    private val client = OkHttpClient()

    /**
     * 농기구 안전 지침 목록을 [knmcNm]으로 필터링해서 반환.
     * API의 sText는 제목/본문 검색이라 농기구 분류(knmcNm)와 매칭되지 않음.
     * 그래서 전체를 받아 와서 클라이언트에서 분류 일치 항목만 추린다.
     */
    /**
     * 전체 농기구 안전 지침 중에서 랜덤으로 1건 반환 ("오늘의 안전" 카드용).
     */
    fun getRandomToolGuide(): ToolGuide? {
        val apiKey = BuildConfig.NONGSARO_API_KEY
        val url = "$TOOL_SAFETY_URL?apiKey=$apiKey&pageNo=1&numOfRows=200"

        return try {
            val xml = fetch(url) ?: return null
            parseToolGuides(xml)
                .filter { it.title.isNotBlank() && it.imageUrl.isNotBlank() }
                .randomOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getToolGuides(
        knmcNm: String,
        pageNo: Int = 1,
        numOfRows: Int = 200
    ): List<ToolGuide> {

        val apiKey = BuildConfig.NONGSARO_API_KEY

        val url = "$TOOL_SAFETY_URL?apiKey=$apiKey" +
                "&pageNo=$pageNo" +
                "&numOfRows=$numOfRows"

        return try {
            val xml = fetch(url) ?: return emptyList()
            parseToolGuides(xml).filter { it.knmcNm == knmcNm }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun fetch(url: String): String? {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        return response.body?.string()
    }

    private fun parseToolGuides(xml: String): List<ToolGuide> {

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val items = mutableListOf<ToolGuide>()
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
                            ToolGuide(
                                cntntsNo = fields["cntntsNo"]?.trim() ?: "",
                                title = fields["cntntsSj"]?.trim() ?: "",
                                content = fields["cn"]?.trim() ?: "",
                                knmcNm = fields["knmcNm"]?.trim() ?: "",
                                safeacdntSeNm = fields["safeacdntSeNm"]?.trim() ?: "",
                                imageUrl = buildImageUrl(
                                    fields["rtnFileCours"]?.trim(),
                                    fields["rtnStreFileNm"]?.trim()
                                )
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

    private fun buildImageUrl(cours: String?, name: String?): String {
        if (cours.isNullOrBlank() || name.isNullOrBlank()) return ""
        val path = cours.trimStart('/')
        val sep = if (path.endsWith("/")) "" else "/"
        return "$NONGSARO_FILE_BASE$path$sep$name"
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
}
