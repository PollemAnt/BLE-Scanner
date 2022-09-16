package com.example.blescanner

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siliconlab.bluetoothmesh.adk.connectable_device.*
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisionerConnection
import java.lang.reflect.Method
import java.util.*

class BluetoothConnectableDevice(result: ScanResult) : ConnectableDevice() {

    var refreshBluetoothDeviceCallback: RefreshBluetoothDeviceCallback? = null
    private lateinit var refreshGattServicesCallback: RefreshGattServicesCallback
    private var mtuSize = 0

    private val _isFragmentReadyToShow: MutableLiveData<Boolean> = MutableLiveData(false)
    val isFragmentReadyToShow: LiveData<Boolean> = _isFragmentReadyToShow

    private val _isButtonPressed: MutableLiveData<Boolean> = MutableLiveData()
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    private val _isDiodeOn: MutableLiveData<Boolean> = MutableLiveData()
    val isDiodeOn: LiveData<Boolean> = _isDiodeOn

    lateinit var diodeControlCharacteristic: BluetoothGattCharacteristic
    private lateinit var buttonStateCharacteristic: BluetoothGattCharacteristic

    private lateinit var bluetoothGatt: BluetoothGatt
    var scanResult: ScanResult = result
    val address: String
        get() = scanResult.device.address

    fun isInitialized(): Boolean = this::bluetoothGatt.isInitialized

    override fun getMTU(): Int = mtuSize

    override fun getName(): String? = scanResult.device.name

    override fun getAdvertisementData(): ByteArray? = scanResult.scanRecord!!.bytes

