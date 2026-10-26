package com.minimal.tvlauncher.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioManager
import android.os.Build

class BluetoothDeviceManager(private val context: Context) {

    enum class DeviceType {
        HEADSET,
        GAMEPAD,
        OTHER
    }

    data class ConnectedDevice(
        val name: String,
        val type: DeviceType,
        val batteryLevel: Int?, // 0-100 or null
        val volumeLevel: Int? = null // 0-100 or null
    )

    private val bluetoothManager: BluetoothManager? by lazy {
        try {
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        } catch (_: Exception) {
            null
        }
    }

    private val audioManager: AudioManager? by lazy {
        try {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (_: Exception) {
            null
        }
    }

    // Optional override for unit testing / mock devices
    internal var mockConnectedDevices: List<ConnectedDevice>? = null

    /**
     * Retrieves connected Bluetooth devices that have battery info or are audio headsets with volume.
     * Guaranteed zero leak, zero crash on missing permissions or unsupported hardware.
     */
    fun getConnectedDevicesWithBatteryOrVolume(): List<ConnectedDevice> {
        mockConnectedDevices?.let { return it }

        val result = mutableListOf<ConnectedDevice>()
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()

        if (!adapter.isEnabled) return emptyList()

        try {
            val bondedDevices: Set<BluetoothDevice> = try {
                adapter.bondedDevices ?: emptySet()
            } catch (_: SecurityException) {
                emptySet()
            }

            for (device in bondedDevices) {
                val isConnected = isDeviceConnected(adapter, device)
                if (!isConnected) continue

                val type = resolveDeviceType(device)
                val battery = queryBatteryLevel(device)
                val volume = if (type == DeviceType.HEADSET) getCurrentAudioVolumePercent() else null

                // Only include if device has known battery or is a headset with volume
                if (battery != null || volume != null) {
                    val name = try { device.name ?: "Périphérique Bluetooth" } catch (_: SecurityException) { "Périphérique Bluetooth" }
                    result.add(
                        ConnectedDevice(
                            name = name,
                            type = type,
                            batteryLevel = battery,
                            volumeLevel = volume
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Defensive catch-all
        }

        return result
    }

    private fun isDeviceConnected(adapter: BluetoothAdapter, device: BluetoothDevice): Boolean {
        return try {
            val isConnectedMethod = device.javaClass.getMethod("isConnected")
            isConnectedMethod.invoke(device) as? Boolean ?: false
        } catch (_: Exception) {
            try {
                adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED ||
                adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
            } catch (_: Exception) {
                false
            }
        }
    }

    fun resolveDeviceType(device: BluetoothDevice): DeviceType {
        val devName = try { device.name?.lowercase() ?: "" } catch (_: SecurityException) { "" }
        val devClass = try { device.bluetoothClass } catch (_: SecurityException) { null }

        if (devClass != null) {
            val major = devClass.majorDeviceClass
            val deviceClass = devClass.deviceClass
            if (major == BluetoothClass.Device.Major.PERIPHERAL ||
                devName.contains("controller") || devName.contains("gamepad") ||
                devName.contains("xbox") || devName.contains("dualshock") ||
                devName.contains("dualsense") || devName.contains("shield")
            ) {
                return DeviceType.GAMEPAD
            }
            if (major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                devName.contains("headset") || devName.contains("buds") ||
                devName.contains("earphones") || devName.contains("airpods") ||
                devName.contains("wh-1000") || devName.contains("casque")
            ) {
                return DeviceType.HEADSET
            }
        } else {
            if (devName.contains("controller") || devName.contains("gamepad") ||
                devName.contains("xbox") || devName.contains("dualshock") ||
                devName.contains("dualsense") || devName.contains("shield")
            ) {
                return DeviceType.GAMEPAD
            }
            if (devName.contains("headset") || devName.contains("buds") ||
                devName.contains("earphones") || devName.contains("airpods") ||
                devName.contains("wh-1000") || devName.contains("casque")
            ) {
                return DeviceType.HEADSET
            }
        }

        return DeviceType.OTHER
    }

    fun queryBatteryLevel(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int
            if (level != null && level in 0..100) level else null
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentAudioVolumePercent(): Int? {
        return try {
            val am = audioManager ?: return null
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) {
                ((current.toFloat() / max.toFloat()) * 100).toInt()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
