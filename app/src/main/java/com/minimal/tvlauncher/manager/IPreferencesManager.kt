package com.minimal.tvlauncher.manager

/**
 * Contract for managing launcher preferences and persistent user configuration.
 *
 * Covers all storage requirements defined in PROJECT.md:
 * - Ordered favorites row persistence
 * - Hidden packages set persistence
 * - Dynamic grid/drawer column count (3, 4, 5, 6)
 * - OLED high-contrast visual mode
 * - Custom icon corner shape ("squircle", "circle", "rounded_rect")
 * - D-pad audio feedback toggle
 * - Dynamic wallpaper sources (Default alpine, Bing 4K, Local storage, Google Photos)
 * - Weather configuration (Auto IP geolocation or manual city search via Open-Meteo)
 * - Google News topics and news sources customization
 * - Drawer columns (5 default) & vertical scroll mode ("smooth", "page")
 */
interface IPreferencesManager {
    /**
     * Retrieves the ordered list of package names pinned to the favorites shelf.
     * Preserves user insertion order.
     * Returns an empty list if no favorites are set or if stored data is corrupted.
     */
    fun getFavorites(): List<String>

    /**
     * Replaces the entire list of favorite package names, preserving the provided list order.
     */
    fun setFavorites(packages: List<String>)

    /**
     * Appends a package name to the favorites shelf if not already present.
     * Preserves insertion order and prevents duplicates.
     */
    fun addFavorite(packageName: String)

    /**
     * Removes a package name from the favorites shelf.
     */
    fun removeFavorite(packageName: String)

    /**
     * Checks if a package name is currently pinned in the favorites shelf.
     */
    fun isFavorite(packageName: String): Boolean

    /**
     * Retrieves the set of package names hidden from the main app grid.
     * Returns an empty set if no apps are hidden or if stored data is corrupted.
     */
    fun getHiddenApps(): Set<String>

    /**
     * Adds or removes a package name from the hidden apps set.
     *
     * @param packageName Package to update.
     * @param hidden True to hide the app from the main grid, false to unhide.
     */
    fun setAppHidden(packageName: String, hidden: Boolean)

    /**
     * Checks if a package name is currently hidden.
     */
    fun isAppHidden(packageName: String): Boolean

    /**
     * Returns whether OLED High-Contrast Mode (#000000 background + #FFD600 border) is enabled.
     * Defaults to false.
     */
    fun isHighContrast(): Boolean

    /**
     * Enables or disables OLED High-Contrast Mode.
     */
    fun setHighContrast(enabled: Boolean)

    /**
     * Retrieves the current grid column count.
     * Clamped to [3, 6]. Default is 5.
     */
    fun getGridColumns(): Int

    /**
     * Updates the grid column count. Values outside [3, 6] are clamped to 3..6.
     */
    fun setGridColumns(columns: Int)

    /**
     * Retrieves the current drawer grid column count (4, 5, 6). Default is 5.
     */
    fun getDrawerColumns(): Int

    /**
     * Sets the drawer grid column count.
     */
    fun setDrawerColumns(columns: Int)

    /**
     * Retrieves the scroll transition mode ("smooth" or "page").
     */
    fun getScrollMode(): String

    /**
     * Sets the scroll transition mode ("smooth" or "page").
     */
    fun setScrollMode(mode: String)

    /**
     * Retrieves the current icon shape style.
     * Returns one of "squircle", "circle", or "rounded_rect". Default is "squircle".
     */
    fun getIconShape(): String

    /**
     * Updates the icon shape style. Invalid values fall back to "squircle".
     */
    fun setIconShape(shape: String)

    /**
     * Returns whether D-pad navigation sound feedback is enabled.
     * Defaults to true.
     */
    fun isAudioFeedbackEnabled(): Boolean

    /**
     * Enables or disables D-pad navigation sound feedback.
     */
    fun setAudioFeedbackEnabled(enabled: Boolean)

    /**
     * Returns whether the Apple TV style Top Shelf preview is enabled.
     * Defaults to true.
     */
    fun isTopShelfEnabled(): Boolean

    /**
     * Enables or disables the Apple TV style Top Shelf preview.
     */
    fun setTopShelfEnabled(enabled: Boolean)

    // ==========================================
    // Requirement R5: Wallpaper & Modular Status Widgets
    // ==========================================

    /**
     * Returns whether the Bing Daily Wallpaper ("Image of the Day" with dark scrim overlay)
     * is enabled in place of the default dark gradient.
     * Defaults to false.
     */
    fun isBingWallpaperEnabled(): Boolean

