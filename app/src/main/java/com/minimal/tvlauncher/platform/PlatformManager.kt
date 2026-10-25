package com.minimal.tvlauncher.platform

import android.os.Build

class PlatformManager(private val platformOverride: String = PLATFORM_AUTO) {

    companion object {
        const val PLATFORM_AUTO = "AUTO"
        const val PLATFORM_GOOGLE_TV = "GOOGLE_TV"
        const val PLATFORM_FIRE_TV = "FIRE_TV"
    }

    private val isAmazonHardware: Boolean
        get() = Build.MANUFACTURER.contains("Amazon", ignoreCase = true)

    fun getActivePlatform(): ITvPlatformProvider {
        return when (platformOverride) {
            PLATFORM_FIRE_TV -> FireTvPlatformProvider()
            PLATFORM_GOOGLE_TV -> GoogleTvPlatformProvider()
            else -> {
                if (isAmazonHardware) {
                    FireTvPlatformProvider()
                } else {
                    GoogleTvPlatformProvider()
                }
            }
        }
    }
}
