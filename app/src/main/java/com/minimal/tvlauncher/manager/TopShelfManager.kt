package com.minimal.tvlauncher.manager

import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * Lightweight, zero-leak Apple TV style Top Shelf manager.
 * Queries Android TV TvContract (content://android.media.tv/watch_next_program and preview_program)
 * to retrieve "Watch Next" and promoted episodes/content for the hovered Dock app.
 */
class TopShelfManager(private val context: Context) {

    data class TopShelfItem(
        val title: String,
        val subtitle: String?,
        val posterUri: Uri?,
        val intentUri: Uri?
    )

    companion object {
        val WATCH_NEXT_URI: Uri = Uri.parse("content://android.media.tv/watch_next_program")
        val PREVIEW_PROGRAMS_URI: Uri = Uri.parse("content://android.media.tv/preview_program")

        private const val COLUMN_PACKAGE_NAME = "package_name"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_EPISODE_TITLE = "episode_title"
        private const val COLUMN_POSTER_ART_URI = "poster_art_uri"
        private const val COLUMN_INTENT_URI = "intent_uri"
    }

    /**
     * Queries Watch Next / Preview program for the specified app package.
     * Safely catches any SecurityException or IllegalArgumentException if provider is missing.
     */
    fun getTopShelfItemForPackage(packageName: String): TopShelfItem? {
        if (packageName.isBlank()) return null

        val watchNext = queryProgramUri(WATCH_NEXT_URI, packageName)
        if (watchNext != null) return watchNext

        return queryProgramUri(PREVIEW_PROGRAMS_URI, packageName)
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