    /**
     * Enables or disables the Bing Daily Wallpaper.
     */
    fun setBingWallpaperEnabled(enabled: Boolean)

    /**
     * Retrieves the active wallpaper source ("default", "bing", "storage", "google_photos").
     */
    fun getWallpaperSource(): String

    /**
     * Sets the active wallpaper source.
     */
    fun setWallpaperSource(source: String)

    /**
     * Retrieves the custom local image path on TV storage.
     */
    fun getWallpaperStoragePath(): String

    /**
     * Sets the custom local image path on TV storage.
     */
    fun setWallpaperStoragePath(path: String)

    /**
     * Retrieves the Google Photos shared link / album feed URL.
     */
    fun getGooglePhotosUrl(): String

    /**
     * Sets the Google Photos shared link / album feed URL.
     */
    fun setGooglePhotosUrl(url: String)

    /**
     * Returns whether the Live RAM usage status widget (Free/Used MB) is enabled in the top status bar.
     * Defaults to false.
     */
    fun isRamWidgetEnabled(): Boolean

    /**
     * Enables or disables the Live RAM usage status widget.
     */
    fun setRamWidgetEnabled(enabled: Boolean)

    /**
     * Returns whether the Network status widget (Wi-Fi/Ethernet indicator) is enabled in the top status bar.
     * Defaults to false.
     */
    fun isNetworkWidgetEnabled(): Boolean

    /**
     * Enables or disables the Network status widget.
     */
    fun setNetworkWidgetEnabled(enabled: Boolean)

    /**
     * Returns whether the Weather status widget is enabled in the top status bar.
     * Defaults to false.
     */
    fun isWeatherWidgetEnabled(): Boolean

    /**
     * Enables or disables the Weather status widget.
     */
    fun setWeatherWidgetEnabled(enabled: Boolean)

    /**
     * Returns whether automatic IP geolocation is used for weather.
     */
    fun isWeatherAutoLocation(): Boolean

    /**
     * Enables or disables automatic IP geolocation for weather.
     */
    fun setWeatherAutoLocation(auto: Boolean)

    /**
     * Retrieves the configured weather city name.
     */
    fun getWeatherCity(): String

    /**
     * Sets the configured weather city name.
     */
    fun setWeatherCity(city: String)

    /**
     * Retrieves the cached weather latitude.
     */
    fun getWeatherLatitude(): Double

    /**
     * Retrieves the cached weather longitude.
     */
    fun getWeatherLongitude(): Double

    /**
     * Sets the cached weather coordinates.
     */
    fun setWeatherCoordinates(latitude: Double, longitude: Double)

    /**
     * Returns whether the Google News RSS headlines ticker widget is enabled in the top status bar.
     * Defaults to false.
     */
    fun isNewsWidgetEnabled(): Boolean

    /**
     * Enables or disables the Google News RSS headlines ticker widget.
     */
    fun setNewsWidgetEnabled(enabled: Boolean)

    /**
     * Retrieves the selected Google News primary topic string.
     */
    fun getNewsTopic(): String

    /**
     * Sets the selected Google News primary topic.
     */
    fun setNewsTopic(topic: String)

    /**
     * Retrieves the set of selected multiple topics (e.g. TECHNOLOGY, SPORTS, SCIENCE, etc.).
     */
    fun getNewsTopics(): Set<String>

    /**
     * Sets the multiple selected topics for Google News.
     */
    fun setNewsTopics(topics: Set<String>)

    /**
     * Retrieves the set of selected preferred news sources/newspapers.
     */
    fun getNewsSources(): Set<String>

    /**
     * Sets the multiple selected news sources.
     */
    fun setNewsSources(sources: Set<String>)

    /**
     * Retrieves the custom keyword query when topic is "CUSTOM" or custom search is active.
     */
    fun getNewsCustomKeyword(): String

    /**
     * Sets the custom keyword query.
     */
    fun setNewsCustomKeyword(keyword: String)

    // ==========================================
    // Sports Widget Preferences
    // ==========================================

    fun isSportsWidgetEnabled(): Boolean
    fun setSportsWidgetEnabled(enabled: Boolean)
    fun getSportsType(): String
    fun setSportsType(type: String)
    fun getSportsSource(): String
    fun setSportsSource(source: String)
    fun getSportsFavoriteTeam(): String
    fun setSportsFavoriteTeam(team: String)

    // ==========================================
    // TV Platform Override (Google TV vs Fire TV)
    // ==========================================

    fun getPlatformOverride(): String
    fun setPlatformOverride(override: String)
}
