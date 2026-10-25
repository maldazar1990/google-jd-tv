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
class WidgetManagerTest {

    private lateinit var context: Context
    private lateinit var widgetManager: WidgetManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        widgetManager = WidgetManager(context)
    }

    @Test
    fun getMemoryStatus_returnsValidMetrics() {
        val status = widgetManager.getMemoryStatus()
        assertThat(status.totalMemMb).isAtLeast(0L)
        assertThat(status.availMemMb).isAtLeast(0L)
        assertThat(status.percentUsed).isIn(0..100)
    }

    @Test
    fun formatRamFreeString_returnsNonEmptyString() {
        val ramString = widgetManager.formatRamFreeString()
        assertThat(ramString).startsWith("RAM:")
        assertThat(ramString).contains("libre")
    }

    @Test
    fun getNetworkStatus_returnsNonNullNetworkType() {
        val netType = widgetManager.getNetworkStatus()
        assertThat(netType).isNotNull()
    }
}
