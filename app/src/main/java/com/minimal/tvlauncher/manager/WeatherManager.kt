package com.minimal.tvlauncher.manager

import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Ultra-lightweight weather provider for TV status bar.
 * Fetches current temperature without API keys via Open-Meteo strictly over HTTPS.
 * Has zero-drain in-memory caching (60 minutes).
 */
class WeatherManager {

    companion object {
        // Default coordinates (e.g., Montreal / Canada: 45.5088, -73.554)
        private const val WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=45.5088&longitude=-73.554&current=temperature_2m"
        private const val TIMEOUT_MS = 4000
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 hour
    }

    private var cachedWeather: String? = null
    private var lastFetchTime: Long = 0L

    fun getWeatherText(forceRefresh: Boolean = false): String {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedWeather != null && (now - lastFetchTime) < CACHE_DURATION_MS) {
            return cachedWeather!!
        }

        val freshWeather = fetchCurrentTemperature()
        val result = freshWeather ?: (cachedWeather ?: "21°C")
        if (freshWeather != null) {
            cachedWeather = result
            lastFetchTime = now
        }
        return result
    }

    private fun fetchCurrentTemperature(): String? {
        return try {
            val url = URL(WEATHER_URL)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"

            if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }

            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val jsonRoot = JSONObject(jsonText)
            val currentObj = jsonRoot.getJSONObject("current")
            val temp = currentObj.getDouble("temperature_2m")
            "${temp.toInt()}°C"
        } catch (_: Exception) {
            null
        }
    }
}
