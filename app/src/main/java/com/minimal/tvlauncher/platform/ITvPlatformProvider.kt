package com.minimal.tvlauncher.platform

import android.content.Context

/**
 * Abstraction layer for Android TV vs Amazon Fire TV OEM environments.
 * Normalizes app store deep links, launcher package overrides, and system settings intents.
 */
interface ITvPlatformProvider {
    val platformType: TvPlatformType
    val defaultLauncherPackage: String
    val settingsAction: String
    val appStorePackage: String
    fun openAppStore(context: Context, packageName: String): Boolean
}
