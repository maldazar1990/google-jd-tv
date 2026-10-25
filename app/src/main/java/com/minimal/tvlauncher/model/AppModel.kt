package com.minimal.tvlauncher.model

import android.graphics.drawable.Drawable

/**
 * Lean, immutable model representing an application entry on the TV home screen.
 *
 * Adheres strictly to PROJECT.md interface contract:
 * - Fully immutable (all fields declared as [val]).
 * - Safe nullability: only visual drawables ([banner], [icon]) are nullable.
 * - Sane defaults for optional state flags and drawables.
 * - Standard Kotlin data class providing automatic [equals], [hashCode], [copy], and [toString].
 *
 * @property packageName Unique application package identifier (e.g., "com.google.android.youtube.tv").
 * @property activityName Fully qualified launch activity class name (e.g., "com.google.android.apps.youtube.tv.activity.ShellActivity").
 * @property label User-facing display title for the application.
 * @property isLeanback True if discovered via Leanback launcher intent (TV-optimized), false if mobile/sideloaded.
 * @property banner 16:9 widescreen TV banner drawable if provided by the application package, or null.
 * @property icon Standard application icon drawable (square/adaptive/legacy), or null.
 * @property isFavorite True if the user has pinned this application to the top favorites shelf.
 * @property isHidden True if the user has marked this application to be hidden from the main grid.
 * @property isSystemApp True if the application is pre-installed in the system partition (FLAG_SYSTEM).
 */
data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isLeanback: Boolean,
    val banner: Drawable? = null,
    val icon: Drawable? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isSystemApp: Boolean = false
) {
    /**
     * Unique component identifier combining package and activity names.
     */
    val componentKey: String
        get() = "$packageName/$activityName"
}
