package com.example.blescanner.koinlesson

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DeviceKoinViewModel(private val deviceId: String, private val repository: BleRepo) : ViewModel() {

    private val _devices = MutableLiveData<List<String>>()
    val devices: LiveData<List<String>> = _devices

    fun startScan() {
        repository.simulateScan()
        _devices.value = repository.getFoundDevices()
    }

    fun getUserName(): String = repository.getUserName()
}