package com.example.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.BluetoothService.checkActivation
import com.example.blescanner.BluetoothService.clearScan
import com.example.blescanner.BluetoothService.initialize
import com.example.blescanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val deviceDialogFragment = DeviceDialogFragment()
    private lateinit var addressDeviceToConnect: String
    private var isScanning = false
        set(value) {
            field = value
            binding.startStopScanButton.text =
                if (value) resources.getString(R.string.stop_scan) else resources.getString(R.string.start_scan)
        }

    private var isConnected = false
        set(value) {
            field = value
            binding.connect.text =
                if (value) resources.getString(R.string.get_info) else resources.getString(R.string.connect)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initialize()
        checkActivation()
        setUpAdapter()
        binding.apply {
            lifecycleOwner = this@MainActivity
            executePendingBindings()
            startStopScanButton.setOnClickListener {
                checkPermissions()
            }
            recyclerClear.setOnClickListener {
                if (isScanning) {
                    BluetoothService.stopScan()
                    isScanning = false
                }
                clearScan()
            }
            connect.setOnClickListener {
                if (isScanning) {
                    BluetoothService.stopScan()
                    isScanning = false
                }
                if (!isConnected)
                    isConnected = BluetoothService.connect(addressDeviceToConnect)
                else
                    deviceDialogFragment.show(supportFragmentManager, "customDialog")
            }
        }
    }

    private fun checkPermissions() {
        checkActivation()
        if (BluetoothService.isLocationPermissionGranted && BluetoothService.isBluetoothPermissionGranted) {
            if (BluetoothService.isLocationOn && BluetoothService.bluetoothAdapter!!.isEnabled) {
                isScanning = if (!isScanning) {
                    BluetoothService.startScan()
                    true
                } else {
                    BluetoothService.stopScan()
                    false
                }
            } else {
                if (!BluetoothService.isLocationOn) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startForResult.launch(intent)
                }
                if (!BluetoothService.bluetoothAdapter!!.isEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startForResult.launch(intent)
                }
            }
        } else {
            if (!BluetoothService.isLocationPermissionGranted) requestLocationPermission()
            if (!BluetoothService.isBluetoothPermissionGranted) requestBluetoothPermission()
        }
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                Log.v("qwe", "64Intent: $intent")
            }
        }

    private fun setUpAdapter() {
        BluetoothService.scanResultAdapter =
            ScanResultAdapter(BluetoothService.scanResults, ::onScanResultClick)
        setupRecyclerScanView()
    }

    private fun setupRecyclerScanView() {
        binding.scanRecycler.apply {
            adapter = BluetoothService.scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }
        val animator = binding.scanRecycler.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun onScanResultClick(scanResult: ScanResult) {
        BluetoothService.stopScan()
        isScanning = false
        isConnected = false
        setAddressToConnect(scanResult)
    }

    private fun setAddressToConnect(scanResult: ScanResult) {
        addressDeviceToConnect = scanResult.device.address
    }

    private fun requestLocationPermission() {
        val activityResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    "Permission granted!".toast()
                } else {
                    "Permission denied :(".toast()
                }
            }

        activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestBluetoothPermission() {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.v("asd", "${it.key} = ${it.value}")
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            val requestBluetooth =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == RESULT_OK) {
                        "Bluetooth permission granted".toast()
                    } else {
                        "Bluetooth permission denied".toast()
                    }
                }
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private fun String.toast() {
        Toast.makeText(applicationContext, this, Toast.LENGTH_SHORT).show()
    }
}







