package com.example.blescanner

import android.os.ParcelUuid
import java.util.*

object Constants {
    val BLINKY_SERVICE_UUID: UUID = UUID.fromString("de8a5aac-a99b-c315-0c80-60d4cbb51224")
    val DIODE_UUID: UUID = UUID.fromString("5b026510-4088-c297-46d8-be6c736a087a")
    val BUTTON_STATE_UUID: UUID = UUID.fromString("61a885a4-41c3-60d0-9a53-6d652a70d29c")
    val MESH_SERVICE_PARCEL_UUID: ParcelUuid =
        ParcelUuid.fromString("00001827-0000-1000-8000-00805f9b34fb")
    val MESH_SILICON_LABS_PARCEL_UUID: ParcelUuid =
        ParcelUuid.fromString("00001828-0000-1000-8000-00805f9b34fb")
    val MESH_SERVICE_UUID: UUID = UUID.fromString("00001827-0000-1000-8000-00805f9b34fb")
    val MESH_PROVISION_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("00002adc-0000-1000-8000-00805f9b34fb")
    val MESH_SILICON_LABS_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("00002ade-0000-1000-8000-00805f9b34fb")
}