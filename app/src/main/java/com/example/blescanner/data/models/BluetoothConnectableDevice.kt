package com.example.blescanner.data.models

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blescanner.core.BlinkyApplication
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.utils.Constants
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

    override fun getAdvertisementData(): ByteArray? = scanResult.scanRecord?.bytes

    override fun connect() {
        try {
            _log("Connect to the GATT server on the device")
            bluetoothGatt = scanResult.device.connectGatt(
                BlinkyApplication.appContext,
                false,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (exception: IllegalArgumentException) {
            _log("Device not found. Unable to connect.")
        }
    }

    override fun disconnect() {
        _isFragmentReadyToShow.postValue(false)
        if (BluetoothService.isInitialized()) {
            bluetoothGatt.let {
                bluetoothGatt.disconnect()
            }
            _log("Disconnect from $name : $address")
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED ->
                        onConnected()

                    BluetoothProfile.STATE_DISCONNECTED ->
                        onDisconnected()
                }
            } else {
                _isFragmentReadyToShow.postValue(false)
                // Ensure gatt.close() is called to free up resources
                gatt.close()

                onDisconnected()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                gatt?.services?.let { services ->
                    BluetoothService.listOfServices.clear()
                    BluetoothService.listOfServices.addAll(services)
                    createListOfCharacteristic(services)

                    if (hasService(Constants.BLINKY_SERVICE_UUID)) {
                        if (properCharacteristic()) {
                            readDiodeStatus()
                            setNotificationDeviceButtonState()
                            _isFragmentReadyToShow.postValue(true)
                        } else {
                            _log("Failed to initialize required characteristics. Disconnecting.")
                            _isFragmentReadyToShow.postValue(false)
                            disconnect()
                            return
                        }
                    } else {
                        _isFragmentReadyToShow.postValue(true)
                    }
                    _log("Connected with $name : $address")
                } ?: run {
                    _log("onServicesDiscovered: GATT services were null")
                }

            } else {
                _log("onServicesDiscovered received: $status. Disconnecting.")
                disconnect()
            }
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic ?: return
            when (characteristic.uuid) {
                Constants.BUTTON_STATE_UUID -> _isButtonPressed.setValue(characteristic.value?.firstOrNull() == 1.toByte())

                Constants.MESH_PROVISION_CHARACTERISTIC_UUID,
                Constants.MESH_SILICON_LABS_CHARACTERISTIC_UUID ->
                    updateData(
                        characteristic.service.uuid,
                        characteristic.uuid,
                        characteristic.value
                    )
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                characteristic ?: return
                if (characteristic == diodeControlCharacteristic) {
                    val value = characteristic.value.firstOrNull()
                    if (value != null) {
                        _isDiodeOn.postValue(value == 1.toByte())
                    }
                }
            }

            setNotificationDeviceButtonState()
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuSize = mtu
                discoverServices(gatt)
            }
        }
    }

    override fun onConnected() {
        super.onConnected()
        _log("STATE_CONNECTED")
        val mtuRequested = bluetoothGatt.requestMtu(517)
        if (!mtuRequested) {
            discoverServices(bluetoothGatt)
        }
        val discoverServices = bluetoothGatt.discoverServices()
        _log("Are services discover ? = $discoverServices")
        _log("Connection with: " + bluetoothGatt.device.name + " " + bluetoothGatt.device.address)
    }

    private fun discoverServices(bluetoothGatt: BluetoothGatt) {
        if (!bluetoothGatt.discoverServices()) {
            _log("Failed to initiate service discovery for ${bluetoothGatt.device.address}. Disconnecting.")
            disconnect() // Or handle error appropriately
        }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        BluetoothService.listOfCharacteristics.clear()
        BluetoothService.listOfServices.clear() // Clear services too
        if (this::bluetoothGatt.isInitialized) { // Check if initialized before closing
            bluetoothGatt.close() // Close GATT to release resources
        }
        _isFragmentReadyToShow.postValue(false)
        _isButtonPressed.postValue(false) // Reset button state
        _isDiodeOn.postValue(false) // Reset diode state
        _log("Disconnected from $name:$address")
    }

    private fun createListOfCharacteristic(gattServices: List<BluetoothGattService>) {
        BluetoothService.listOfCharacteristics.clear()
        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            gattCharacteristics.forEach { gattCharacteristic ->
                BluetoothService.listOfCharacteristics.add(gattCharacteristic)
            }
        }
    }

    private fun properCharacteristic(): Boolean {
        val service = BluetoothService.listOfServices.find {
            it.uuid == Constants.BLINKY_SERVICE_UUID
        } ?: run {
            _log("Blinky service not found")
            return false
        }
        diodeControlCharacteristic = service.characteristics.find {
            it.uuid == Constants.DIODE_UUID
        } ?: run {
            _log("Diode characteristic not found")
            return false
        }
        buttonStateCharacteristic = service.characteristics.find {
            it.uuid == Constants.BUTTON_STATE_UUID
        } ?: run {
            _log("Button state characteristic not found")
            return false
        }
        return true
    }

    private fun setNotificationDeviceButtonState() {
        writeWithNotification(buttonStateCharacteristic, true)
    }

    private fun writeWithNotification(
        characteristic: BluetoothGattCharacteristic,
        enable: Boolean
    ): Boolean {
        if (!bluetoothGatt.setCharacteristicNotification(characteristic, enable)) return false

        val descriptor = characteristic.descriptors.firstOrNull()
            ?: return false

        descriptor.value = if (enable)
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        else
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

        return bluetoothGatt.writeDescriptor(descriptor)
    }

    fun readDiodeStatus() {
        if (!this::diodeControlCharacteristic.isInitialized) return

        if (!bluetoothGatt.readCharacteristic(diodeControlCharacteristic)) {
            _log("Failed to initiate readDiodeStatus")
        }
    }

    fun setValueToControlDiode() {

        if (!this::diodeControlCharacteristic.isInitialized) return

        val currentDiodeState = _isDiodeOn.value ?: false
        val byteArray = if (currentDiodeState) byteArrayOf(0x00) else byteArrayOf(0x01)

        writeData(
            Constants.BLINKY_SERVICE_UUID,
            Constants.DIODE_UUID,
            byteArray,
            object : ConnectableDeviceWriteCallback {
                override fun onWrite(p0: UUID?, p1: UUID?) {
                    _isDiodeOn.postValue(byteArray.firstOrNull() == 0x01.toByte())
                }

                override fun onFailed(p0: UUID?, p1: UUID?) {
                    _log("Led turn " + if (byteArray.firstOrNull() == 0x01.toByte()) "on" else "off" + " failed")
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
        val bluetoothGattCharacteristic = service?.let { srv ->
            characteristic?.let { charUUID ->
                bluetoothGatt.getService(srv)?.getCharacteristic(charUUID)
            }
        } ?: run {
            connectableDeviceWriteCallback?.onFailed(service, characteristic)
            return
        }
        try {
            bluetoothGattCharacteristic.value = data
            if (!bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)) {
                throw Exception("Writing failed")
            }
            connectableDeviceWriteCallback?.onWrite(service, characteristic)
        } catch (e: Exception) {
            _log("writeData: ${e.message}")
            connectableDeviceWriteCallback.onFailed(service, characteristic)
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
                        BluetoothService.stopScan()
                        scanResult = result
                        refreshBluetoothDeviceCallback?.success()
                    }
                }
            }
        }
        BluetoothService.refreshBluetoothDevice(
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

    private fun _log(message: String) {
        Log.d("BluetoothDevice", message)
        // W przyszłości: emituj przez LiveData do ViewModel -> Fragment jeśli potrzebujesz to pokazać
    }
}