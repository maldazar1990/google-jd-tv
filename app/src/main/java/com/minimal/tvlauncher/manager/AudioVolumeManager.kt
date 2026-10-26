package com.minimal.tvlauncher.manager

import android.content.Context
import android.media.AudioManager

/**
 * Dedicated manager for TV audio stream music volume reading and adjustments.
 */
class AudioVolumeManager(private val context: Context) {

    private val audioManager: AudioManager? = try {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    } catch (_: Exception) {
        null
    }

    /**
     * Returns current music volume percentage (0..100).
     */
    fun getVolumePercent(): Int {
        val am = audioManager ?: return 0
        return try {
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) {
                ((current.toFloat() / max.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Increases music volume by 1 step with standard TV UI overlay and returns new percent.
     */
    fun increaseVolume(): Int {
        try {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (_: Exception) {}
        return getVolumePercent()
    }

    /**
     * Decreases music volume by 1 step with standard TV UI overlay and returns new percent.
     */
    fun decreaseVolume(): Int {
        try {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (_: Exception) {}
        return getVolumePercent()
    }
}
