package com.example.blescanner

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siliconlab.bluetoothmesh.adk.connectable_device.*
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisionerConnection
import java.lang.reflect.Method
import java.util.*


object BluetoothService : ConnectableDevice() {

    private lateinit var bluetoothGatt: BluetoothGatt
    private val bluetoothManager =
        getSystemService(
            BlinkyApplication.appContext,
            BluetoothManager::class.java
        ) as BluetoothManager

    private val bluetoothScanner: BluetoothLeScanner by lazy {
        bluetoothManager.adapter.bluetoothLeScanner
    }

    private val _scanResults: MutableLiveData<List<ScanResult>> = MutableLiveData(emptyList())
    val scanResults: LiveData<List<ScanResult>> = _scanResults

    lateinit var listOfServices: List<BluetoothGattService>
    var listOfCharacteristic = mutableListOf<BluetoothGattCharacteristic>()
    lateinit var bluetoothDevice: BluetoothDevice

    private var isDiodeOn: String = ""
    private var isButtonPressed = false

    private val _newDiode: MutableLiveData<Boolean> = MutableLiveData(false)
    val newDiode: LiveData<Boolean> = _newDiode

    //I dont use it yet, this variables is copy-paste from the documentation
    private lateinit var scanResult: ScanResult
    private var refreshBluetoothDeviceCallback: RefreshBluetoothDeviceCallback? = null
    lateinit var refreshGattServicesCallback: RefreshGattServicesCallback
    private var mtuSize = 0

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
                val results = _scanResults.value!!.toMutableList()

