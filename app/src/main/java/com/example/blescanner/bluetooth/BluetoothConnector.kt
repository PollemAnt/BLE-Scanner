package com.example.blescanner.bluetooth

import com.example.blescanner.data.models.BluetoothConnectableDevice

class BluetoothConnector {

    private var currentDevice: BluetoothConnectableDevice? = null

    fun connectTo(device: BluetoothConnectableDevice) {
        currentDevice?.disconnect()
        currentDevice = device
        device.connect()
    }

    fun disconnect() {
        currentDevice?.disconnect()
        currentDevice = null
    }

    fun getConnectedDevice(): BluetoothConnectableDevice? = currentDevice

    fun isConnected(): Boolean = currentDevice?.isInitialized() == true

    fun controlDiode() {
        currentDevice?.setValueToControlDiode()
    }
}
