package com.example.blescanner.viewmodel

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.core.BlinkyApplication.Companion.appContext

class MeshViewModel : BaseDeviceViewModel() {

    private val _deviceName = MutableLiveData<String>()
    val deviceName: LiveData<String> = _deviceName

    private val _services = MutableLiveData<List<BluetoothGattService>>()
    val services: LiveData<List<BluetoothGattService>> = _services

    private val _characteristics = MutableLiveData<List<BluetoothGattCharacteristic>>()
    val characteristics: LiveData<List<BluetoothGattCharacteristic>> = _characteristics

    override val prefs: SharedPreferences =
        appContext.getSharedPreferences("Favorites mesh devices", Context.MODE_PRIVATE)

    init {
        _deviceName.value = BluetoothService.getConnectedDevice()?.name
        updateLists()
    }

    fun isDeviceConnected(): Boolean = BluetoothService.getConnectedDevice() == null

    fun prepareProvision() {
        BluetoothMeshNetwork.prepareProvision()
    }

    override fun onBackPressed() {
        BluetoothMeshNetwork.onBackPressed()
    }

    fun updateLists() {
        _services.value = BluetoothService.listOfServices
        _characteristics.value = BluetoothService.listOfCharacteristics
    }
}
