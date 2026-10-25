package com.minimal.tvlauncher.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class FireTvPlatformProvider : ITvPlatformProvider {
    override val platformType: TvPlatformType = TvPlatformType.FIRE_TV
    override val defaultLauncherPackage: String = "com.amazon.tv.launcher"
    override val settingsAction: String = "com.amazon.device.settings.SETTINGS"
    override val appStorePackage: String = "com.amazon.venezia"

    override fun openAppStore(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("amzn://apps/android?p=$packageName")).apply {
                setPackage(appStorePackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val genericIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
