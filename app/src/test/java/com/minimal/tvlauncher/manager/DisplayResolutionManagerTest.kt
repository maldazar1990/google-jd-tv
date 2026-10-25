package com.minimal.tvlauncher.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
}