                val indexQuery =
                    results.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) {
                    results[indexQuery] = result

                    _scanResults.value = results
                    //scanResultAdapter.notifyItemChanged(indexQuery)
                } else {
                    results.add(result)
                    results.sortByDescending { it.rssi }
                    results.removeAll { it.device.name == null }

                    _scanResults.value = results
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
            scanResults.clear()
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

    fun clearScan() {
        scanResultAdapter.setData(mutableListOf())
    }

    fun setDeviceToConnect(scanResult: ScanResult) {
        bluetoothDevice = scanResult.device
    }

    override fun connect() {
        prepareToConnection()
        try {
            Log.v("qwe", "Connect to the GATT server on the device")
            bluetoothGatt = bluetoothDevice.connectGatt(
                BlinkyApplication.appContext,
                false,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (exception: IllegalArgumentException) {
            Log.v("qwe", "Device not found.  Unable to connect.")
        }
    }

    private fun prepareToConnection() {
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
                //refreshGattServicesCallback.onSuccess()
            } else {
                Log.v("qwe", "onServicesDiscovered received: $status")
                refreshGattServicesCallback.onFail()
            }
            createListOfCharacteristic(listOfServices)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(BlinkyApplication.appContext, "Connected", Toast.LENGTH_LONG).show()
            }
            setNotificationDeviceButtonState()
        }

        //I dont use it yet, this function is copy-paste from the documentation
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuSize = mtu
                gatt.discoverServices()
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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (gatt!!.services[3].characteristics[1] == characteristic) {
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

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (characteristic == listOfServices[3].characteristics[0])
                diodeControl()
        }
    }

    private fun setNotificationDeviceButtonState() {
        val buttonState = bluetoothGatt.services[3].characteristics[1]
        bluetoothGatt.setCharacteristicNotification(buttonState, true)

        val descriptor = buttonState.descriptors[0]
        descriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        bluetoothGatt.writeDescriptor(descriptor)
    }

    fun readDiodeStatus() {
        bluetoothGatt.readCharacteristic(listOfServices[3].characteristics[0])
    }

    private fun diodeControl() {
        val characteristic = listOfServices[3].characteristics[0]
        isDiodeOn = listOfServices[3].characteristics[0].value.contentToString()
        val byteArray = if (isDiodeOn == "[0]") byteArrayOf(0x01) else byteArrayOf(0x00)
        writeData(
            listOfServices[3].uuid,
            characteristic.uuid,
            byteArray,
            BluetoothService
        )
        Toast.makeText(
            BlinkyApplication.appContext,
            "Led is " + if (isDiodeOn == "[0]") "on" else "off",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun writeData(
        service: UUID?,
        characteristic: UUID?,
        data: ByteArray?,
        connectableDeviceWriteCallback: ConnectableDeviceWriteCallback?
    ) {
        try {
            val bluetoothGattCharacteristic =
                bluetoothGatt.getService(service)!!.getCharacteristic(characteristic)
            bluetoothGattCharacteristic.value = data
            //bluetoothGattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            if (!bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)) {
                throw Exception("Writing to characteristic failed")
            }
            connectableDeviceWriteCallback?.onWrite(service, characteristic)
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "writeData error: ${e.message}")
            connectableDeviceWriteCallback?.onFailed(service, characteristic)
        }
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun refreshBluetoothDevice(callback: RefreshBluetoothDeviceCallback) {
        refreshBluetoothDeviceCallback = callback
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bluetoothLeScanner.startScan(null, settings, scanCallback)
    }

    //I dont use it yet, this function is copy-paste from the documentation
    private fun refreshDeviceCache() {
        try {
            val refreshMethod: Method = bluetoothGatt.javaClass.getMethod("refresh")
            val result = refreshMethod.invoke(bluetoothGatt, *arrayOfNulls(0)) as? Boolean
            Log.d(ContentValues.TAG, "refreshDeviceCache $result")
        } catch (localException: Exception) {
            Log.e(ContentValues.TAG, "An exception occured while refreshing device")
        }
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun refreshGattServices(callback: RefreshGattServicesCallback) {
        refreshGattServicesCallback = callback
        refreshDeviceCache()
        bluetoothGatt.discoverServices()
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun disconnect() {
        if (this::bluetoothGatt.isInitialized) {
            bluetoothGatt.let {
                bluetoothGatt.close()
            }
        }
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun getMTU(): Int {
        return mtuSize
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun hasService(service: UUID?): Boolean {
        if (bluetoothGatt.services.isNotEmpty()) {
            return bluetoothGatt.getService(service) != null
        } else {
            return scanResult.scanRecord?.serviceUuids?.contains(ParcelUuid(service))
                ?: return false
        }
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun getServiceData(service: UUID?): ByteArray {
        TODO("Not yet implemented")
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun subscribe(
        service: UUID?,
        characteristic: UUID?,
        connectableDeviceSubscriptionCallback: ConnectableDeviceSubscriptionCallback
    ) {
        try {
            val bluetoothGattCharacteristic =
                bluetoothGatt.getService(service)!!.getCharacteristic(characteristic)
            if (!bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)) {
                throw Exception("Enabling characteristic notification failed")
            }
            val bluetoothGattDescriptor = bluetoothGattCharacteristic.descriptors.takeIf {
                it.size == 1
            }?.first()
                ?: throw Exception("Descriptors size (${bluetoothGattCharacteristic.descriptors.size}) different than expected: 1")
            bluetoothGattDescriptor.apply {
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            if (!bluetoothGatt.writeDescriptor(bluetoothGattDescriptor)) {
                throw Exception("Writing to descriptor failed")
            }
            connectableDeviceSubscriptionCallback.onSuccess(service, characteristic)
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "subscribe error: ${e.message}")
            connectableDeviceSubscriptionCallback.onFail(service, characteristic)
        }
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun unsubscribe(
        service: UUID?,
        characteristic: UUID?,
        connectableDeviceUnsubscriptionCallback: ConnectableDeviceUnsubscriptionCallback
    ) {
        try {
            val bluetoothGattCharacteristic =
                bluetoothGatt.getService(service)!!.getCharacteristic(characteristic)
            if (!bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, false)) {
                throw Exception("Disabling characteristic notification failed")
            }
            val bluetoothGattDescriptor = bluetoothGattCharacteristic.descriptors.takeIf {
                it.size == 1
            }?.first()
                ?: throw Exception("Descriptors size (${bluetoothGattCharacteristic.descriptors.size}) different than expected: 1")
            bluetoothGattDescriptor.apply {
                value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            if (!bluetoothGatt.writeDescriptor(bluetoothGattDescriptor)) {
                throw Exception("Writing to descriptor failed")
            }
            connectableDeviceUnsubscriptionCallback.onSuccess(service, characteristic)
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "unsubscribe error: ${e.message}")
            connectableDeviceUnsubscriptionCallback.onFail(service, characteristic)
        }
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun getName(): String? {
        return bluetoothDevice.name
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun getAdvertisementData(): ByteArray? = TODO("Not yet implemented")

    //I dont use it yet, this function is copy-paste from the documentation
    override fun getUUID(): ByteArray? {
        val data = getServiceData(ProvisionerConnection.MESH_UNPROVISIONED_SERVICE)
        if (data != null && data.size >= 16) {
            val uuid = ByteArray(16)
            System.arraycopy(data, 0, uuid, 0, uuid.size)
            return uuid
        }
        return null
    }

    //connectableDeviceWriteCallback functions :
    override fun onWrite(p0: UUID?, p1: UUID?) {
        Log.v("qwe", "connectableDeviceWriteCallback onWrite: " + p0 + " " + p1)
    }

    override fun onFailed(p0: UUID?, p1: UUID?) {
        Log.v("qwe", "connectableDeviceWriteCallback onFailed: " + p0 + " " + p1)
    }


}