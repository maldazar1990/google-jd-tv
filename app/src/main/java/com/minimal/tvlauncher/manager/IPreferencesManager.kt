package com.minimal.tvlauncher.manager

/**
 * Contract for managing launcher preferences and persistent user configuration.
 *
 * Covers all storage requirements defined in PROJECT.md:
 * - Ordered favorites row persistence
 * - Hidden packages set persistence
 * - Dynamic grid column count (3, 4, 6)
 * - OLED high-contrast visual mode
 * - Custom icon corner shape ("squircle", "circle", "rounded_rect")
 * - D-pad audio feedback toggle
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
     * Clamped to [3, 6]. Default is 6.
     */
    fun getGridColumns(): Int

    /**
     * Updates the grid column count. Values outside [3, 6] are clamped to 3..6.
     */
    fun setGridColumns(columns: Int)

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
     * Returns whether the Google News RSS headlines ticker widget is enabled in the top status bar.
     * Defaults to false.
     */
    fun isNewsWidgetEnabled(): Boolean

    /**
     * Enables or disables the Google News RSS headlines ticker widget.
     */
    fun setNewsWidgetEnabled(enabled: Boolean)
}

