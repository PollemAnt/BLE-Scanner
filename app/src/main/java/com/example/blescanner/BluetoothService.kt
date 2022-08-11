package com.example.blescanner

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService

object BluetoothService {

    private var bluetoothGatt: BluetoothGatt? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    var scanResults = mutableListOf<ScanResult>()
    lateinit var scanResultAdapter: ScanResultAdapter
    lateinit var listOfServices: List<BluetoothGattService>
    var listOfCharacteristic = mutableListOf<BluetoothGattCharacteristic>()
    lateinit var bluetoothDevice: BluetoothDevice
    private val bleScanner by lazy {
        bluetoothAdapter!!.bluetoothLeScanner
    }
    private var isDiodeOn = true
    private var isButtonPressed = false

    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    val isBluetoothPermissionGranted
        get() = hasPermission(Manifest.permission.BLUETOOTH)

    val isLocationOn
        get() = isLocationEnabled()

    fun initialize() {
        bluetoothAdapter = provideBluetoothAdapter()
    }

    private fun provideBluetoothAdapter(): BluetoothAdapter {
        val bluetoothManager =
            getSystemService(
                BlinkyApplication.appContext,
                BluetoothManager::class.java
            ) as BluetoothManager
        return bluetoothManager.adapter
    }

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

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null && ContextCompat.checkSelfPermission(
                    BlinkyApplication.appContext,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val indexQuery =
                    scanResults.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) {
                    scanResults[indexQuery] = result
                    scanResultAdapter.notifyItemChanged(indexQuery)
                } else {
                    scanResults.add(result)
                    scanResults.sortByDescending { it.rssi }
                    scanResults.removeAll { it.device.name == null }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.v("asd", "onScanFailed: code $errorCode")
        }
    }

    fun checkActivation(): Boolean {
        return checkLocationPermission() && checkBluetoothPermission()
    }

    fun startScan() {
        if (ContextCompat.checkSelfPermission(
                BlinkyApplication.appContext,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scanResults.clear()
            scanResultAdapter.setData(scanResults)
            bleScanner.startScan(null, scanSettings, scanCallback)
        }
    }

    fun stopScan() {
        if (ContextCompat.checkSelfPermission(
                BlinkyApplication.appContext,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bleScanner.stopScan(scanCallback)
        }
    }

    fun clearScan() {
        scanResultAdapter.setData(mutableListOf())
    }

    fun connect(address: String?): Boolean {
        prepareToNewConnection()
        bluetoothAdapter?.let { adapter ->
            try {
                bluetoothDevice = adapter.getRemoteDevice(address)
                Log.v("qwe", "connect to the GATT server on the device")
                bluetoothGatt =
                    bluetoothDevice.connectGatt(
                        BlinkyApplication.appContext,
                        true,
                        bluetoothGattCallback
                    )
                return true
            } catch (exception: IllegalArgumentException) {
                Log.v("qwe", "Device not found with provided address.  Unable to connect.")
                return false
            }
        } ?: run {
            Log.v("qwe", "BluetoothAdapter not initialized")
            return false
        }
    }

    fun prepareToNewConnection() {
        listOfCharacteristic.clear()
        isButtonPressed = false
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.v("qwe", "Start  onConnectionStateChange ")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v("qwe", "successfully connected to the GATT Server")
                val discoverServices = gatt.discoverServices()
                Log.v("qwe", "Czy discoverServices sie udal? = $discoverServices")
                Log.v("qwe", "Lączenie z: " + gatt.device.name + " " + gatt.device.address)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v("qwe", "disconnected from the GATT Server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("qwe", "onServicesDiscovered GATT_SUCCESS")
                listOfServices = gatt?.services as List<BluetoothGattService>
                Log.v("lista", "listOfServices:$listOfServices")
            } else {
                Log.v("qwe", "onServicesDiscovered received: $status")
            }
            createListOfCharacteristic(listOfServices)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(BlinkyApplication.appContext, "Connected", Toast.LENGTH_SHORT).show()
            }
            setNotificationDeviceButtonState()
        }

        private fun createListOfCharacteristic(gattServices: List<BluetoothGattService>) {
            gattServices.forEach { gattService ->
                val gattCharacteristics = gattService.characteristics
                gattCharacteristics.forEach { gattCharacteristic ->
                    listOfCharacteristic.add(gattCharacteristic)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (gatt!!.services[3].characteristics[1] == characteristic) {
                Log.v("qwe", "onCharacteristicChanged")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        BlinkyApplication.appContext,
                        if (isButtonPressed) "Button is pressed " else "Button is not pressed  ",
                        Toast.LENGTH_SHORT
                    ).show()
                    isButtonPressed = !isButtonPressed
                }
            }
            super.onCharacteristicChanged(gatt, characteristic)
        }
    }

    private fun setNotificationDeviceButtonState() {
        val buttonState = bluetoothGatt!!.services[3].characteristics[1]
        bluetoothGatt!!.setCharacteristicNotification(buttonState, true)

        val descriptor = buttonState.descriptors[0]
        descriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        bluetoothGatt!!.writeDescriptor(descriptor)
    }

    fun diodeControl(listOfService: List<BluetoothGattService>) {
        val characteristic = listOfService[3].characteristics[0]
        val byteArray = if (isDiodeOn) byteArrayOf(0x01) else byteArrayOf(0x00)
        characteristic.value = byteArray
        bluetoothGatt?.writeCharacteristic(characteristic)
        Toast.makeText(
            BlinkyApplication.appContext,
            "Led is " + if (isDiodeOn) "on" else "off",
            Toast.LENGTH_LONG
        ).show()
        isDiodeOn = !isDiodeOn
    }

    fun hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(BlinkyApplication.appContext, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(
                BlinkyApplication.appContext,
                LocationManager::class.java
            ) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkLocationPermission(): Boolean {
        return when (ContextCompat.checkSelfPermission(
            BlinkyApplication.appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    private fun checkBluetoothPermission(): Boolean {
        return when (ContextCompat.checkSelfPermission(
            BlinkyApplication.appContext,
            Manifest.permission.BLUETOOTH
        )) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }


}