package com.example.blescanner.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.blescanner.core.BlinkyApplication
import com.example.blescanner.data.models.BluetoothConnectableDevice

class BluetoothScanner {

    private val bluetoothManager: BluetoothManager
        get() = BlinkyApplication.appContext.getSystemService(BluetoothManager::class.java)

    private val bluetoothAdapter = bluetoothManager?.adapter
    private val bluetoothScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val scanSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()
    } else {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()
    }

    private var scanCallback: ScanCallback? = null

    fun startScan(onDeviceFound: (BluetoothConnectableDevice) -> Unit) {
        if (ContextCompat.checkSelfPermission(BlinkyApplication.appContext, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.let {
                        val device = BluetoothConnectableDevice(it)
                        onDeviceFound(device)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e("BluetoothScanner", "Scan failed: $errorCode")
                }
            }
            bluetoothScanner?.startScan(null, scanSettings, scanCallback)
        }
    }

    fun stopScan() {
        if (ContextCompat.checkSelfPermission(BlinkyApplication.appContext, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            bluetoothScanner?.stopScan(scanCallback)
            scanCallback = null
        }
    }
}
