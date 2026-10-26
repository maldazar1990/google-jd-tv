package com.minimal.tvlauncher.manager

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TopShelfManagerTest {

    private lateinit var context: Context
    private lateinit var topShelfManager: TopShelfManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        topShelfManager = TopShelfManager(context)
    }

    @Test
    fun getTopShelfItemForPackage_withBlankPackage_returnsNull() {
        assertThat(topShelfManager.getTopShelfItemForPackage("")).isNull()
        assertThat(topShelfManager.getTopShelfItemForPackage("   ")).isNull()
    }

    @Test
    fun getTopShelfItemForPackage_whenNoProviderAvailable_safelyReturnsNull() {
        val result = topShelfManager.getTopShelfItemForPackage("com.unknown.nonexistent.app")
        assertThat(result).isNull()
    }

    @Test
    fun getTopShelfItemForPackage_whenStreamingApp_returnsCuratedFallback() {
        val result = topShelfManager.getTopShelfItemForPackage("com.netflix.ninja")
        assertThat(result).isNotNull()
        assertThat(result?.title).contains("Netflix")
    }

    @Test
    fun posterCache_storesAndRetrievesBitmap() {
        val testUri = Uri.parse("content://android.media.tv/poster/123")
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565)

        assertThat(topShelfManager.getCachedPosterBitmap(testUri)).isNull()

        topShelfManager.cachePosterBitmap(testUri, bitmap)
        val cached = topShelfManager.getCachedPosterBitmap(testUri)
        assertThat(cached).isNotNull()
        assertThat(cached).isSameInstanceAs(bitmap)

        topShelfManager.clearPosterCache()
        assertThat(topShelfManager.getCachedPosterBitmap(testUri)).isNull()
    }

    @Test
    fun loadPosterBitmap_withNullUri_returnsNull() {
        val result = topShelfManager.loadPosterBitmap(null)
        assertThat(result).isNull()
    }

    @Test
    fun topShelfItem_modelProperties() {
        val uri = Uri.parse("content://android.media.tv/poster/456")
        val intentUri = Uri.parse("intent://example.com")
        val item = TopShelfManager.TopShelfItem(
            title = "Stranger Things",
            subtitle = "S4:E1 The Hellfire Club",
            posterUri = uri,
            intentUri = intentUri
        )

        assertThat(item.title).isEqualTo("Stranger Things")
        assertThat(item.subtitle).isEqualTo("S4:E1 The Hellfire Club")
        assertThat(item.posterUri).isEqualTo(uri)
        assertThat(item.posterArtUri).isEqualTo(uri)
        assertThat(item.intentUri).isEqualTo(intentUri)
    }
}
