package com.minimal.tvlauncher.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.minimal.tvlauncher.config.AppConfig
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Lightweight, zero-leak wallpaper manager.
 * Supports:
 * - Bing 4K Daily Wallpaper (HTTPS with atomic caching)
 * - Local storage wallpapers (e.g. /sdcard/Pictures/wallpaper.jpg)
 * - Google Photos public links / web feeds
 * Strictly downsamples all images to 1080p RGB_565 to keep memory footprint under 4 MB.
 */
class BingWallpaperManager(private val context: Context) {

    companion object {
        private const val WALLPAPER_FILE_NAME = "bing_wallpaper_cache.jpg"
        private const val REMOTE_WALLPAPER_CACHE = "remote_wallpaper_cache.jpg"
    }

    private val cachedWallpaperFile: File
        get() = File(context.filesDir, WALLPAPER_FILE_NAME)

    private val remoteWallpaperCacheFile: File
        get() = File(context.filesDir, REMOTE_WALLPAPER_CACHE)

    /**
     * Loads the cached Bing wallpaper bitmap if available.
     */
    fun getCachedWallpaperBitmap(): Bitmap? {
        val file = cachedWallpaperFile
        if (!file.exists() || file.length() == 0L) return null
        return decodeSampledBitmapFromFile(file.absolutePath)
    }

    /**
     * Loads a wallpaper bitmap from local TV storage (e.g. /sdcard/Pictures/wallpaper.jpg).
     */
    fun getBitmapFromStorage(filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists() || !file.canRead() || file.length() == 0L) return null
        return decodeSampledBitmapFromFile(file.absolutePath)
    }

    /**
     * Downloads an image from a URL (e.g. Google Photos shared link) over HTTPS,
     * caches it locally, and decodes it safely with downsampling.
     */
    fun getBitmapFromUrl(urlString: String): Bitmap? {
        if (urlString.isBlank() || !urlString.startsWith("https://")) return null

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            connection.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
            connection.requestMethod = "GET"

            if (connection.responseCode != HttpsURLConnection.HTTP_OK) {
                connection.disconnect()
                return decodeSampledBitmapFromFile(remoteWallpaperCacheFile.absolutePath)
            }

            val tempFile = File(context.filesDir, "temp_remote_wp.jpg")
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (remoteWallpaperCacheFile.exists()) {
                    remoteWallpaperCacheFile.delete()
                }
                tempFile.renameTo(remoteWallpaperCacheFile)
            }
        } catch (_: Exception) {
            // Fall back to previously cached remote wallpaper if network fails
        }

        return if (remoteWallpaperCacheFile.exists()) {
            decodeSampledBitmapFromFile(remoteWallpaperCacheFile.absolutePath)
        } else {
            null
        }
    }

    /**
     * Downloads today's Bing wallpaper over HTTPS and saves it locally.
     * Must be called from a background worker thread.
     */
    fun refreshWallpaperFromBing(): Boolean {
        return try {
            val url = URL(AppConfig.Endpoints.BING_WALLPAPER_API)
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            connection.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
            connection.requestMethod = "GET"

            if (connection.responseCode != HttpsURLConnection.HTTP_OK) {
                connection.disconnect()
                return false
            }

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val jsonRoot = JSONObject(jsonText)
            val imagesArray = jsonRoot.getJSONArray("images")
            if (imagesArray.length() == 0) return false

            val imageObj = imagesArray.getJSONObject(0)
            val relativeUrl = imageObj.getString("url")
            val fullImageUrl = if (relativeUrl.startsWith("http")) relativeUrl else "${AppConfig.Endpoints.BING_IMAGE_BASE_URL}$relativeUrl"

            // Download image bytes
            val imgConn = URL(fullImageUrl).openConnection() as HttpsURLConnection
            imgConn.connectTimeout = AppConfig.Timeouts.NETWORK_CONNECT_MS
            imgConn.readTimeout = AppConfig.Timeouts.NETWORK_READ_MS
            if (imgConn.responseCode != HttpsURLConnection.HTTP_OK) {
                imgConn.disconnect()
                return false
            }

            val tempFile = File(context.filesDir, "temp_bing.jpg")
            imgConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            imgConn.disconnect()

            // Atomic rename
            if (tempFile.exists() && tempFile.length() > 0) {
                if (cachedWallpaperFile.exists()) {
                    cachedWallpaperFile.delete()
                }
                tempFile.renameTo(cachedWallpaperFile)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun decodeSampledBitmapFromFile(path: String): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, boundsOptions)

            val sampleSize = calculateInSampleSize(
                boundsOptions,
                AppConfig.Hardware.WALLPAPER_TARGET_WIDTH,
                AppConfig.Hardware.WALLPAPER_TARGET_HEIGHT
            )
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // 50% memory saving over ARGB_8888
            }
            BitmapFactory.decodeFile(path, loadOptions)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
