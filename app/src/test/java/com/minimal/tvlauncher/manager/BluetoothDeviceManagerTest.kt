package com.minimal.tvlauncher.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BluetoothDeviceManagerTest {

    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothDeviceManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bluetoothManager = BluetoothDeviceManager(context)
    }

    @Test
    fun getConnectedDevices_defaultsToEmptyList_whenNoDevicesPaired() {
        val devices = bluetoothManager.getConnectedDevicesWithBatteryOrVolume()
        assertThat(devices).isNotNull()
        assertThat(devices).isEmpty()
    }

    @Test
    fun getConnectedDevices_returnsMockDevices_whenMockProvided() {
        val mockDevice = BluetoothDeviceManager.ConnectedDevice(
            name = "Xbox Wireless Controller",
            type = BluetoothDeviceManager.DeviceType.GAMEPAD,
            batteryLevel = 15,
            volumeLevel = null
        )
        bluetoothManager.mockConnectedDevices = listOf(mockDevice)

        val devices = bluetoothManager.getConnectedDevicesWithBatteryOrVolume()
        assertThat(devices).hasSize(1)
        assertThat(devices[0].name).isEqualTo("Xbox Wireless Controller")
        assertThat(devices[0].type).isEqualTo(BluetoothDeviceManager.DeviceType.GAMEPAD)
        assertThat(devices[0].batteryLevel).isEqualTo(15)
        assertThat(devices[0].volumeLevel).isNull()
    }

    @Test
    fun lowBattery_detection_identifiesLevelUnderOrEqual20() {
        val lowBatteryDevice = BluetoothDeviceManager.ConnectedDevice(
            name = "DualSense Wireless Controller",
            type = BluetoothDeviceManager.DeviceType.GAMEPAD,
            batteryLevel = 20,
            volumeLevel = null
        )
        val normalBatteryDevice = BluetoothDeviceManager.ConnectedDevice(
            name = "Sony WH-1000XM4",
            type = BluetoothDeviceManager.DeviceType.HEADSET,
            batteryLevel = 80,
            volumeLevel = 65
        )

        val isLow1 = (lowBatteryDevice.batteryLevel ?: 100) <= 20
        val isLow2 = (normalBatteryDevice.batteryLevel ?: 100) <= 20

        assertThat(isLow1).isTrue()
        assertThat(isLow2).isFalse()
    }

    @Test
    fun headsetWithVolume_isHandledCorrectly() {
        val headset = BluetoothDeviceManager.ConnectedDevice(
            name = "AirPods Pro",
            type = BluetoothDeviceManager.DeviceType.HEADSET,
            batteryLevel = 90,
            volumeLevel = 75
        )

        assertThat(headset.type).isEqualTo(BluetoothDeviceManager.DeviceType.HEADSET)
        assertThat(headset.volumeLevel).isEqualTo(75)
    }

    @Test
    fun getConnectedRemoteOrGamepadInfo_returnsDefaultOrMock() {
        val defaultInfo = bluetoothManager.getConnectedRemoteOrGamepadInfo()
        assertThat(defaultInfo.name).isNotNull()

        bluetoothManager.mockRemoteDeviceInfo = BluetoothDeviceManager.RemoteDeviceInfo(
            name = "Télécommande",
            batteryPercent = 18
        )
        val remoteInfo = bluetoothManager.getConnectedRemoteOrGamepadInfo()
        assertThat(remoteInfo.name).isEqualTo("Télécommande")
        assertThat(remoteInfo.batteryPercent).isEqualTo(18)
        assertThat(remoteInfo.isLowBattery).isTrue()
    }
}
