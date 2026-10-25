package com.minimal.tvlauncher.manager

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException

/**
 * Zero-bloat, thread-safe implementation of [IPreferencesManager] using native [SharedPreferences].
 *
 * Guarantees:
 * 1. Zero Memory Leaks: References strictly [Context.getApplicationContext], never retaining Activity references.
 * 2. Thread Safety: Compound read-modify-write operations are guarded with internal synchronization locks.
 * 3. Dual Write Modes: Default asynchronous writes ([SharedPreferences.Editor.apply]) for 60fps UI performance,
 *    with optional synchronous disk flushes ([SharedPreferences.Editor.commit]) for unit tests or shutdown.
 * 4. Fault Tolerance & Corruption Recovery: Corrupted JSON strings, unexpected ClassCastExceptions,
 *    and malformed parameters gracefully fall back to safe defaults without throwing exceptions or crashing.
 * 5. Strict Bounds & Input Clamping: Grid column numbers are clamped to [3..6], invalid shapes/topics fall back safely.
 */
class PreferencesManager @JvmOverloads constructor(
    context: Context,
    prefsName: String = PREFS_NAME,
    var isSyncMode: Boolean = false
) : IPreferencesManager {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    companion object {
        private val lock = Any()

        const val PREFS_NAME = "tv_launcher_prefs"

        // SharedPreferences Keys
        const val PREF_FAVORITES = "pref_favorites"
        const val PREF_HIDDEN_APPS = "pref_hidden_apps"
        const val PREF_GRID_COLUMNS = "pref_grid_columns"
        const val PREF_HIGH_CONTRAST = "pref_high_contrast"
        const val PREF_ICON_SHAPE = "pref_icon_shape"
        const val PREF_AUDIO_FEEDBACK = "pref_audio_feedback"

        // Top Shelf Key
        const val PREF_TOP_SHELF = "pref_top_shelf"
        const val DEFAULT_TOP_SHELF = true

        // Requirement R5: Wallpaper & Modular Status Widgets Keys
        const val PREF_BING_WALLPAPER = "pref_bing_wallpaper"
        const val PREF_WIDGET_RAM = "pref_widget_ram"
        const val PREF_WIDGET_NETWORK = "pref_widget_network"
        const val PREF_WIDGET_WEATHER = "pref_widget_weather"
        const val PREF_WIDGET_NEWS = "pref_widget_news"

        // Extended Wallpaper Sources Keys
        const val PREF_WALLPAPER_SOURCE = "pref_wallpaper_source"
        const val PREF_WALLPAPER_STORAGE_PATH = "pref_wallpaper_storage_path"
        const val PREF_WALLPAPER_GOOGLE_PHOTOS_URL = "pref_wallpaper_google_photos_url"

        const val WALLPAPER_DEFAULT = "default"
        const val WALLPAPER_BING = "bing"
        const val WALLPAPER_STORAGE = "storage"
        const val WALLPAPER_GOOGLE_PHOTOS = "google_photos"
        val VALID_WALLPAPER_SOURCES = setOf(WALLPAPER_DEFAULT, WALLPAPER_BING, WALLPAPER_STORAGE, WALLPAPER_GOOGLE_PHOTOS)
        const val DEFAULT_WALLPAPER_SOURCE = WALLPAPER_DEFAULT
        const val DEFAULT_WALLPAPER_STORAGE_PATH = "/sdcard/Pictures/wallpaper.jpg"
        const val SCENIC_WALLPAPER_RES_NAME = "bg_default_scenic"

        // Extended Weather Keys & Defaults
        const val PREF_WEATHER_AUTO_LOCATION = "pref_weather_auto_location"
        const val PREF_WEATHER_CITY = "pref_weather_city"
        const val PREF_WEATHER_LATITUDE = "pref_weather_latitude"
        const val PREF_WEATHER_LONGITUDE = "pref_weather_longitude"

        const val DEFAULT_WEATHER_AUTO_LOCATION = true
        const val DEFAULT_WEATHER_CITY = "Montréal"
        const val DEFAULT_WEATHER_LATITUDE = 45.5088
        const val DEFAULT_WEATHER_LONGITUDE = -73.5540

        // Extended Google News Keys & Defaults
        const val PREF_NEWS_TOPIC = "pref_news_topic"
        const val PREF_NEWS_CUSTOM_KEYWORD = "pref_news_custom_keyword"
        const val PREF_NEWS_TOPICS_SET = "pref_news_topics_set"
        const val PREF_NEWS_SOURCES_SET = "pref_news_sources_set"

        const val TOPIC_TOP_STORIES = "TOP_STORIES"
        const val TOPIC_TECHNOLOGY = "TECHNOLOGY"
        const val TOPIC_ENTERTAINMENT = "ENTERTAINMENT"
        const val TOPIC_SPORTS = "SPORTS"
        const val TOPIC_SCIENCE = "SCIENCE"
        const val TOPIC_WORLD = "WORLD"
        const val TOPIC_BUSINESS = "BUSINESS"
        const val TOPIC_CUSTOM = "CUSTOM"
        val VALID_NEWS_TOPICS = setOf(
            TOPIC_TOP_STORIES, TOPIC_TECHNOLOGY, TOPIC_ENTERTAINMENT,
            TOPIC_SPORTS, TOPIC_SCIENCE, TOPIC_WORLD, TOPIC_BUSINESS, TOPIC_CUSTOM
        )
        const val DEFAULT_NEWS_TOPIC = TOPIC_TOP_STORIES

        val PREDEFINED_NEWS_SOURCES = listOf(
            "Radio-Canada",
            "La Presse",
            "Le Devoir",
            "TVA Nouvelles",
            "Le Monde",
            "IGN",
            "RDS"
        )

        // Grid Column Bounds & Defaults (Legacy Grid: 6 default, valid 3, 4, 6)
        const val DEFAULT_GRID_COLUMNS = 6
        const val MIN_GRID_COLUMNS = 3
        const val MAX_GRID_COLUMNS = 6
        val VALID_GRID_CHOICES = setOf(3, 4, 6)

        // Drawer Column Bounds & Defaults (5 columns default aligning with 5-app dock)
        const val PREF_DRAWER_COLUMNS = "pref_drawer_columns"
        const val DEFAULT_DRAWER_COLUMNS = 5

        // Scroll Mode Keys & Defaults
        const val PREF_SCROLL_MODE = "pref_scroll_mode"
        const val SCROLL_MODE_SMOOTH = "smooth"
        const val SCROLL_MODE_PAGE = "page"
        const val DEFAULT_SCROLL_MODE = SCROLL_MODE_SMOOTH
        val VALID_SCROLL_MODES = setOf(SCROLL_MODE_SMOOTH, SCROLL_MODE_PAGE)

        // Accessibility & Visual Defaults
        const val DEFAULT_HIGH_CONTRAST = false
        const val DEFAULT_AUDIO_FEEDBACK = true

        // Requirement R5 Defaults
        const val DEFAULT_BING_WALLPAPER = false
        const val DEFAULT_WIDGET_RAM = false
        const val DEFAULT_WIDGET_NETWORK = false
        const val DEFAULT_WIDGET_WEATHER = false
        const val DEFAULT_WIDGET_NEWS = false

        // Sports Widget Keys & Defaults
        const val PREF_WIDGET_SPORTS = "pref_widget_sports"
        const val DEFAULT_WIDGET_SPORTS = false
        const val PREF_SPORTS_TYPE = "pref_sports_type"
        const val DEFAULT_SPORTS_TYPE = "NHL"
        const val PREF_SPORTS_SOURCE = "pref_sports_source"
        const val DEFAULT_SPORTS_SOURCE = "ESPN"
        const val PREF_SPORTS_FAVORITE_TEAM = "pref_sports_favorite_team"

        // Platform Override Key
        const val PREF_PLATFORM_OVERRIDE = "pref_platform_override"
        const val DEFAULT_PLATFORM_OVERRIDE = "AUTO"

        // Icon Shape Constants & Defaults
        const val SHAPE_SQUIRCLE = "squircle"
        const val SHAPE_CIRCLE = "circle"
        const val SHAPE_ROUNDED_RECT = "rounded_rect"
        const val DEFAULT_ICON_SHAPE = SHAPE_SQUIRCLE
        val VALID_ICON_SHAPES = setOf(SHAPE_SQUIRCLE, SHAPE_CIRCLE, SHAPE_ROUNDED_RECT)
    }

    // ==========================================
    // Favorites Management (Ordered JSON Array)
    // ==========================================

    override fun getFavorites(): List<String> {
        val rawJson = try {
            prefs.getString(PREF_FAVORITES, null)
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        val trimmed = rawJson.trim()
        if (trimmed.isBlank() || !trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()

        return try {
            val jsonArray = JSONArray(trimmed)
            val list = ArrayList<String>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i, "").trim()
                if (item.isNotEmpty() && !list.contains(item)) {
                    list.add(item)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun setFavorites(packages: List<String>) {
        setFavorites(packages, isSyncMode)
    }

    fun setFavorites(packages: List<String>, sync: Boolean): Boolean {
        synchronized(lock) {
            val sanitizedList = ArrayList<String>(packages.size)
            for (pkg in packages) {
                val clean = pkg.trim()
                if (clean.isNotEmpty() && !sanitizedList.contains(clean)) {
                    sanitizedList.add(clean)
                }
            }
            val jsonArray = JSONArray()
            for (pkg in sanitizedList) {
                jsonArray.put(pkg)
            }
            return prefs.edit().putString(PREF_FAVORITES, jsonArray.toString()).save(sync)
        }
    }

    override fun addFavorite(packageName: String) {
        addFavorite(packageName, isSyncMode)
    }

    fun addFavorite(packageName: String, sync: Boolean): Boolean {
        if (packageName.isBlank()) return false
        synchronized(lock) {
            val current = getFavorites().toMutableList()
            if (!current.contains(packageName)) {
                current.add(packageName)
                return setFavorites(current, sync)
            }
            return true
        }
    }

    override fun removeFavorite(packageName: String) {
        removeFavorite(packageName, isSyncMode)
    }

    fun removeFavorite(packageName: String, sync: Boolean): Boolean {
        if (packageName.isBlank()) return false
        synchronized(lock) {
            val current = getFavorites().toMutableList()
            if (current.remove(packageName)) {
                return setFavorites(current, sync)
            }
            return true
        }
    }

    override fun isFavorite(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return getFavorites().contains(packageName)
    }

    // ==========================================
    // Hidden Apps Management (Defensive Set)
    // ==========================================

    override fun getHiddenApps(): Set<String> {
        val rawSet = try {
            prefs.getStringSet(PREF_HIDDEN_APPS, null)
        } catch (e: Exception) {
            null
        }
        return rawSet?.toSet() ?: emptySet()
    }

    override fun setAppHidden(packageName: String, hidden: Boolean) {
        setAppHidden(packageName, hidden, isSyncMode)
    }

    fun setAppHidden(packageName: String, hidden: Boolean, sync: Boolean): Boolean {
        if (packageName.isBlank()) return false
        synchronized(lock) {
            val current = getHiddenApps().toMutableSet()
            val changed = if (hidden) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            return if (changed) {
                prefs.edit().putStringSet(PREF_HIDDEN_APPS, current).save(sync)
            } else {
                true
            }
        }
    }

    override fun isAppHidden(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return getHiddenApps().contains(packageName)
    }

    // ==========================================
    // Visual Modes & Layout Preferences
    // ==========================================

    override fun isHighContrast(): Boolean {
        return try {
            prefs.getBoolean(PREF_HIGH_CONTRAST, DEFAULT_HIGH_CONTRAST)
        } catch (e: Exception) {
            DEFAULT_HIGH_CONTRAST
        }
    }

    override fun setHighContrast(enabled: Boolean) {
        setHighContrast(enabled, isSyncMode)
    }

    fun setHighContrast(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_HIGH_CONTRAST, enabled).save(sync)
    }

    override fun getGridColumns(): Int {
        val raw = try {
            prefs.getInt(PREF_GRID_COLUMNS, DEFAULT_GRID_COLUMNS)
        } catch (e: Exception) {
            DEFAULT_GRID_COLUMNS
        }
        return raw.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    override fun setGridColumns(columns: Int) {
        setGridColumns(columns, isSyncMode)
    }

    fun setGridColumns(columns: Int, sync: Boolean): Boolean {
        val clamped = columns.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
        return prefs.edit().putInt(PREF_GRID_COLUMNS, clamped).save(sync)
    }

    override fun getDrawerColumns(): Int {
        val raw = try {
            prefs.getInt(PREF_DRAWER_COLUMNS, DEFAULT_DRAWER_COLUMNS)
        } catch (e: Exception) {
            DEFAULT_DRAWER_COLUMNS
        }
        return raw.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    override fun setDrawerColumns(columns: Int) {
        setDrawerColumns(columns, isSyncMode)
    }

    fun setDrawerColumns(columns: Int, sync: Boolean): Boolean {
        val clamped = columns.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
        return prefs.edit().putInt(PREF_DRAWER_COLUMNS, clamped).save(sync)
    }

    override fun getScrollMode(): String {
        val raw = try {
            prefs.getString(PREF_SCROLL_MODE, DEFAULT_SCROLL_MODE)
        } catch (e: Exception) {
            DEFAULT_SCROLL_MODE
        }
        return if (raw != null && raw in VALID_SCROLL_MODES) raw else DEFAULT_SCROLL_MODE
    }

    override fun setScrollMode(mode: String) {
        setScrollMode(mode, isSyncMode)
    }

    fun setScrollMode(mode: String, sync: Boolean): Boolean {
        val sanitized = if (mode in VALID_SCROLL_MODES) mode else DEFAULT_SCROLL_MODE
        return prefs.edit().putString(PREF_SCROLL_MODE, sanitized).save(sync)
    }

    override fun getIconShape(): String {
        val raw = try {
            prefs.getString(PREF_ICON_SHAPE, DEFAULT_ICON_SHAPE)
        } catch (e: Exception) {
            DEFAULT_ICON_SHAPE
        }
        return if (raw != null && raw in VALID_ICON_SHAPES) raw else DEFAULT_ICON_SHAPE
    }

    override fun setIconShape(shape: String) {
        setIconShape(shape, isSyncMode)
    }

    fun setIconShape(shape: String, sync: Boolean): Boolean {
        val sanitized = if (shape in VALID_ICON_SHAPES) shape else DEFAULT_ICON_SHAPE
        return prefs.edit().putString(PREF_ICON_SHAPE, sanitized).save(sync)
    }

    // ==========================================
    // D-pad Navigation Audio Feedback
    // ==========================================

    override fun isAudioFeedbackEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_AUDIO_FEEDBACK, DEFAULT_AUDIO_FEEDBACK)
        } catch (e: Exception) {
            DEFAULT_AUDIO_FEEDBACK
        }
    }

    override fun setAudioFeedbackEnabled(enabled: Boolean) {
        setAudioFeedbackEnabled(enabled, isSyncMode)
    }

    fun setAudioFeedbackEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_AUDIO_FEEDBACK, enabled).save(sync)
    }

    // ==========================================
    // Top Shelf (Apple TV style preview)
    // ==========================================

    override fun isTopShelfEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_TOP_SHELF, DEFAULT_TOP_SHELF)
        } catch (e: Exception) {
            DEFAULT_TOP_SHELF
        }
    }

    override fun setTopShelfEnabled(enabled: Boolean) {
        setTopShelfEnabled(enabled, isSyncMode)
    }

    fun setTopShelfEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_TOP_SHELF, enabled).save(sync)
    }

    // ==========================================
    // Wallpaper Preferences & Sources
    // ==========================================

    override fun isBingWallpaperEnabled(): Boolean {
        return try {
            val source = getWallpaperSource()
            if (source == WALLPAPER_DEFAULT) {
                false
            } else {
                source == WALLPAPER_BING || prefs.getBoolean(PREF_BING_WALLPAPER, DEFAULT_BING_WALLPAPER)
            }
        } catch (e: Exception) {
            DEFAULT_BING_WALLPAPER
        }
    }

    /**
     * Checks if the default scenic alpine wallpaper (bg_default_scenic) is active and visible.
     */
    fun isDefaultScenicWallpaperActive(): Boolean {
        return getWallpaperSource() == WALLPAPER_DEFAULT && !isHighContrast()
    }

    /**
     * Ensures the scenic alpine wallpaper is active and visible by resetting wallpaper source to default
     * and disabling high contrast mode if necessary.
     */
    fun ensureDefaultScenicWallpaper(sync: Boolean = isSyncMode): Boolean {
        synchronized(lock) {
            val sourceChanged = setWallpaperSource(WALLPAPER_DEFAULT, sync)
            val contrastChanged = setHighContrast(false, sync)
            val bingChanged = prefs.edit().putBoolean(PREF_BING_WALLPAPER, false).save(sync)
            return sourceChanged && contrastChanged && bingChanged
        }
    }

    /**
     * Resets wallpaper source to default alpine scenic background.
     */
    fun resetWallpaperToDefault(sync: Boolean = isSyncMode): Boolean {
        return setWallpaperSource(WALLPAPER_DEFAULT, sync)
    }

    override fun setBingWallpaperEnabled(enabled: Boolean) {
        setBingWallpaperEnabled(enabled, isSyncMode)
    }

    fun setBingWallpaperEnabled(enabled: Boolean, sync: Boolean): Boolean {
        setWallpaperSource(if (enabled) WALLPAPER_BING else WALLPAPER_DEFAULT, sync)
        return prefs.edit().putBoolean(PREF_BING_WALLPAPER, enabled).save(sync)
    }

    override fun getWallpaperSource(): String {
        val raw = try {
            prefs.getString(PREF_WALLPAPER_SOURCE, DEFAULT_WALLPAPER_SOURCE)
        } catch (e: Exception) {
            DEFAULT_WALLPAPER_SOURCE
        }
        return if (raw != null && raw in VALID_WALLPAPER_SOURCES) raw else DEFAULT_WALLPAPER_SOURCE
    }

    override fun setWallpaperSource(source: String) {
        setWallpaperSource(source, isSyncMode)
    }

    fun setWallpaperSource(source: String, sync: Boolean): Boolean {
        val sanitized = if (source in VALID_WALLPAPER_SOURCES) source else DEFAULT_WALLPAPER_SOURCE
        return prefs.edit().putString(PREF_WALLPAPER_SOURCE, sanitized).save(sync)
    }

    override fun getWallpaperStoragePath(): String {
        return try {
            prefs.getString(PREF_WALLPAPER_STORAGE_PATH, DEFAULT_WALLPAPER_STORAGE_PATH) ?: DEFAULT_WALLPAPER_STORAGE_PATH
        } catch (e: Exception) {
            DEFAULT_WALLPAPER_STORAGE_PATH
        }
    }

    override fun setWallpaperStoragePath(path: String) {
        setWallpaperStoragePath(path, isSyncMode)
    }

    fun setWallpaperStoragePath(path: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_WALLPAPER_STORAGE_PATH, path.trim()).save(sync)
    }

    override fun getGooglePhotosUrl(): String {
        return try {
            prefs.getString(PREF_WALLPAPER_GOOGLE_PHOTOS_URL, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun setGooglePhotosUrl(url: String) {
        setGooglePhotosUrl(url, isSyncMode)
    }

    fun setGooglePhotosUrl(url: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_WALLPAPER_GOOGLE_PHOTOS_URL, url.trim()).save(sync)
    }

    // ==========================================
    // Modular Status Widgets
    // ==========================================

    override fun isRamWidgetEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_WIDGET_RAM, DEFAULT_WIDGET_RAM)
        } catch (e: Exception) {
            DEFAULT_WIDGET_RAM
        }
    }

    override fun setRamWidgetEnabled(enabled: Boolean) {
        setRamWidgetEnabled(enabled, isSyncMode)
    }

    fun setRamWidgetEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_WIDGET_RAM, enabled).save(sync)
    }

    override fun isNetworkWidgetEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_WIDGET_NETWORK, DEFAULT_WIDGET_NETWORK)
        } catch (e: Exception) {
            DEFAULT_WIDGET_NETWORK
        }
    }

    override fun setNetworkWidgetEnabled(enabled: Boolean) {
        setNetworkWidgetEnabled(enabled, isSyncMode)
    }

    fun setNetworkWidgetEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_WIDGET_NETWORK, enabled).save(sync)
    }

    // ==========================================
    // Weather Preferences
    // ==========================================

    override fun isWeatherWidgetEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_WIDGET_WEATHER, DEFAULT_WIDGET_WEATHER)
        } catch (e: Exception) {
            DEFAULT_WIDGET_WEATHER
        }
    }

    override fun setWeatherWidgetEnabled(enabled: Boolean) {
        setWeatherWidgetEnabled(enabled, isSyncMode)
    }

    fun setWeatherWidgetEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_WIDGET_WEATHER, enabled).save(sync)
    }

    override fun isWeatherAutoLocation(): Boolean {
        return try {
            prefs.getBoolean(PREF_WEATHER_AUTO_LOCATION, DEFAULT_WEATHER_AUTO_LOCATION)
        } catch (e: Exception) {
            DEFAULT_WEATHER_AUTO_LOCATION
        }
    }

    override fun setWeatherAutoLocation(auto: Boolean) {
        setWeatherAutoLocation(auto, isSyncMode)
    }

    fun setWeatherAutoLocation(auto: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_WEATHER_AUTO_LOCATION, auto).save(sync)
    }

    override fun getWeatherCity(): String {
        return try {
            prefs.getString(PREF_WEATHER_CITY, DEFAULT_WEATHER_CITY) ?: DEFAULT_WEATHER_CITY
        } catch (e: Exception) {
            DEFAULT_WEATHER_CITY
        }
    }

    override fun setWeatherCity(city: String) {
        setWeatherCity(city, isSyncMode)
    }

    fun setWeatherCity(city: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_WEATHER_CITY, city.trim()).save(sync)
    }

    override fun getWeatherLatitude(): Double {
        return try {
            prefs.getString(PREF_WEATHER_LATITUDE, null)?.toDoubleOrNull() ?: DEFAULT_WEATHER_LATITUDE
        } catch (e: Exception) {
            DEFAULT_WEATHER_LATITUDE
        }
    }

    override fun getWeatherLongitude(): Double {
        return try {
            prefs.getString(PREF_WEATHER_LONGITUDE, null)?.toDoubleOrNull() ?: DEFAULT_WEATHER_LONGITUDE
        } catch (e: Exception) {
            DEFAULT_WEATHER_LONGITUDE
        }
    }

    override fun setWeatherCoordinates(latitude: Double, longitude: Double) {
        setWeatherCoordinates(latitude, longitude, isSyncMode)
    }

    fun setWeatherCoordinates(latitude: Double, longitude: Double, sync: Boolean): Boolean {
        return prefs.edit()
            .putString(PREF_WEATHER_LATITUDE, latitude.toString())
            .putString(PREF_WEATHER_LONGITUDE, longitude.toString())
            .save(sync)
    }

    // ==========================================
    // Google News Preferences (Topics & Sources)
    // ==========================================

    override fun isNewsWidgetEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_WIDGET_NEWS, DEFAULT_WIDGET_NEWS)
        } catch (e: Exception) {
            DEFAULT_WIDGET_NEWS
        }
    }

    override fun setNewsWidgetEnabled(enabled: Boolean) {
        setNewsWidgetEnabled(enabled, isSyncMode)
    }

    fun setNewsWidgetEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_WIDGET_NEWS, enabled).save(sync)
    }

    override fun getNewsTopic(): String {
        val raw = try {
            prefs.getString(PREF_NEWS_TOPIC, DEFAULT_NEWS_TOPIC)
        } catch (e: Exception) {
            DEFAULT_NEWS_TOPIC
        }
        return if (raw != null && raw in VALID_NEWS_TOPICS) raw else DEFAULT_NEWS_TOPIC
    }

    override fun setNewsTopic(topic: String) {
        setNewsTopic(topic, isSyncMode)
    }

    fun setNewsTopic(topic: String, sync: Boolean): Boolean {
        val sanitized = if (topic in VALID_NEWS_TOPICS) topic else DEFAULT_NEWS_TOPIC
        return prefs.edit().putString(PREF_NEWS_TOPIC, sanitized).save(sync)
    }

    override fun getNewsTopics(): Set<String> {
        val rawSet = try {
            prefs.getStringSet(PREF_NEWS_TOPICS_SET, null)
        } catch (e: Exception) {
            null
        }
        return rawSet?.toSet() ?: emptySet()
    }

    override fun setNewsTopics(topics: Set<String>) {
        setNewsTopics(topics, isSyncMode)
    }

    fun setNewsTopics(topics: Set<String>, sync: Boolean): Boolean {
        return prefs.edit().putStringSet(PREF_NEWS_TOPICS_SET, topics).save(sync)
    }

    override fun getNewsSources(): Set<String> {
        val rawSet = try {
            prefs.getStringSet(PREF_NEWS_SOURCES_SET, null)
        } catch (e: Exception) {
            null
        }
        return rawSet?.toSet() ?: emptySet()
    }

    override fun setNewsSources(sources: Set<String>) {
        setNewsSources(sources, isSyncMode)
    }

    fun setNewsSources(sources: Set<String>, sync: Boolean): Boolean {
        return prefs.edit().putStringSet(PREF_NEWS_SOURCES_SET, sources).save(sync)
    }

    override fun getNewsCustomKeyword(): String {
        return try {
            prefs.getString(PREF_NEWS_CUSTOM_KEYWORD, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun setNewsCustomKeyword(keyword: String) {
        setNewsCustomKeyword(keyword, isSyncMode)
    }

    fun setNewsCustomKeyword(keyword: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_NEWS_CUSTOM_KEYWORD, keyword.trim()).save(sync)
    }

    // ==========================================
    // Sports Widget Preferences
    // ==========================================

    override fun isSportsWidgetEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_WIDGET_SPORTS, DEFAULT_WIDGET_SPORTS)
        } catch (e: Exception) {
            DEFAULT_WIDGET_SPORTS
        }
    }

    override fun setSportsWidgetEnabled(enabled: Boolean) {
        setSportsWidgetEnabled(enabled, isSyncMode)
    }

    fun setSportsWidgetEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_WIDGET_SPORTS, enabled).save(sync)
    }

    override fun getSportsType(): String {
        return try {
            prefs.getString(PREF_SPORTS_TYPE, DEFAULT_SPORTS_TYPE) ?: DEFAULT_SPORTS_TYPE
        } catch (e: Exception) {
            DEFAULT_SPORTS_TYPE
        }
    }

    override fun setSportsType(type: String) {
        setSportsType(type, isSyncMode)
    }

    fun setSportsType(type: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_SPORTS_TYPE, type).save(sync)
    }

    override fun getSportsSource(): String {
        return try {
            prefs.getString(PREF_SPORTS_SOURCE, DEFAULT_SPORTS_SOURCE) ?: DEFAULT_SPORTS_SOURCE
        } catch (e: Exception) {
            DEFAULT_SPORTS_SOURCE
        }
    }

    override fun setSportsSource(source: String) {
        setSportsSource(source, isSyncMode)
    }

    fun setSportsSource(source: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_SPORTS_SOURCE, source).save(sync)
    }

    override fun getSportsFavoriteTeam(): String {
        return try {
            prefs.getString(PREF_SPORTS_FAVORITE_TEAM, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    override fun setSportsFavoriteTeam(team: String) {
        setSportsFavoriteTeam(team, isSyncMode)
    }

    fun setSportsFavoriteTeam(team: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_SPORTS_FAVORITE_TEAM, team.trim()).save(sync)
    }

    // ==========================================
    // Platform Override Preferences
    // ==========================================

    override fun getPlatformOverride(): String {
        return try {
            prefs.getString(PREF_PLATFORM_OVERRIDE, DEFAULT_PLATFORM_OVERRIDE) ?: DEFAULT_PLATFORM_OVERRIDE
        } catch (e: Exception) {
            DEFAULT_PLATFORM_OVERRIDE
        }
    }

    override fun setPlatformOverride(override: String) {
        setPlatformOverride(override, isSyncMode)
    }

    fun setPlatformOverride(override: String, sync: Boolean): Boolean {
        return prefs.edit().putString(PREF_PLATFORM_OVERRIDE, override).save(sync)
    }

    // ==========================================
    // Maintenance & Utility Methods
    // ==========================================

    fun clear(sync: Boolean = isSyncMode): Boolean {
        synchronized(lock) {
            return prefs.edit().clear().save(sync)
        }
    }

    private fun SharedPreferences.Editor.save(sync: Boolean): Boolean {
        return if (sync) {
            commit()
        } else {
            apply()
            true
        }
    }
}
