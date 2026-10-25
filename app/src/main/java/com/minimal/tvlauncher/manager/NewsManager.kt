package com.minimal.tvlauncher.manager

import android.util.Xml
import com.minimal.tvlauncher.config.AppConfig
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * Lightweight, non-blocking RSS news feed fetcher for Google News.
 * Strictly adheres to Android TV low-RAM limits by using streaming XmlPullParser.
 * Supports combined multi-topic selection, publisher sources filtering, and custom search keywords.
 */
class NewsManager {

    companion object {
        const val RSS_URL = AppConfig.Endpoints.GOOGLE_NEWS_BASE_RSS
        private const val MAX_HEADLINES = 5
    }

    private var cachedTickerText: String? = null
    private var lastFetchTimeMs: Long = 0L
    private var lastQuerySignature: String = ""

    fun buildRssUrl(
        topics: Set<String> = emptySet(),
        sources: Set<String> = emptySet(),
        customKeyword: String = "",
        legacyTopic: String = PreferencesManager.TOPIC_TOP_STORIES
    ): String {
        if (topics.isEmpty() && sources.isEmpty() && customKeyword.isBlank()) {
            return when (legacyTopic) {
                PreferencesManager.TOPIC_TECHNOLOGY ->
                    "${AppConfig.Endpoints.GOOGLE_NEWS_TOPIC_PREFIX}TECHNOLOGY?hl=fr&gl=FR&ceid=FR:fr"
                PreferencesManager.TOPIC_ENTERTAINMENT ->
                    "${AppConfig.Endpoints.GOOGLE_NEWS_TOPIC_PREFIX}ENTERTAINMENT?hl=fr&gl=FR&ceid=FR:fr"
                PreferencesManager.TOPIC_SPORTS ->
                    "${AppConfig.Endpoints.GOOGLE_NEWS_TOPIC_PREFIX}SPORTS?hl=fr&gl=FR&ceid=FR:fr"
                PreferencesManager.TOPIC_SCIENCE ->
                    "${AppConfig.Endpoints.GOOGLE_NEWS_TOPIC_PREFIX}SCIENCE?hl=fr&gl=FR&ceid=FR:fr"
                else -> RSS_URL
            }
        }

        val parts = mutableListOf<String>()

        // 1. Sources filter: (source:"Radio-Canada" OR source:"La Presse")
        if (sources.isNotEmpty()) {
            val srcList = sources.joinToString(" OR ") { "source:\"$it\"" }
            parts.add("($srcList)")
        }

        // 2. Topics keywords: (Technologie OR Sports)
        if (topics.isNotEmpty()) {
            val topicKeywords = topics.mapNotNull {
                when (it) {
                    PreferencesManager.TOPIC_TECHNOLOGY -> "Technologie"
                    PreferencesManager.TOPIC_ENTERTAINMENT -> "Cinéma OR Séries"
                    PreferencesManager.TOPIC_SPORTS -> "Sports"
                    PreferencesManager.TOPIC_SCIENCE -> "Sciences OR Espace"
                    PreferencesManager.TOPIC_WORLD -> "Monde"
                    PreferencesManager.TOPIC_BUSINESS -> "Économie"
                    else -> null
                }
            }
            if (topicKeywords.isNotEmpty()) {
                parts.add("(${topicKeywords.joinToString(" OR ")})")
            }
        }

        // 3. Custom search term
        if (customKeyword.isNotBlank()) {
            parts.add(customKeyword.trim())
        }

        val combinedQuery = parts.joinToString(" ")
        val encodedQuery = try {
            URLEncoder.encode(combinedQuery, "UTF-8")
        } catch (_: Exception) {
            combinedQuery
        }

        return "${AppConfig.Endpoints.GOOGLE_NEWS_SEARCH_PREFIX}$encodedQuery&hl=fr&gl=FR&ceid=FR:fr"
    }

    /**
     * Retrieves the news ticker headline text for the specified filters.
     */
    fun getNewsTickerText(
        topics: Set<String> = emptySet(),
        sources: Set<String> = emptySet(),
        customKeyword: String = "",
        legacyTopic: String = PreferencesManager.TOPIC_TOP_STORIES,
        forceRefresh: Boolean = false
    ): String {
        val now = System.currentTimeMillis()
        val querySignature = "${topics.sorted().joinToString()}|${sources.sorted().joinToString()}|$customKeyword|$legacyTopic"
        val queryChanged = querySignature != lastQuerySignature

        if (!forceRefresh && !queryChanged && cachedTickerText != null && (now - lastFetchTimeMs) < AppConfig.Cache.NEWS_CACHE_DURATION_MS) {
            return cachedTickerText!!
        }

        val urlString = buildRssUrl(topics, sources, customKeyword, legacyTopic)
        val headlines = fetchHeadlines(urlString)

        val prefix = when {
            sources.isNotEmpty() -> sources.first()
            topics.isNotEmpty() -> "Actualités"
            customKeyword.isNotBlank() -> customKeyword.trim()
            else -> "Google News"
        }

        val formatted = if (headlines.isNotEmpty()) {
            "$prefix : " + headlines.joinToString("   •   ")
        } else {
            cachedTickerText ?: "$prefix : Actualités en direct disponibles en ligne"
        }

        if (headlines.isNotEmpty()) {
            cachedTickerText = formatted
            lastFetchTimeMs = now
            lastQuerySignature = querySignature
        }

        return formatted
    }

    /**
     * Fetches and parses RSS stream over HTTPS.
     */
    fun fetchHeadlines(urlString: String = RSS_URL): List<String> {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            conn.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
            conn.requestMethod = "GET"

            if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                conn.disconnect()
                return emptyList()
            }

            val headlines = conn.inputStream.use { stream ->
                parseRssStream(stream)
            }
            conn.disconnect()
            headlines
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Streaming XML pull parser for memory efficiency.
     */
    fun parseRssStream(inputStream: InputStream): List<String> {
        val headlines = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var insideItem = false
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT && headlines.size < MAX_HEADLINES) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("item", ignoreCase = true)) {
                        insideItem = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem && currentTag.equals("title", ignoreCase = true)) {
                        val text = parser.text?.trim()
                        if (!text.isNullOrEmpty() && !headlines.contains(text)) {
                            val cleanTitle = text.substringBeforeLast(" - ")
                            headlines.add(cleanTitle.ifEmpty { text })
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true)) {
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return headlines
    }
}
