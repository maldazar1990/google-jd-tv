package com.minimal.tvlauncher.manager

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache

/**
 * Lightweight, zero-leak Apple TV style Top Shelf manager.
 * Queries Android TV TvContract (content://android.media.tv/watch_next_program and preview_program)
 * to retrieve "Watch Next" and promoted episodes/content for the hovered Dock app.
 * Provides memory-efficient RGB_565 downsampling and an LRU cache for poster art.
 */
class TopShelfManager(private val context: Context) {

    data class TopShelfItem(
        val title: String,
        val subtitle: String?,
        val posterUri: Uri?,
        val intentUri: Uri?
    ) {
        val posterArtUri: Uri? get() = posterUri
    }

    private val posterCache = object : LruCache<String, Bitmap>(MAX_POSTER_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    companion object {
        val WATCH_NEXT_URI: Uri = Uri.parse("content://android.media.tv/watch_next_program")
        val PREVIEW_PROGRAMS_URI: Uri = Uri.parse("content://android.media.tv/preview_program")

        private const val COLUMN_PACKAGE_NAME = "package_name"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_EPISODE_TITLE = "episode_title"
        private const val COLUMN_POSTER_ART_URI = "poster_art_uri"
        private const val COLUMN_INTENT_URI = "intent_uri"

        // Maximum 4 MB in-memory cache to strictly preserve the 30 MB launcher RAM limit
        const val MAX_POSTER_CACHE_BYTES = 4 * 1024 * 1024
    }

    /**
     * Retrieves cached poster bitmap if available.
     */
    fun getCachedPosterBitmap(uri: Uri?): Bitmap? {
        if (uri == null) return null
        return posterCache.get(uri.toString())
    }

    /**
     * Stores a bitmap in the poster art LRU cache.
     */
    fun cachePosterBitmap(uri: Uri, bitmap: Bitmap) {
        posterCache.put(uri.toString(), bitmap)
    }

    /**
     * Clears all cached poster art bitmaps.
     */
    fun clearPosterCache() {
        posterCache.evictAll()
    }

    /**
     * Memory-efficient downsampling and decoding of poster artwork from Uri.
     * Uses RGB_565 config for 50% memory reduction on Android TV.
     */
    fun loadPosterBitmap(uri: Uri?, reqWidth: Int = 1280, reqHeight: Int = 720): Bitmap? {
        if (uri == null) return null
        val key = uri.toString()
        posterCache.get(key)?.let { return it }

        return try {
            val openStream: () -> java.io.InputStream? = {
                val scheme = uri.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") {
                    val conn = java.net.URL(uri.toString()).openConnection()
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.getInputStream()
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            }

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = openStream()?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (bitmap != null) {
                posterCache.put(key, bitmap)
            }
            bitmap
        } catch (_: OutOfMemoryError) {
            posterCache.evictAll()
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Queries Watch Next / Preview program for the specified app package.
     * Safely catches any SecurityException or IllegalArgumentException if provider is missing.
     */
    fun getTopShelfItemForPackage(packageName: String): TopShelfItem? {
        if (packageName.isBlank()) return null

        val watchNext = queryProgramUri(WATCH_NEXT_URI, packageName)
        if (watchNext != null) return watchNext

        val preview = queryProgramUri(PREVIEW_PROGRAMS_URI, packageName)
        if (preview != null) return preview

        return getCuratedFallbackForPackage(packageName)
    }

    private fun getCuratedFallbackForPackage(packageName: String): TopShelfItem? {
        return when (packageName) {
            "com.netflix.ninja" -> TopShelfItem(
                title = "Netflix",
                subtitle = "Films et séries en streaming 4K HDR",
                posterUri = Uri.parse("https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=1280&q=80"),
                intentUri = null
            )
            "com.amazon.amazonvideo.livingroom" -> TopShelfItem(
                title = "Prime Video",
                subtitle = "Séries originales Amazon & Cinéma",
                posterUri = Uri.parse("https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1280&q=80"),
                intentUri = null
            )
            "ca.bellmedia.cravetv" -> TopShelfItem(
                title = "Crave",
                subtitle = "HBO, Max Originals et séries exclusives",
                posterUri = Uri.parse("https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1280&q=80"),
                intentUri = null
            )
            "com.google.android.youtube.tv" -> TopShelfItem(
                title = "YouTube",
                subtitle = "Tendances et vidéos recommandées",
                posterUri = Uri.parse("https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=1280&q=80"),
                intentUri = null
            )
            "com.spotify.tv.android" -> TopShelfItem(
                title = "Spotify",
                subtitle = "Musique et ambiances sonores en continu",
                posterUri = Uri.parse("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=1280&q=80"),
                intentUri = null
            )
            "tv.twitch.android.app" -> TopShelfItem(
                title = "Twitch",
                subtitle = "Jeux vidéo et diffusions en direct",
                posterUri = Uri.parse("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1280&q=80"),
                intentUri = null
            )
            "org.videolan.vlc" -> TopShelfItem(
                title = "VLC Media Player",
                subtitle = "Lecteur multimédia cinéma local et réseau",
                posterUri = Uri.parse("https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=1280&q=80"),
                intentUri = null
            )
            else -> null
        }
    }

    private fun queryProgramUri(uri: Uri, packageName: String): TopShelfItem? {
        return try {
            val projection = arrayOf(
                COLUMN_TITLE,
                COLUMN_EPISODE_TITLE,
                COLUMN_POSTER_ART_URI,
                COLUMN_INTENT_URI
            )
            val selection = "$COLUMN_PACKAGE_NAME = ?"
            val selectionArgs = arrayOf(packageName)

            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val titleIdx = it.getColumnIndex(COLUMN_TITLE)
                    val episodeIdx = it.getColumnIndex(COLUMN_EPISODE_TITLE)
                    val posterIdx = it.getColumnIndex(COLUMN_POSTER_ART_URI)
                    val intentIdx = it.getColumnIndex(COLUMN_INTENT_URI)

                    val title = if (titleIdx >= 0) it.getString(titleIdx) else null
                    val episodeTitle = if (episodeIdx >= 0) it.getString(episodeIdx) else null
                    val posterStr = if (posterIdx >= 0) it.getString(posterIdx) else null
                    val intentStr = if (intentIdx >= 0) it.getString(intentIdx) else null

                    if (!title.isNullOrBlank()) {
                        TopShelfItem(
                            title = title,
                            subtitle = episodeTitle,
                            posterUri = posterStr?.let { s -> try { Uri.parse(s) } catch (_: Exception) { null } },
                            intentUri = intentStr?.let { s -> try { Uri.parse(s) } catch (_: Exception) { null } }
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
