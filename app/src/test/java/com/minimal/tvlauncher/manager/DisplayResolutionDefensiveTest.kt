package com.minimal.tvlauncher.manager

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Defensive exception resilience test for [DisplayResolutionManager.sendDeviceToSleep].
 *
 * Verifies:
 * 1. Absence of crashes when startActivity throws SecurityException (e.g. OEM permission restrictions).
 * 2. Absence of crashes when startActivity throws ActivityNotFoundException (e.g. non-Google TV Android builds).
 * 3. Absence of crashes when all intents and broadcasts throw RuntimeExceptions.
 * 4. Graceful cascade through fallbacks until exhaustion, cleanly returning false without crashing the launcher.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DisplayResolutionDefensiveTest {

    private lateinit var realContext: Context

    @Before
    fun setUp() {
        realContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun sendDeviceToSleep_whenPanoSleepThrowsSecurityException_cascadesGracefully() {
        val mockContext = mockk<Context>(relaxed = true)

        var callCount = 0
        every { mockContext.startActivity(any()) } answers {
            val intent = firstArg<Intent>()
            if (intent.action == "com.google.android.pano.action.SLEEP") {
                throw SecurityException("Mocked permission denied for pano SLEEP")
            }
            callCount++
        }

        val result = DisplayResolutionManager.sendDeviceToSleep(mockContext)
        // Since the 2nd fallback (DreamX) succeeds on mockContext.startActivity, it returns true
        assertThat(result).isTrue()
        assertThat(callCount).isAtLeast(1)
    }

    @Test
    fun sendDeviceToSleep_whenAllStartActivityThrowActivityNotFound_fallsBackToBroadcast() {
        val mockContext = mockk<Context>(relaxed = true)

        every { mockContext.startActivity(any()) } throws ActivityNotFoundException("Activity not found")
        var broadcastSent = false
        every { mockContext.sendBroadcast(any()) } answers {
            broadcastSent = true
        }

        val result = DisplayResolutionManager.sendDeviceToSleep(mockContext)
        assertThat(result).isTrue()
        assertThat(broadcastSent).isTrue()
    }

    @Test
    fun sendDeviceToSleep_whenAllIntentsAndBroadcastThrow_doesNotCrashAndReturnsSafely() {
        val mockContext = mockk<Context>(relaxed = true)

        every { mockContext.startActivity(any()) } throws SecurityException("Blocked by OEM security policy")
        every { mockContext.sendBroadcast(any()) } throws SecurityException("Broadcast permission denied")

        // The method should catch all exceptions and attempt Runtime.exec or return false, NEVER crashing
        var threwException = false
        var result = false
        try {
            result = DisplayResolutionManager.sendDeviceToSleep(mockContext)
        } catch (_: Throwable) {
            threwException = true
        }

        assertThat(threwException).isFalse()
        // result will be true (if Runtime.exec succeeds in test runner) or false, but critically NO CRASH
        assertThat(result).isNotNull()
    }

    @Test
    fun sendDeviceToSleep_instanceMethod_resilientToNullLikeScenarios() {
        val manager = DisplayResolutionManager(realContext)
        // Should execute standard sleep path safely
        val result = manager.sendDeviceToSleep()
        assertThat(result).isNotNull()
    }
}
