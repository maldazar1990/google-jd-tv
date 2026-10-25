package com.minimal.tvlauncher.manager

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Lightweight, non-blocking RSS news feed fetcher for Google News.
 * Strictly adheres to Android TV low-RAM limits by using streaming XmlPullParser
 * instead of DOM/SAX full tree memory loading.
 */
class NewsManager {

    companion object {
        const val RSS_URL = "https://news.google.com/rss?hl=fr&gl=FR&ceid=FR:fr"
        private const val TIMEOUT_MS = 5000
        private const val MAX_HEADLINES = 5
        private const val CACHE_EXPIRY_MS = 20 * 60 * 1000L // 20 minutes
    }

    private var cachedTickerText: String? = null
    private var lastFetchTimeMs: Long = 0L

    /**
     * Retrieves the news ticker headline text.
     * Uses in-memory cache if still fresh. Must be called from a background thread when fetching.
     */
    fun getNewsTickerText(forceRefresh: Boolean = false): String {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedTickerText != null && (now - lastFetchTimeMs) < CACHE_EXPIRY_MS) {
            return cachedTickerText!!
        }

        val headlines = fetchHeadlines()
        val formatted = if (headlines.isNotEmpty()) {
            "Google News : " + headlines.joinToString("   •   ")
        } else {
            cachedTickerText ?: "Google News : Actualités en direct disponibles en ligne"
        }

        if (headlines.isNotEmpty()) {
            cachedTickerText = formatted
            lastFetchTimeMs = now
        }

        return formatted
    }

    /**
     * Fetches and parses RSS stream over HTTPS.
     */
    fun fetchHeadlines(): List<String> {
        return try {
            val url = URL(RSS_URL)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
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
                            // Strip redundant "- SourceName" at end of Google News headlines if desired
                            headlines.add(cleanHeadline(text))
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

    private fun cleanHeadline(raw: String): String {
        return raw.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
    }
}
