package com.example.blescanner.bluetooth

import com.example.blescanner.data.models.BluetoothConnectableDevice
import com.example.blescanner.utils.Constants

class BluetoothDeviceManager {

    private var selectedDevice: BluetoothConnectableDevice? = null

    fun selectDevice(device: BluetoothConnectableDevice): String {
        selectedDevice = device
        return getDeviceType(device)
    }

    fun getSelectedDevice(): BluetoothConnectableDevice? = selectedDevice

    fun clearSelection() {
        selectedDevice = null
    }

    private fun getDeviceType(device: BluetoothConnectableDevice): String {
        return when {
            device.name?.contains("Blinky", ignoreCase = true) == true -> "Blinky"
            device.scanResult.scanRecord?.serviceUuids?.firstOrNull() == Constants.MESH_SERVICE_PARCEL_UUID ||
                    device.scanResult.scanRecord?.serviceUuids?.firstOrNull() == Constants.MESH_SILICON_LABS_PARCEL_UUID -> "Mesh"
            else -> "Unknown"
        }
    }
}