    override fun connect() {
        try {
            Log.v("qwe", "Connect to the GATT server on the device")
            bluetoothGatt = scanResult.device.connectGatt(
                BlinkyApplication.appContext,
                false,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (exception: IllegalArgumentException) {
            "Device not found. Unable to connect.".toast()
        }
    }

    override fun disconnect() {
        _isFragmentReadyToShow.postValue(false)
        if (BluetoothService.isInitialized()) {
            bluetoothGatt.let {
                bluetoothGatt.disconnect()
            }
            Log.v("qwe", "Disconnect from $name : $address")
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.v("qwe", "onConnectionStateChange status: $status")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED ->
                    onConnected()

                BluetoothProfile.STATE_DISCONNECTED ->
                    onDisconnected()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("qwe", "onServicesDiscovered GATT_SUCCESS")
                BluetoothService.listOfServices = gatt?.services as List<BluetoothGattService>
                createListOfCharacteristic(BluetoothService.listOfServices)

                if (BluetoothService.connectedDevice!!.hasService(Constants.BLINKY_SERVICE_UUID)) {
                    properCharacteristic()
                }
                _isFragmentReadyToShow.postValue(true)
                "Connected with $name : $address".toast()
            } else {
                Log.v("qwe", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.v("qwe", "onCharacteristicChanged")
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic!!.uuid == Constants.BUTTON_STATE_UUID)
                _isButtonPressed.postValue(characteristic.value[0] == 1.toByte())

            if (characteristic.uuid == Constants.MESH_PROVISION_CHARACTERISTIC_UUID || characteristic.uuid == Constants.MESH_SILICON_LABS_CHARACTERISTIC_UUID)
                updateData(characteristic.service.uuid, characteristic.uuid, characteristic.value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (characteristic == diodeControlCharacteristic) {
                _isDiodeOn.postValue(characteristic.value[0] == 1.toByte())
            }
            setNotificationDeviceButtonState()
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuSize = mtu
                gatt.discoverServices()
            }
        }
    }

    override fun onConnected() {
        super.onConnected()
        Log.v("qwe", "STATE_CONNECTED")
        val discoverServices = bluetoothGatt.discoverServices()
        Log.v("qwe", "Are services discover ? = $discoverServices")
        Log.v(
            "qwe",
            "Connection with: " + bluetoothGatt.device.name + " " + bluetoothGatt.device.address
        )
    }

    override fun onDisconnected() {
        super.onDisconnected()
        BluetoothService.listOfCharacteristic.clear()
        bluetoothGatt.close()
        _isFragmentReadyToShow.postValue(false)
        "Disconnected from $name:$address".toast()
    }

    private fun createListOfCharacteristic(gattServices: List<BluetoothGattService>) {
        BluetoothService.listOfCharacteristic.clear()
        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            gattCharacteristics.forEach { gattCharacteristic ->
                BluetoothService.listOfCharacteristic.add(gattCharacteristic)
            }
        }
    }

    private fun properCharacteristic() {
        diodeControlCharacteristic =
            BluetoothService.listOfServices[3].characteristics[0]
        buttonStateCharacteristic =
            BluetoothService.listOfServices[3].characteristics[1]
    }

    private fun setNotificationDeviceButtonState() {
        bluetoothGatt.setCharacteristicNotification(
            buttonStateCharacteristic,
            true
        )
        val descriptor = buttonStateCharacteristic.descriptors[0]
        descriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        bluetoothGatt.writeDescriptor(descriptor)
    }

    fun readDiodeStatus() {
        bluetoothGatt.readCharacteristic(diodeControlCharacteristic)
    }

    fun setValueToControlDiode() {
        _isDiodeOn.value = (diodeControlCharacteristic.value[0] == 1.toByte())
        val byteArray = if (_isDiodeOn.value!!) byteArrayOf(0x00) else byteArrayOf(0x01)
        writeData(
            Constants.BLINKY_SERVICE_UUID,
            Constants.DIODE_UUID,
            byteArray,
            object : ConnectableDeviceWriteCallback {
                override fun onWrite(p0: UUID?, p1: UUID?) {
                    _isDiodeOn.postValue(byteArray.contentEquals(byteArrayOf(0x01)))
                }

                override fun onFailed(p0: UUID?, p1: UUID?) {
                    "Led turn " + if (_isDiodeOn.value!!) "on" else "off" + " failed".toast()
                }
            }
        )
    }

    override fun writeData(
        service: UUID?,
        characteristic: UUID?,
        data: ByteArray?,
        connectableDeviceWriteCallback: ConnectableDeviceWriteCallback?
    ) {
        try {
            val bluetoothGattCharacteristic =
                bluetoothGatt.getService(service)!!
                    .getCharacteristic(characteristic)
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

    override fun hasService(service: UUID?): Boolean {
        if (bluetoothGatt.services.isNotEmpty()) {
            return bluetoothGatt.getService(service) != null
        } else {
            return scanResult.scanRecord?.serviceUuids?.contains(ParcelUuid(service))
                ?: return false
        }
    }

    override fun subscribe(
        service: UUID?,
        characteristic: UUID?,
        connectableDeviceSubscriptionCallback: ConnectableDeviceSubscriptionCallback
    ) {
        try {
            val bluetoothGattCharacteristic =
                bluetoothGatt.getService(service)!!
                    .getCharacteristic(characteristic)
            if (!bluetoothGatt.setCharacteristicNotification(
                    bluetoothGattCharacteristic,
                    true
                )
            ) {
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

    override fun unsubscribe(
        service: UUID?,
        characteristic: UUID?,
        connectableDeviceUnsubscriptionCallback: ConnectableDeviceUnsubscriptionCallback
    ) {
        try {
            val bluetoothGattCharacteristic =
                bluetoothGatt.getService(service)!!
                    .getCharacteristic(characteristic)
            if (!bluetoothGatt.setCharacteristicNotification(
                    bluetoothGattCharacteristic,
                    false
                )
            ) {
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

    override fun refreshBluetoothDevice(callback: RefreshBluetoothDeviceCallback) {
        refreshBluetoothDeviceCallback = callback
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
        val filters = ArrayList<ScanFilter>()
        val filter = ScanFilter.Builder().setDeviceAddress(address)
            .build()
        filters.add(filter)

        val refreshScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {

                result?.let {
                    if (it.device.address == address) {
                        BluetoothService.bluetoothScanner.stopScan(this)
                        scanResult = result
                        refreshBluetoothDeviceCallback?.success()
                    }
                }
            }
        }
        BluetoothService.bluetoothScanner.startScan(
            filters,
            settings,
            refreshScanCallback
        )
    }

    private fun refreshDeviceCache() {
        try {
            val refreshMethod: Method =
                bluetoothGatt.javaClass.getMethod("refresh")
            val result =
                refreshMethod.invoke(bluetoothGatt, *arrayOfNulls(0)) as? Boolean
            Log.d(ContentValues.TAG, "refreshDeviceCache $result")
        } catch (localException: Exception) {
            Log.e(ContentValues.TAG, "An exception occured while refreshing device")
        }
    }

    override fun refreshGattServices(callback: RefreshGattServicesCallback) {
        refreshGattServicesCallback = callback
        refreshDeviceCache()
        bluetoothGatt.discoverServices()
    }

    override fun getServiceData(service: UUID?): ByteArray? {
        return service?.let {
            scanResult.scanRecord?.getServiceData(ParcelUuid(it))
        }
    }

    override fun getUUID(): ByteArray? {
        val data = getServiceData(ProvisionerConnection.MESH_UNPROVISIONED_SERVICE)
        if (data != null && data.size >= 16) {
            val uuid = ByteArray(16)
            System.arraycopy(data, 0, uuid, 0, uuid.size)
            return uuid
        }
        return null
    }

    private fun String.toast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                BlinkyApplication.appContext,
                this,
                Toast.LENGTH_LONG
            )
                .show()
        }
    }
}