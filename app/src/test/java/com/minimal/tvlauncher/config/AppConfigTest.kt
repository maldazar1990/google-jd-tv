package com.minimal.tvlauncher.config

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppConfigTest {

    @Test
    fun allEndpoints_areStrictlyHttps() {
        assertThat(AppConfig.Endpoints.BING_WALLPAPER_API).startsWith("https://")
        assertThat(AppConfig.Endpoints.GOOGLE_NEWS_BASE_RSS).startsWith("https://")
        assertThat(AppConfig.Endpoints.OPEN_METEO_FORECAST_API).startsWith("https://")
        assertThat(AppConfig.Endpoints.OPEN_METEO_GEOCODING_API).startsWith("https://")
        assertThat(AppConfig.Endpoints.IP_GEOLOCATION_API).startsWith("https://")
        assertThat(AppConfig.Endpoints.ESPN_SCOREBOARD_BASE_URL).startsWith("https://")
    }

    @Test
    fun uiConstants_areWithinSafeBounds() {
        assertThat(AppConfig.Ui.DOCK_MAX_APPS).isEqualTo(5)
        assertThat(AppConfig.Ui.DRAWER_DEFAULT_COLUMNS).isEqualTo(5)
        assertThat(AppConfig.Ui.CARD_WIDTH_DP).isGreaterThan(AppConfig.Ui.CARD_HEIGHT_DP)
        assertThat(AppConfig.Ui.CARD_CORNER_RADIUS_DP).isAtLeast(10f)
    }

    @Test
    fun hardwareLimits_enforceBudget() {
        assertThat(AppConfig.Hardware.MAX_LAUNCHER_RAM_CEILING_MB).isEqualTo(30)
        assertThat(AppConfig.Hardware.WALLPAPER_TARGET_WIDTH).isEqualTo(1920)
        assertThat(AppConfig.Hardware.WALLPAPER_TARGET_HEIGHT).isEqualTo(1080)
    }
}
