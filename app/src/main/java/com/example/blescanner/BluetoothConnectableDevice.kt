package com.example.blescanner

import android.bluetooth.*
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

    //I dont use it yet, this variables is copy-paste from the documentation
    private var refreshBluetoothDeviceCallback: RefreshBluetoothDeviceCallback? = null
    lateinit var refreshGattServicesCallback: RefreshGattServicesCallback
    private var mtuSize = 0

    private val _isFragmentReadyToShow: MutableLiveData<Boolean> = MutableLiveData(false)
    val isFragmentReadyToShow: LiveData<Boolean> = _isFragmentReadyToShow

    private val _isButtonPressed: MutableLiveData<Boolean> = MutableLiveData()
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    private val _isDiodeOn: MutableLiveData<Boolean> = MutableLiveData()
    val isDiodeOn: LiveData<Boolean> = _isDiodeOn

    lateinit var diodeControlCharacteristic: BluetoothGattCharacteristic
    lateinit var buttonStateCharacteristic: BluetoothGattCharacteristic

    lateinit var bluetoothGatt: BluetoothGatt
    var scanResult: ScanResult = result
    val address: String
        get() = scanResult.device.address

    fun isInitialized(): Boolean {
        return this::bluetoothGatt.isInitialized
    }

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
            Toast.makeText(
                BlinkyApplication.appContext,
                "Device not found. Unable to connect.",
                Toast.LENGTH_SHORT
            ).show()
            Log.v("qwe", "Device not found. Unable to connect.")
        }
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
                    Log.v("qwe", "ConnectableDeviceWriteCallback: onWrite ")
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

    override fun disconnect() {
        _isFragmentReadyToShow.postValue(false)
        if (BluetoothService.isInitialized()) {
            bluetoothGatt.let {
                bluetoothGatt.close()
            }
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

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.v("qwe", "Start  onConnectionStateChange ")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v("qwe", "Successfully connected to the GATT Server")
                val discoverServices = gatt.discoverServices()
                Log.v("qwe", "Are services discover ? = $discoverServices")
                Log.v("qwe", "Connection with: " + gatt.device.name + " " + gatt.device.address)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onConnectionFailed()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("qwe", "onServicesDiscovered GATT_SUCCESS")
                BluetoothService.listOfServices = gatt?.services as List<BluetoothGattService>
                createListOfCharacteristic(BluetoothService.listOfServices)

                if (BluetoothService.selectedDevice!!.hasService(Constants.BLINKY_SERVICE_UUID)) {
                    properCharacteristic()
                    readDiodeStatus()
                }
                _isFragmentReadyToShow.postValue(true)

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
                _isButtonPressed.postValue(characteristic.value[0] == 1.toByte())
                super.onCharacteristicChanged(gatt, characteristic)
            }
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

        //I dont use it yet, this function is copy-paste from the documentation
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuSize = mtu
                gatt.discoverServices()
            }
        }
    }

    private fun onConnectionFailed() {
        disconnect()
        _isFragmentReadyToShow.postValue(false)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                BlinkyApplication.appContext,
                "Disconnected from the GATT Server",
                Toast.LENGTH_SHORT
            ).show()
        }
        Log.v("qwe", "disconnected from the GATT Server")
    }

    private fun createListOfCharacteristic(gattServices: List<BluetoothGattService>) {
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

    private fun readDiodeStatus() {
        bluetoothGatt.readCharacteristic(diodeControlCharacteristic)
        _isFragmentReadyToShow.postValue(true)
    }


    //I dont use it yet, this function is copy-paste from the documentation
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

    //I dont use it yet, this function is copy-paste from the documentation
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


    //I dont use it yet, this function is copy-paste from the documentation
    override fun refreshBluetoothDevice(callback: RefreshBluetoothDeviceCallback) {
        refreshBluetoothDeviceCallback = callback
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        BluetoothService.bluetoothScanner.startScan(null, settings, BluetoothService.scanCallback)
    }

    //I dont use it yet, this function is copy-paste from the documentation
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

    //I dont use it yet, this function is copy-paste from the documentation
    override fun refreshGattServices(callback: RefreshGattServicesCallback) {
        refreshGattServicesCallback = callback
        refreshDeviceCache()
        bluetoothGatt.discoverServices()
    }

    override fun getMTU(): Int {
        return mtuSize
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun getServiceData(service: UUID?): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getName(): String? {
        return scanResult.device.name
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun getAdvertisementData(): ByteArray? = advertisementData

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


}