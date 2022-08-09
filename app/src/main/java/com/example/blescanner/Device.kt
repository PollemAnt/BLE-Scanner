package com.example.blescanner

import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

object Device {
    val device: HashMap<String, String> = HashMap()
    var address :String? = null
    var name :String? = null
    val services: MutableList<BluetoothGattService> = mutableListOf()
    val characteristics: MutableList<BluetoothGattCharacteristic> = mutableListOf()
    var bluetoothGatt: BluetoothGatt? = null
    var buttonState :Boolean = true
    var activity : Activity? = null
}