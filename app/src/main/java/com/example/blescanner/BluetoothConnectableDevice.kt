package com.example.blescanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues
import android.os.ParcelUuid
import android.util.Log
import com.siliconlab.bluetoothmesh.adk.connectable_device.*
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisionerConnection
import java.lang.reflect.Method
import java.util.*

class BluetoothConnectableDevice(result: ScanResult) : ConnectableDevice() {


    //I dont use it yet, this variables is copy-paste from the documentation
    private var refreshBluetoothDeviceCallback: RefreshBluetoothDeviceCallback? = null
    lateinit var refreshGattServicesCallback: RefreshGattServicesCallback
    private var mtuSize = 0


    var scanResult: ScanResult = result
    val address: String
        get() = scanResult.device.address

    override fun connect() {
        try {
            Log.v("qwe", "Connect to the GATT server on the device")
            BluetoothService.bluetoothGatt = scanResult.device.connectGatt(
                BlinkyApplication.appContext,
                false,
                BluetoothService.bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (exception: IllegalArgumentException) {
            Log.v("qwe", "Device not found.  Unable to connect.")
        }
    }

    override fun writeData(
        service: UUID?,
        characteristic: UUID?,
        data: ByteArray?,
        connectableDeviceWriteCallback: ConnectableDeviceWriteCallback?
    ) {
        try {
            val bluetoothGattCharacteristic =
                BluetoothService.bluetoothGatt.getService(service)!!
                    .getCharacteristic(characteristic)
            bluetoothGattCharacteristic.value = data
            //bluetoothGattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            if (!BluetoothService.bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic)) {
                throw Exception("Writing to characteristic failed")
            }
            connectableDeviceWriteCallback?.onWrite(service, characteristic)
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "writeData error: ${e.message}")
            connectableDeviceWriteCallback?.onFailed(service, characteristic)
        }
    }

    override fun disconnect() {
        if (BluetoothService.isInitialized()) {
            BluetoothService.bluetoothGatt.let {
                BluetoothService.bluetoothGatt.close()
            }
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        //I dont use it yet, this function is copy-paste from the documentation
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuSize = mtu
                gatt.discoverServices()
            }
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
                BluetoothService.bluetoothGatt.javaClass.getMethod("refresh")
            val result =
                refreshMethod.invoke(BluetoothService.bluetoothGatt, *arrayOfNulls(0)) as? Boolean
            Log.d(ContentValues.TAG, "refreshDeviceCache $result")
        } catch (localException: Exception) {
            Log.e(ContentValues.TAG, "An exception occured while refreshing device")
        }
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun refreshGattServices(callback: RefreshGattServicesCallback) {
        refreshGattServicesCallback = callback
        refreshDeviceCache()
        BluetoothService.bluetoothGatt.discoverServices()
    }


    //I dont use it yet, this function is copy-paste from the documentation
    override fun getMTU(): Int {
        return mtuSize
    }

    //I dont use it yet, this function is copy-paste from the documentation
    override fun hasService(service: UUID?): Boolean {
        if (BluetoothService.bluetoothGatt.services.isNotEmpty()) {
            return BluetoothService.bluetoothGatt.getService(service) != null
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
                BluetoothService.bluetoothGatt.getService(service)!!
                    .getCharacteristic(characteristic)
            if (!BluetoothService.bluetoothGatt.setCharacteristicNotification(
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
            if (!BluetoothService.bluetoothGatt.writeDescriptor(bluetoothGattDescriptor)) {
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
                BluetoothService.bluetoothGatt.getService(service)!!
                    .getCharacteristic(characteristic)
            if (!BluetoothService.bluetoothGatt.setCharacteristicNotification(
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
            if (!BluetoothService.bluetoothGatt.writeDescriptor(bluetoothGattDescriptor)) {
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
        return scanResult.device.name
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
}