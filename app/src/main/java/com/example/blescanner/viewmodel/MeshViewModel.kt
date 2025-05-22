package com.example.blescanner.viewmodel

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.core.BlinkyApplication.Companion.appContext
import com.example.blescanner.data.models.BluetoothConnectableDevice

class MeshViewModel : ViewModel() {

    private val _isFragmentReadyToShow = MutableLiveData<Boolean>()
    val isFragmentReadyToShow: LiveData<Boolean> get() = _isFragmentReadyToShow

    private val _deviceName = MutableLiveData<String>()
    val deviceName: LiveData<String> = _deviceName

    private val _services = MutableLiveData<List<BluetoothGattService>>()
    val services: LiveData<List<BluetoothGattService>> = _services

    private val _characteristics = MutableLiveData<List<BluetoothGattCharacteristic>>()
    val characteristics: LiveData<List<BluetoothGattCharacteristic>> = _characteristics

    private val _areListsShown = MutableLiveData<Boolean>()
    val areListsShown: LiveData<Boolean> = _areListsShown

    //private val _connectedDevice = MutableLiveData<BluetoothConnectableDevice>()
    //val connectedDevice: LiveData<BluetoothConnectableDevice> = _connectedDevice

    private val prefs = appContext.getSharedPreferences("Favorites mesh devices", 0)

    init {

        BluetoothService.connectedDevice?.isFragmentReadyToShow?.observeForever {
            _isFragmentReadyToShow.value = it
        }

        _deviceName.value = BluetoothService.connectedDevice?.name
        updateLists()
    }

    fun isDeviceConnected() = BluetoothService.connectedDevice == null

    fun prepareProvision() {
        BluetoothMeshNetwork.prepareProvision()
    }

    fun callDisconnect() {
        BluetoothService.callDisconnect()
    }

    fun updateLists() {
        _services.value = BluetoothService.listOfServices
        _characteristics.value = BluetoothService.listOfCharacteristic
    }

    fun toggleListsVisibility() {
        _areListsShown.value = !(_areListsShown.value ?: false)
    }

    fun onBackPressed() {
        BluetoothMeshNetwork.onBackPressed()
    }

    fun isFavorite() =
        prefs!!.all.contains(BluetoothService.connectedDevice!!.address)

    fun addToFavorite(deviceAddress: String, deviceName : String?) {
        with(prefs!!.edit()) {
            putString(deviceAddress, deviceName + "no empty")
            commit()
        }
    }

    fun deleteFromFavorite() {
        with(prefs!!.edit()) {
            remove(BluetoothService.connectedDevice!!.address)
            commit()
        }
    }

    override fun onCleared() {
        super.onCleared()
        BluetoothService.connectedDevice?.isFragmentReadyToShow?.removeObserver { }
    }
}