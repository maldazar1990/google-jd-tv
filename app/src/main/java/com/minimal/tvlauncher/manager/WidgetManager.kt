package com.minimal.tvlauncher.manager

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Debug

/**
 * Lightweight, zero-drain status monitor for RAM and Network connectivity.
 * Engineered specifically for low-memory Android TV (Chromecast 4K / 2 GB RAM).
 */
class WidgetManager(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    data class MemoryStatus(
        val totalMemMb: Long,
        val availMemMb: Long,
        val usedMemMb: Long,
        val launcherUsedMb: Long,
        val percentUsed: Int
    )

    enum class NetworkType {
        NONE,
        WIFI,
        ETHERNET
    }

    /**
     * Reads system RAM metrics in real-time.
     * Takes less than 1 ms to execute with zero background polling.
     */
    fun getMemoryStatus(): MemoryStatus {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        val availMb = memoryInfo.availMem / (1024 * 1024)
        val usedMb = totalMb - availMb
        val percent = if (totalMb > 0) ((usedMb.toDouble() / totalMb) * 100).toInt() else 0

        // Measure launcher app's own memory footprint (must stay < 30 MB)
        val pssKb = Debug.getPss()
        val launcherMb = pssKb / 1024

        return MemoryStatus(
            totalMemMb = totalMb,
            availMemMb = availMb,
            usedMemMb = usedMb,
            launcherUsedMb = launcherMb,
            percentUsed = percent
        )
    }

    /**
     * Formats RAM status string matching tvOS capsule widget: e.g. "RAM: 1.2G libre"
     */
    fun formatRamFreeString(): String {
        val status = getMemoryStatus()
        return if (status.availMemMb >= 1024) {
            val gb = status.availMemMb / 1024.0
            String.format(java.util.Locale.US, "RAM: %.1fG libre", gb)
        } else {
            "RAM: ${status.availMemMb}M libre"
        }
    }

    /**
     * Inspects active network transport (Ethernet takes priority on TV, then Wi-Fi).
     */
    fun getNetworkStatus(): NetworkType {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                else -> NetworkType.NONE
            }
        } catch (_: Exception) {
            NetworkType.NONE
        }
    }
}
