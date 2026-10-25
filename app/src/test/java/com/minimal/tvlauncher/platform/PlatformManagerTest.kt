package com.minimal.tvlauncher.platform

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlatformManagerTest {

    @Test
    fun getActivePlatform_googleTvOverride_returnsGoogleTvProvider() {
        val manager = PlatformManager(PlatformManager.PLATFORM_GOOGLE_TV)
        val provider = manager.getActivePlatform()
        assertThat(provider.platformType).isEqualTo(TvPlatformType.GOOGLE_TV)
        assertThat(provider.defaultLauncherPackage).isEqualTo("com.google.android.apps.tv.launcherx")
    }

    @Test
    fun getActivePlatform_fireTvOverride_returnsFireTvProvider() {
        val manager = PlatformManager(PlatformManager.PLATFORM_FIRE_TV)
        val provider = manager.getActivePlatform()
        assertThat(provider.platformType).isEqualTo(TvPlatformType.FIRE_TV)
        assertThat(provider.defaultLauncherPackage).isEqualTo("com.amazon.tv.launcher")
        assertThat(provider.appStorePackage).isEqualTo("com.amazon.venezia")
    }

    @Test
    fun getActivePlatform_autoDefault_returnsValidProvider() {
        val manager = PlatformManager(PlatformManager.PLATFORM_AUTO)
        val provider = manager.getActivePlatform()
        assertThat(provider).isNotNull()
    }
}
