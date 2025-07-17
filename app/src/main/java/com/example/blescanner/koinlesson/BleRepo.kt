package com.example.blescanner.koinlesson

class BleRepo(private val deviceManager: DeviceManager, private val logger: AppLogger):IBleRepo {

    override fun simulateScan() {
        deviceManager.addDevice("Device_1")
        deviceManager.addDevice("Device_2")
    }

    override fun getFoundDevices(): List<String> = deviceManager.getDevices()

    fun getUserName(): String = "TestUser"
}

interface IBleRepo {
    fun simulateScan()
    fun getFoundDevices(): List<String>
}