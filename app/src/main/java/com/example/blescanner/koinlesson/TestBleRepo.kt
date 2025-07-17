package com.example.blescanner.koinlesson

class TestBleRepo : IBleRepo {
    private val testDevices = listOf("TestDevice_A", "TestDevice_B")

    override fun simulateScan() {}

    override fun getFoundDevices(): List<String> {
        return testDevices
    }
}