package com.minimal.tvlauncher.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class GoogleTvPlatformProvider : ITvPlatformProvider {
    override val platformType: TvPlatformType = TvPlatformType.GOOGLE_TV
    override val defaultLauncherPackage: String = "com.google.android.apps.tv.launcherx"
    override val settingsAction: String = Settings.ACTION_SETTINGS
    override val appStorePackage: String = "com.android.vending"

    override fun openAppStore(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage(appStorePackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
