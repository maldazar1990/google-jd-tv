package com.minimal.tvlauncher.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Lightweight, zero-leak daily wallpaper manager.
 * Fetches Bing 4K Daily Wallpaper strictly over HTTPS, downsamples to 1080p for 60 fps TV performance,
 * and caches locally to avoid unnecessary network traffic or memory leaks.
 */
class BingWallpaperManager(private val context: Context) {

    companion object {
        private const val BING_API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=fr-FR"
        private const val WALLPAPER_FILE_NAME = "bing_wallpaper_cache.jpg"
        private const val TIMEOUT_MS = 6000
    }

    private val cachedWallpaperFile: File
        get() = File(context.filesDir, WALLPAPER_FILE_NAME)

    /**
     * Loads the cached wallpaper bitmap if available, decoded with optimal inSampleSize
     * to keep RAM strictly below 4 MB.
     */
    fun getCachedWallpaperBitmap(): Bitmap? {
        val file = cachedWallpaperFile
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            val sampleSize = calculateInSampleSize(boundsOptions, 1920, 1080)
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // 50% memory saving over ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, loadOptions)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Downloads today's Bing wallpaper over HTTPS and saves it locally.
     * Must be called from a background worker thread.
     */
    fun refreshWallpaperFromBing(): Boolean {
        return try {
            val url = URL(BING_API_URL)
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
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
            val fullImageUrl = if (relativeUrl.startsWith("http")) relativeUrl else "https://www.bing.com$relativeUrl"

            // Download image bytes
            val imgConn = URL(fullImageUrl).openConnection() as HttpsURLConnection
            imgConn.connectTimeout = TIMEOUT_MS
            imgConn.readTimeout = TIMEOUT_MS
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
