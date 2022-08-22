package com.example.blescanner

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siliconlab.bluetoothmesh.adk.connectable_device.ConnectableDeviceWriteCallback
import java.util.*


object BluetoothService {

    lateinit var bluetoothGatt: BluetoothGatt
    private val bluetoothManager =
        getSystemService(
            BlinkyApplication.appContext,
            BluetoothManager::class.java
        ) as BluetoothManager

    val bluetoothScanner: BluetoothLeScanner by lazy {
        bluetoothManager.adapter.bluetoothLeScanner
    }
    lateinit var listOfServices: List<BluetoothGattService>
    var listOfCharacteristic = mutableListOf<BluetoothGattCharacteristic>()

    lateinit var diodeControlCharacteristic: BluetoothGattCharacteristic
    lateinit var buttonStateCharacteristic: BluetoothGattCharacteristic

    var selectedDevice: BluetoothConnectableDevice? = null
        private set

    private val _isButtonPressed: MutableLiveData<Boolean> = MutableLiveData()
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    private val _connectableDevices: MutableLiveData<List<BluetoothConnectableDevice>> =
        MutableLiveData(emptyList())
    val connectableDevices: LiveData<List<BluetoothConnectableDevice>> = _connectableDevices

    private val _isDiodeOn: MutableLiveData<Boolean> = MutableLiveData()
    val isDiodeOn: LiveData<Boolean> = _isDiodeOn

    private val _isFragmentReadyToShow: MutableLiveData<Boolean> = MutableLiveData()
    val isFragmentReadyToShow: LiveData<Boolean> = _isFragmentReadyToShow

    private val scanSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()
    } else {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    }

    val scanCallback = object : ScanCallback() {
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
        }
    }

    fun stopScan() {
        if (ContextCompat.checkSelfPermission(
                BlinkyApplication.appContext,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothScanner.stopScan(scanCallback)
        }
    }

    fun setDeviceToConnect(bluetoothConnectableDevice: BluetoothConnectableDevice) {
        selectedDevice = bluetoothConnectableDevice
        _isFragmentReadyToShow.value = false
        bluetoothConnectableDevice.connect()
    }

    val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.v("qwe", "Start  onConnectionStateChange ")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v("qwe", "Successfully connected to the GATT Server")
                val discoverServices = gatt.discoverServices()
                Log.v("qwe", "Are services discover ? = $discoverServices")
                Log.v("qwe", "Connection with: " + gatt.device.name + " " + gatt.device.address)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v("qwe", "disconnected from the GATT Server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("qwe", "onServicesDiscovered GATT_SUCCESS")
                listOfServices = gatt?.services as List<BluetoothGattService>
                Log.v("list", "listOfServices:$listOfServices")
                createListOfCharacteristic(listOfServices)
                properUUID()
                readDiodeStatus()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(BlinkyApplication.appContext, "Connected", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                Log.v("qwe", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic == buttonStateCharacteristic) {
                _isButtonPressed.postValue(characteristic.value.contentToString() == "[1]")
                super.onCharacteristicChanged(gatt, characteristic)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (characteristic == diodeControlCharacteristic) {
                _isDiodeOn.postValue(characteristic.value.contentToString() == "[1]")
            }
            setNotificationDeviceButtonState()
            super.onCharacteristicRead(gatt, characteristic, status)
        }
    }

    private fun createListOfCharacteristic(gattServices: List<BluetoothGattService>) {
        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            gattCharacteristics.forEach { gattCharacteristic ->
                listOfCharacteristic.add(gattCharacteristic)
            }
        }
    }

    private fun properUUID() {
        diodeControlCharacteristic = listOfServices[3].characteristics[0]
        buttonStateCharacteristic = listOfServices[3].characteristics[1]
    }

    private fun setNotificationDeviceButtonState() {
        bluetoothGatt.setCharacteristicNotification(buttonStateCharacteristic, true)

        val descriptor = buttonStateCharacteristic.descriptors[0]
        descriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        bluetoothGatt.writeDescriptor(descriptor)
    }

    fun readDiodeStatus() {
        bluetoothGatt.readCharacteristic(diodeControlCharacteristic)
        _isFragmentReadyToShow.postValue(true)
    }

    fun diodeControl() {
        _isDiodeOn.value = (diodeControlCharacteristic.value.contentToString() == "[1]")
        val byteArray = if (_isDiodeOn.value!!) byteArrayOf(0x00) else byteArrayOf(0x01)
        selectedDevice?.writeData(
            listOfServices[3].uuid,
            diodeControlCharacteristic.uuid,
            byteArray,
            object : ConnectableDeviceWriteCallback {

                override fun onWrite(p0: UUID?, p1: UUID?) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            BlinkyApplication.appContext,
                            "Led is " + if (_isDiodeOn.value!!) "off" else "on",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailed(p0: UUID?, p1: UUID?) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            BlinkyApplication.appContext,
                            "Led turn " + if (_isDiodeOn.value!!) "on" else "off" + " failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    fun disconnectOnFragmentDestroy() {
        listOfCharacteristic.clear()
        selectedDevice?.disconnect()
        selectedDevice = null
    }

    fun isEnabled(): Boolean {
        return bluetoothManager.adapter.isEnabled
    }

    fun isInitialized(): Boolean {
        return this::bluetoothGatt.isInitialized
    }
}


