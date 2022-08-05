package com.example.blescanner

import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

object Device {
    var name :String? = null
    var address :String? = null
    val services: MutableList<BluetoothGattService> = mutableListOf()
    val characteristics: MutableList<BluetoothGattCharacteristic> = mutableListOf()
    val device: HashMap<String, String> = HashMap()
    var bluetoothGatt: BluetoothGatt? = null
    var buttonState :Boolean = true
    var activity : Activity? = null
}