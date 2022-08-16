package com.example.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.BluetoothService.clearScan
import com.example.blescanner.BluetoothService.initialize
import com.example.blescanner.BluetoothService.setDeviceToConnect
import com.example.blescanner.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val deviceDialogFragment = DeviceDialogFragment()
    private var deviceIsSelected = false

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

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private val isBluetoothPermissionGranted
        get() = hasPermission(Manifest.permission.BLUETOOTH)

    private val isCoarseLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    private val isLocationOn
        get() = isLocationEnabled(this)

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                Log.v("permission", "Intent: $intent")
            }
        }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
            }
            else -> {
                Log.v("permission", "locationPermissionRequest: No location access granted.")
            }
        }
    }

    private val requestBluetoothScanConnectPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.v("keys", "${it.key} = ${it.value}")
            }
        }

    val requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                "Bluetooth permission granted".toast()
            } else {
                "Bluetooth permission denied".toast()
            }
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
                if (deviceIsSelected) {
                    if (isScanning) {
                        BluetoothService.stopScan()
                        isScanning = false
                    }
                    if (!isConnected) {
                        BluetoothService.connect()
                        isConnected = true
                    } else
                        deviceDialogFragment.show(supportFragmentManager, "customDialog")
                } else {
                    Toast.makeText(
                        BlinkyApplication.appContext,
                        "Select device first",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkPermissions() {
        checkActivation()
        if (isLocationPermissionGranted && isBluetoothPermissionGranted && isCoarseLocationPermissionGranted) {
            if (isLocationOn && BluetoothService.bluetoothAdapter!!.isEnabled) {
                isScanning = if (!isScanning) {
                    BluetoothService.startScan()
                    true
                } else {
                    BluetoothService.stopScan()
                    false
                }
            } else {
                if (!isLocationOn) {
                    requestDeviceLocationSettings()
                }
                if (!BluetoothService.bluetoothAdapter!!.isEnabled) {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startForResult.launch(intent)
                }
            }
        } else {
            if (!isLocationPermissionGranted or !isCoarseLocationPermissionGranted) requestLocationPermission()
            if (!isBluetoothPermissionGranted) requestBluetoothPermission()
        }
    }

    private fun checkActivation(): Boolean {
        return checkLocationPermission() && checkBluetoothPermission()
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun requestDeviceLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@MainActivity, 100)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.v("permission", "requestDeviceLocationSettings:  Ignore the error")
                }
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothScanConnectPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private fun checkBluetoothPermission(): Boolean {
        return when (ContextCompat.checkSelfPermission(
            BlinkyApplication.appContext,
            Manifest.permission.BLUETOOTH
        )) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    private fun checkLocationPermission(): Boolean {
        return when (ContextCompat.checkSelfPermission(
            BlinkyApplication.appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    fun hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(BlinkyApplication.appContext, permissionType) ==
                PackageManager.PERMISSION_GRANTED
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
        deviceIsSelected = true
        setDeviceToConnect(scanResult)
    }

    private fun String.toast() {
        Toast.makeText(applicationContext, this, Toast.LENGTH_SHORT).show()
    }

}







