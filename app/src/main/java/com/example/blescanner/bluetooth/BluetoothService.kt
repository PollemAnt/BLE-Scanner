package com.example.blescanner.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blescanner.core.BlinkyApplication
import com.example.blescanner.data.models.BluetoothConnectableDevice
import java.util.ArrayList

object BluetoothService {

    private val scanner = BluetoothScanner()
    private val connector = BluetoothConnector()
    private val deviceManager = BluetoothDeviceManager()

    private val _scannedDevices = MutableLiveData<List<BluetoothConnectableDevice>>(emptyList())
    val scannedDevices: LiveData<List<BluetoothConnectableDevice>> = _scannedDevices

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _deviceType = MutableLiveData<String?>(null)
    val deviceType: LiveData<String?> = _deviceType

    var listOfServices = mutableListOf<BluetoothGattService>()
    var listOfCharacteristics = mutableListOf<BluetoothGattCharacteristic>()


    fun startScan() {
        _isScanning.postValue(true)
        scanner.startScan { device ->
            val deviceList = _scannedDevices.value.orEmpty().toMutableList()
            val index =
                deviceList.indexOfFirst { it.scanResult.device.address == device.scanResult.device.address }

            if (index != -1) {
                deviceList[index] = device
            } else {
                deviceList.add(device)
            }

            deviceList.sortByDescending { it.scanResult.rssi }
            _scannedDevices.postValue(deviceList)
        }
    }

    fun stopScan() {
        scanner.stopScan()
        _isScanning.postValue(false)
    }

    fun clearScanList() {
        _scannedDevices.postValue(emptyList())
    }

    fun selectAndConnectDevice(device: BluetoothConnectableDevice) {
        val type = deviceManager.selectDevice(device)
        _deviceType.value = type

        if (type != "Unknown") {
            connector.connectTo(device)
        }
    }

    fun getConnectedDevice(): BluetoothConnectableDevice? = connector.getConnectedDevice()

    fun disconnect() {
        listOfServices.clear()
        listOfCharacteristics.clear()
        connector.disconnect()
        _deviceType.value = null
    }

    fun onBackPressed() {
        _deviceType.value  = null
    }

    fun isEnabled(): Boolean {
        val bluetoothManager = BlinkyApplication.appContext.getSystemService(BluetoothManager::class.java)
        return bluetoothManager?.adapter?.isEnabled == true
    }

    fun isConnected(): Boolean = connector.isConnected()

    fun diodeControl() {
        connector.controlDiode()
    }

    fun getSelectedDevice(): BluetoothConnectableDevice? = deviceManager.getSelectedDevice()

    fun refreshBluetoothDevice(
        filters: ArrayList<ScanFilter>,
        settings: ScanSettings?,
        refreshScanCallback: ScanCallback
    ) {
        scanner.scanAfterRefresh(filters,settings,refreshScanCallback)
    }
}



