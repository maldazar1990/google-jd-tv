package com.minimal.tvlauncher.manager

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.Display
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DisplayResolutionManagerTest {

    private lateinit var context: Context
    private lateinit var displayManager: DisplayResolutionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        displayManager = DisplayResolutionManager(context)
    }

    @Test
    fun classifyResolution_correctlyIdentifies4KUhd() {
        val result = displayManager.classifyResolution(3840, 2160)
        assertThat(result).isEqualTo(DisplayResolutionManager.ResolutionType.UHD_4K)
    }

    @Test
    fun classifyResolution_correctlyIdentifies1080pFhd() {
        val result = displayManager.classifyResolution(1920, 1080)
        assertThat(result).isEqualTo(DisplayResolutionManager.ResolutionType.FHD_1080P)
    }

    @Test
    fun classifyResolution_correctlyIdentifies720pHd() {
        val result = displayManager.classifyResolution(1280, 720)
        assertThat(result).isEqualTo(DisplayResolutionManager.ResolutionType.HD_720P)
    }

    @Test
    fun classifyResolution_correctlyIdentifiesStandardDef() {
        val result = displayManager.classifyResolution(640, 480)
        assertThat(result).isEqualTo(DisplayResolutionManager.ResolutionType.STANDARD_DEF)
    }

    @Test
    fun classifyRefreshRate_classifies24HzCinema() {
        val mode = displayManager.classifyRefreshRate(23.976f)
        assertThat(mode).isEqualTo(DisplayResolutionManager.RefreshRateMode.FPS_24)
    }

    @Test
    fun classifyRefreshRate_classifies50HzPal() {
        val mode = displayManager.classifyRefreshRate(50.0f)
        assertThat(mode).isEqualTo(DisplayResolutionManager.RefreshRateMode.FPS_50)
    }

    @Test
    fun classifyRefreshRate_classifies60HzStandard() {
        val mode = displayManager.classifyRefreshRate(60.0f)
        assertThat(mode).isEqualTo(DisplayResolutionManager.RefreshRateMode.FPS_60)
    }

    @Test
    fun classifyRefreshRate_classifies120HzHighFrameRate() {
        val mode = displayManager.classifyRefreshRate(120.0f)
        assertThat(mode).isEqualTo(DisplayResolutionManager.RefreshRateMode.FPS_120)
    }

    @Test
    fun calculateSafeInsets_computes5PercentOverscanMargins() {
        val insets = displayManager.calculateSafeInsets(horizontalPercent = 0.05f, verticalPercent = 0.05f)
        assertThat(insets.left).isAtLeast(0)
        assertThat(insets.top).isAtLeast(0)
        assertThat(insets.right).isAtLeast(0)
        assertThat(insets.bottom).isAtLeast(0)
    }

    @Test
    fun getDisplayInfo_returnsNonNullDisplayMetrics() {
        val info = displayManager.getDisplayInfo()
        assertThat(info.widthPixels).isGreaterThan(0)
        assertThat(info.heightPixels).isGreaterThan(0)
        assertThat(info.refreshRateHz).isGreaterThan(0f)
        assertThat(info.resolutionType).isNotNull()
    }

    // ==========================================
    // 4K Capability Detection & Labels
    // ==========================================

    @Test
    fun formatActiveModeLabel_formatsCorrectly() {
        assertThat(displayManager.formatActiveModeLabel(1280, 720, 60.0f))
            .isEqualTo("720p (1280×720 @ 60Hz)")
        assertThat(displayManager.formatActiveModeLabel(1920, 1080, 60.0f))
            .isEqualTo("1080p (1920×1080 @ 60Hz)")
        assertThat(displayManager.formatActiveModeLabel(3840, 2160, 60.0f))
            .isEqualTo("4K (3840×2160 @ 60Hz)")
    }

    @Test
    fun formatPhysicalTvLabel_formatsCorrectly() {
        assertThat(displayManager.formatPhysicalTvLabel(3840, 2160, hasHdr = true))
            .isEqualTo("4K Ultra HD (3840×2160 HDR)")
        assertThat(displayManager.formatPhysicalTvLabel(3840, 2160, hasHdr = false))
            .isEqualTo("4K Ultra HD (3840×2160)")
        assertThat(displayManager.formatPhysicalTvLabel(1920, 1080, hasHdr = false))
            .isEqualTo("Full HD (1920×1080)")
        assertThat(displayManager.formatPhysicalTvLabel(1280, 720, hasHdr = false))
            .isEqualTo("HD Ready (1280×720)")
    }

    @Test
    fun evaluateDisplayCapabilities_whenPhysicalTvSupports4K_andActiveIs720p_detects4KCapability() {
        val constructor = Display.Mode::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Float::class.javaPrimitiveType).apply { isAccessible = true }; val mode4k = constructor.newInstance(1, 3840, 2160, 60.0f); val mode720p = constructor.newInstance(2, 1280, 720, 60.0f)

        val modes = arrayOf(mode720p, mode4k)

        val info = displayManager.evaluateDisplayCapabilities(
            modes = modes,
            activeWidth = 1280,
            activeHeight = 720,
            activeRefreshRate = 60.0f,
            hasHdr = true
        )

        assertThat(info.isTv4KHardwareCapable).isTrue()
        assertThat(info.is4KCapable).isTrue()
        assertThat(info.physicalTvLabel).isEqualTo("4K Ultra HD (3840×2160 HDR)")
        assertThat(info.physicalTvName).isEqualTo("Téléviseur 4K Ultra HD")
        assertThat(info.displayBadgeText).isEqualTo("TV 4K Ultra HD détectée")
        assertThat(info.displaySubtitleText).isEqualTo("Rendu actuel : 720p @ 60Hz • Cliquer pour passer en 4K")
        assertThat(info.activeModeLabel).isEqualTo("720p (1280×720 @ 60Hz)")
        assertThat(info.resolutionType).isEqualTo(DisplayResolutionManager.ResolutionType.HD_720P)
    }

    @Test
    fun evaluateDisplayCapabilities_withTclTv_preservesPhysicalTvName() {
        val constructor = Display.Mode::class.java.getDeclaredConstructor(
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Float::class.javaPrimitiveType
        ).apply { isAccessible = true }
        val mode4k = constructor.newInstance(1, 3840, 2160, 60.0f)

        val info = displayManager.evaluateDisplayCapabilities(
            modes = arrayOf(mode4k),
            activeWidth = 1280,
            activeHeight = 720,
            activeRefreshRate = 60.0f,
            hasHdr = false,
            tvName = "TCL 55S535CA"
        )

        assertThat(info.is4KCapable).isTrue()
        assertThat(info.physicalTvName).isEqualTo("TCL 55S535CA")
        assertThat(info.displayBadgeText).isEqualTo("TV 4K Ultra HD détectée")
        assertThat(info.displaySubtitleText).isEqualTo("Rendu actuel : 720p @ 60Hz • Cliquer pour passer en 4K")
    }

    @Test
    fun evaluateDisplayCapabilities_whenActiveIs4K_detects4KCapability() {
        val info = displayManager.evaluateDisplayCapabilities(
            modes = null,
            activeWidth = 3840,
            activeHeight = 2160,
            activeRefreshRate = 60.0f,
            hasHdr = false
        )

        assertThat(info.isTv4KHardwareCapable).isTrue()
        assertThat(info.is4KCapable).isTrue()
        assertThat(info.resolutionType).isEqualTo(DisplayResolutionManager.ResolutionType.UHD_4K)
        assertThat(info.activeModeLabel).isEqualTo("4K (3840×2160 @ 60Hz)")
        assertThat(info.displayBadgeText).isEqualTo("TV 4K Ultra HD détectée")
        assertThat(info.displaySubtitleText).isEqualTo("Rendu actuel : 4K @ 60Hz (Optimal)")
    }

    // ==========================================
    // Quick Controls & Settings Intents
    // ==========================================

    @Test
    fun openDisplaySoundSettings_launchesGoogleTvDisplaySoundActivity() {
        val launched = displayManager.openDisplaySoundSettings(context)
        assertThat(launched).isTrue()

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.component?.packageName).isEqualTo("com.android.tv.settings")
        assertThat(startedIntent.component?.className).isEqualTo("com.android.tv.settings.device.displaysound.DisplaySoundActivity")
    }

    @Test
    fun openDisplaySettings_launchesIntent() {
        val launched = displayManager.openDisplaySettings()
        assertThat(launched).isTrue()

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        if (startedIntent.component != null) {
            assertThat(startedIntent.component?.className).isEqualTo("com.android.tv.settings.device.displaysound.DisplaySoundActivity")
        } else {
            assertThat(startedIntent.action).isAnyOf(
                "android.settings.DISPLAY_SETTINGS",
                "com.android.tv.settings.DISPLAY",
                Settings.ACTION_DISPLAY_SETTINGS,
                Settings.ACTION_SETTINGS
            )
        }
    }

    @Test
    fun openWifiSettings_launchesIntent() {
        val launched = displayManager.openWifiSettings()
        assertThat(launched).isTrue()

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isAnyOf(
            Settings.ACTION_WIFI_SETTINGS,
            Settings.ACTION_WIRELESS_SETTINGS,
            Settings.ACTION_SETTINGS
        )
    }

    @Test
    fun openBluetoothSettings_launchesIntent() {
        val launched = displayManager.openBluetoothSettings()
        assertThat(launched).isTrue()

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isAnyOf(
            Settings.ACTION_BLUETOOTH_SETTINGS,
            Settings.ACTION_SETTINGS
        )
    }

    @Test
    fun openSoundSettings_launchesIntent() {
        val launched = displayManager.openSoundSettings()
        assertThat(launched).isTrue()

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isAnyOf(
            Settings.ACTION_SOUND_SETTINGS,
            Settings.ACTION_SETTINGS
        )
    }

    @Test
    fun getConnectedWifiSsid_returnsStatusString() {
        val status = displayManager.getConnectedWifiSsid()
        assertThat(status).isNotEmpty()
        assertThat(status).isAnyOf("Wi-Fi", "Ethernet", "Non connecté")
    }

    @Test
    fun helperMethods_returnConsistentValues() {
        assertThat(displayManager.is4KCapable()).isEqualTo(displayManager.getDisplayInfo().is4KCapable)
        assertThat(displayManager.getPhysicalTvName()).isEqualTo(displayManager.getDisplayInfo().physicalTvName)
        assertThat(displayManager.getDisplayBadgeText()).isEqualTo(displayManager.getDisplayInfo().displayBadgeText)
        assertThat(displayManager.getDisplaySubtitleText()).isEqualTo(displayManager.getDisplayInfo().displaySubtitleText)
    }

    @Test
    fun sendDeviceToSleep_startsSleepIntent() {
        val result = displayManager.sendDeviceToSleep(context)
        assertThat(result).isTrue()

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isEqualTo("com.google.android.pano.action.SLEEP")
        assertThat(startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }

    @Test
    fun sendDeviceToSleep_companion_startsSleepIntent() {
        val result = DisplayResolutionManager.sendDeviceToSleep(context)
        assertThat(result).isTrue()

        val shadowApp = shadowOf(context as Application)
        val startedIntent = shadowApp.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isEqualTo("com.google.android.pano.action.SLEEP")
        assertThat(startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }

    @Test
    fun wifiConnectionDetails_formattedStatus_formatsCorrectly() {
        val detailsSsid = DisplayResolutionManager.WifiConnectionDetails("maison", 433)
        assertThat(detailsSsid.formattedStatus).isEqualTo("Connecté • maison")

        val detailsWifi = DisplayResolutionManager.WifiConnectionDetails("Wi-Fi", 433)
        assertThat(detailsWifi.formattedStatus).isEqualTo("Connecté")

        val detailsEthernet = DisplayResolutionManager.WifiConnectionDetails("Ethernet", null)
        assertThat(detailsEthernet.formattedStatus).isEqualTo("Connecté • Ethernet")

        val detailsOffline = DisplayResolutionManager.WifiConnectionDetails("Non connecté", null)
        assertThat(detailsOffline.formattedStatus).isEqualTo("Non connecté")
    }

    @Test
    fun sendTvCecStandby_executesDefensively() {
        // Must execute cleanly without uncaught exceptions
        val result = displayManager.sendTvCecStandby(context)
        val companionResult = DisplayResolutionManager.sendTvCecStandby(context)
        assertThat(result).isNotNull()
        assertThat(companionResult).isNotNull()
    }
}
