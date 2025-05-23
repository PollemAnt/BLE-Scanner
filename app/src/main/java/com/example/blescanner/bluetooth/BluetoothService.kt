package com.example.blescanner.bluetooth

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blescanner.core.BlinkyApplication
import com.example.blescanner.data.models.BluetoothConnectableDevice
import com.example.blescanner.utils.Constants

object BluetoothService {

    private val bluetoothManager =
        getSystemService(
            BlinkyApplication.appContext,
            BluetoothManager::class.java
        ) as BluetoothManager

    val bluetoothScanner: BluetoothLeScanner by lazy {
        bluetoothManager.adapter.bluetoothLeScanner
    }
    var listOfServices = mutableListOf<BluetoothGattService>()
    var listOfCharacteristic = mutableListOf<BluetoothGattCharacteristic>()

    var selectedDevice: BluetoothConnectableDevice? = null
        private set
    var connectedDevice: BluetoothConnectableDevice? = null
        private set

    private val _connectableDevices: MutableLiveData<List<BluetoothConnectableDevice>> =
        MutableLiveData(emptyList())
    val connectableDevices: LiveData<List<BluetoothConnectableDevice>> = _connectableDevices

    private val _typeOfSelectedDevice: MutableLiveData<String?> = MutableLiveData()
    val typeOfSelectedDevice: LiveData<String?> = _typeOfSelectedDevice

    private val _isScanning: MutableLiveData<Boolean> = MutableLiveData()
    val isScanning: LiveData<Boolean> = _isScanning

    private val scanSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()
    } else {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null && ContextCompat.checkSelfPermission(
                    BlinkyApplication.appContext,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val results = _connectableDevices.value!!.toMutableList()
                val indexQuery =
                    results.indexOfFirst { it.scanResult.device.address == result.device.address }
                if (indexQuery != -1) {
                    results[indexQuery].scanResult = result
                    _connectableDevices.value = results
                } else {
                    val newConnectableDevice = BluetoothConnectableDevice(result)
                    results.add(newConnectableDevice)
                    results.sortByDescending { it.scanResult.rssi }
                    _connectableDevices.value = results
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.v("error", "onScanFailed: code $errorCode")
        }
    }

    fun startScan() {
        if (ContextCompat.checkSelfPermission(
                BlinkyApplication.appContext,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothScanner.startScan(null, scanSettings, scanCallback)
            _isScanning.value = true
        }
    }

    fun stopScan() {
        if (ContextCompat.checkSelfPermission(
                BlinkyApplication.appContext,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothScanner.stopScan(scanCallback)
            _isScanning.value = false
        }
    }

    fun clearScanList() {
        _connectableDevices.value = emptyList()
    }

    fun setDeviceToConnect(bluetoothConnectableDevice: BluetoothConnectableDevice) {
        selectedDevice = bluetoothConnectableDevice
        _typeOfSelectedDevice.value = checkDeviceType()
        if (checkDeviceType() != "Unknown")
            connectToSelectedDevice()
    }

    private fun checkDeviceType(): String {
        val deviceType: String = if (selectedDevice!!.name?.contains("Blinky") == true) {
            "Blinky"
        } else if (selectedDevice!!.scanResult.scanRecord?.serviceUuids?.get(0) == Constants.MESH_SERVICE_PARCEL_UUID || selectedDevice!!.scanResult.scanRecord?.serviceUuids?.get(
                0
            ) == Constants.MESH_SILICON_LABS_PARCEL_UUID
        )
            "Mesh"
        else "Unknown"

        return deviceType
    }

    private fun connectToSelectedDevice() {
        connectedDevice?.disconnect()
        connectedDevice = selectedDevice
        selectedDevice!!.connect()
    }

    fun onBackPressed() {
        _typeOfSelectedDevice.value = null
    }

    fun callDisconnect() {
        listOfCharacteristic.clear()
        connectedDevice!!.disconnect()
        connectedDevice = null
        _typeOfSelectedDevice.value = null
    }

    fun isEnabled(): Boolean = bluetoothManager.adapter.isEnabled

    fun isInitialized(): Boolean = connectedDevice!!.isInitialized()

    fun diodeControl() {
        connectedDevice!!.setValueToControlDiode()
    }
}


