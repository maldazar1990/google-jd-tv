package com.minimal.tvlauncher.config

/**
 * Centralized in-memory configuration registry.
 * Houses all remote API endpoints, timeouts, cache durations, and UI constants.
 * Loaded once into RAM at startup to avoid hardcoded strings across managers and views.
 */
object AppConfig {

    // ==========================================
    // Remote HTTPS Endpoints (Strictly TLS / Jules Rule 2)
    // ==========================================
    object Endpoints {
        const val BING_WALLPAPER_API = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=fr-FR"
        const val BING_IMAGE_BASE_URL = "https://www.bing.com"

        const val GOOGLE_NEWS_BASE_RSS = "https://news.google.com/rss?hl=fr&gl=FR&ceid=FR:fr"
        const val GOOGLE_NEWS_TOPIC_PREFIX = "https://news.google.com/rss/headlines/section/topic/"
        const val GOOGLE_NEWS_SEARCH_PREFIX = "https://news.google.com/rss/search?q="

        const val OPEN_METEO_FORECAST_API = "https://api.open-meteo.com/v1/forecast?current=temperature_2m,weather_code"
        const val OPEN_METEO_GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=fr&format=json&name="
        const val IP_GEOLOCATION_API = "https://ipapi.co/json/"

        const val ESPN_SCOREBOARD_BASE_URL = "https://site.api.espn.com/apis/site/v2/sports/"
    }

    // ==========================================
    // Network Timeouts & Throttling
    // ==========================================
    object Timeouts {
        const val NETWORK_CONNECT_MS = 5000
        const val NETWORK_READ_MS = 5000
    }

    // ==========================================
    // Cache Durations (Zero Background Drain)
    // ==========================================
    object Cache {
        const val NEWS_CACHE_DURATION_MS = 20 * 60 * 1000L      // 20 minutes
        const val WEATHER_CACHE_DURATION_MS = 60 * 60 * 1000L   // 1 hour
        const val SPORTS_CACHE_DURATION_MS = 5 * 60 * 1000L      // 5 minutes
    }

    // ==========================================
    // Layout & Ergonomic Constants
    // ==========================================
    object Ui {
        const val DOCK_MAX_APPS = 5
        const val DRAWER_DEFAULT_COLUMNS = 5
        const val CARD_WIDTH_DP = 152
        const val CARD_HEIGHT_DP = 94
        const val CARD_CORNER_RADIUS_DP = 18f
        const val CARD_ICON_PADDING_DP = 8
        const val FOCUS_SCALE_FACTOR = 1.10f
        const val FOCUS_ANIMATION_DURATION_MS = 120L
    }

    // ==========================================
    // Strict Hardware Limits (Chromecast 4K)
    // ==========================================
    object Hardware {
        const val MAX_LAUNCHER_RAM_CEILING_MB = 30
        const val WALLPAPER_TARGET_WIDTH = 1920
        const val WALLPAPER_TARGET_HEIGHT = 1080
        const val BITMAP_LRU_CACHE_MAX_MB = 8
    }
}
