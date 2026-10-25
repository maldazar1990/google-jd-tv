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
 * 5. Strict Bounds & Input Clamping: Invalid column numbers are clamped to [3..6], invalid shapes fall back to "squircle".
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

        // Requirement R5: Wallpaper & Modular Status Widgets Keys
        const val PREF_BING_WALLPAPER = "pref_bing_wallpaper"
        const val PREF_WIDGET_RAM = "pref_widget_ram"
        const val PREF_WIDGET_NETWORK = "pref_widget_network"
        const val PREF_WIDGET_WEATHER = "pref_widget_weather"
        const val PREF_WIDGET_NEWS = "pref_widget_news"

        // Grid Column Bounds & Defaults
        const val DEFAULT_GRID_COLUMNS = 6
        const val MIN_GRID_COLUMNS = 3
        const val MAX_GRID_COLUMNS = 6
        val VALID_GRID_CHOICES = setOf(3, 4, 6)

        // Accessibility & Visual Defaults
        const val DEFAULT_HIGH_CONTRAST = false
        const val DEFAULT_AUDIO_FEEDBACK = true

        // Requirement R5 Defaults
        const val DEFAULT_BING_WALLPAPER = false
        const val DEFAULT_WIDGET_RAM = false
        const val DEFAULT_WIDGET_NETWORK = false
        const val DEFAULT_WIDGET_WEATHER = false
        const val DEFAULT_WIDGET_NEWS = false

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
            val result = ArrayList<String>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val pkg = jsonArray.optString(i, null)
                if (!pkg.isNullOrBlank()) {
                    result.add(pkg)
                }
            }
            result
        } catch (e: Exception) {
            // Graceful corruption recovery: malformed JSON returns empty list
            emptyList()
        }
    }

    override fun setFavorites(packages: List<String>) {
        setFavorites(packages, isSyncMode)
    }

    fun setFavorites(packages: List<String>, sync: Boolean): Boolean {
        synchronized(lock) {
            val nonNullPackages = packages.filter { it.isNotBlank() }
            val jsonArray = JSONArray()
            for (pkg in nonNullPackages) {
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
        // Defensive copy prevents caller mutation from corrupting SharedPreferences cache
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
                prefs.edit().putStringSet(PREF_HIDDEN_APPS, HashSet(current)).save(sync)
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
    // High-Contrast Mode (#000000 & #FFD600)
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

    // ==========================================
    // Dynamic Grid Columns (3, 4, 6)
    // ==========================================

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

    // ==========================================
    // Icon Corner Shapes ("squircle", "circle", "rounded_rect")
    // ==========================================

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
    // Requirement R5: Wallpaper & Modular Status Widgets
    // ==========================================

    override fun isBingWallpaperEnabled(): Boolean {
        return try {
            prefs.getBoolean(PREF_BING_WALLPAPER, DEFAULT_BING_WALLPAPER)
        } catch (e: Exception) {
            DEFAULT_BING_WALLPAPER
        }
    }

    override fun setBingWallpaperEnabled(enabled: Boolean) {
        setBingWallpaperEnabled(enabled, isSyncMode)
    }

    fun setBingWallpaperEnabled(enabled: Boolean, sync: Boolean): Boolean {
        return prefs.edit().putBoolean(PREF_BING_WALLPAPER, enabled).save(sync)
    }

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

    // ==========================================
    // Maintenance & Utility Methods
    // ==========================================

    /**
     * Clears all stored preferences. Useful for testing and factory resets.
     */
    fun clear(sync: Boolean = isSyncMode): Boolean {
        synchronized(lock) {
            return prefs.edit().clear().save(sync)
        }
    }

    /**
     * Internal persistence dispatcher: invokes [commit] if [sync] is true,
     * otherwise dispatches [apply] asynchronously and returns true.
     */
    private fun SharedPreferences.Editor.save(sync: Boolean): Boolean {
        return if (sync) {
            commit()
        } else {
            apply()
            true
        }
    }
}
