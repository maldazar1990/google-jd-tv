package com.minimal.tvlauncher.manager

import com.minimal.tvlauncher.config.AppConfig
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * Ultra-lightweight weather provider for TV status bar.
 * Supports auto IP geolocation or manual city geocoding via Open-Meteo strictly over HTTPS.
 * Has zero-drain in-memory caching (60 minutes).
 */
class WeatherManager {

    private var cachedWeather: String? = null
    private var lastFetchTime: Long = 0L

    data class GeoLocation(val cityName: String, val latitude: Double, val longitude: Double)

    /**
     * Resolves coordinates from city name using Open-Meteo Geocoding API.
     */
    fun geocodeCity(cityName: String): GeoLocation? {
        if (cityName.isBlank()) return null
        return try {
            val encoded = URLEncoder.encode(cityName.trim(), "UTF-8")
            val url = URL(AppConfig.Endpoints.OPEN_METEO_GEOCODING_API + encoded)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            conn.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
            conn.requestMethod = "GET"

            if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val root = JSONObject(text)
            if (!root.has("results")) return null
            val results = root.getJSONArray("results")
            if (results.length() == 0) return null

            val first = results.getJSONObject(0)
            val name = first.optString("name", cityName)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            GeoLocation(name, lat, lon)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves location from public IP.
     */
    fun resolveIpLocation(): GeoLocation? {
        return try {
            val url = URL(AppConfig.Endpoints.IP_GEOLOCATION_API)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            conn.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "MinimalTVLauncher/1.0")

            if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val root = JSONObject(text)
            val city = root.optString("city", "Météo")
            val lat = root.getDouble("latitude")
            val lon = root.getDouble("longitude")
            GeoLocation(city, lat, lon)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retrieves weather text (e.g. "21°C" or "Montréal 21°C") given coordinates.
     */
    fun getWeatherText(
        latitude: Double = 45.5088,
        longitude: Double = -73.5540,
        cityName: String = "",
        forceRefresh: Boolean = false
    ): String {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedWeather != null && (now - lastFetchTime) < AppConfig.Cache.WEATHER_CACHE_DURATION_MS) {
            return cachedWeather!!
        }

        val temp = fetchTemperature(latitude, longitude)
        val formatted = if (temp != null) {
            if (cityName.isNotBlank()) "$cityName $temp" else temp
        } else {
            cachedWeather ?: (if (cityName.isNotBlank()) "$cityName 21°C" else "21°C")
        }

        if (temp != null) {
            cachedWeather = formatted
            lastFetchTime = now
        }
        return formatted
    }

    private fun fetchTemperature(lat: Double, lon: Double): String? {
        return try {
            val urlString = "${AppConfig.Endpoints.OPEN_METEO_FORECAST_API}&latitude=$lat&longitude=$lon"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            conn.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
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
