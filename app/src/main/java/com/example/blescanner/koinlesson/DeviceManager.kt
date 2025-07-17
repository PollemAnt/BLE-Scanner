package com.example.blescanner.koinlesson

class DeviceManager(private val logger: AppLogger) {
    private val devices = mutableListOf<String>()

    fun addDevice(name: String) {
        devices.add(name)
        logger.log("Dodano urządzenie: $name")
    }

    fun getDevices(): List<String> = devices
}