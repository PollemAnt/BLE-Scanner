package com.example.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val deviceFragment = DeviceDialogFragment()
    private val scanResultAdapter =
        ScanResultAdapter(emptyList(), ::onBluetoothConnectableDeviceClick)

    private var isScanning = false
        set(value) {
            field = value
            binding.startStopScanButton.text =
                if (value) resources.getString(R.string.stop_scan) else resources.getString(R.string.start_scan)
        }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private val isCoarseLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    private val isLocationOn
        get() = isLocationEnabled(this)

    private val bluetoothTurnOnRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
            }
            else -> {
                "No location access granted!".toast()
                Log.v("permission", "locationPermissionRequest: No location access granted.")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setupRecyclerScanView()
        setUpViews()

        BluetoothService.connectableDevices.observe(this) { scanResults ->
            scanResultAdapter.setData(scanResults)
        }
        scrollToTopOfRecycleList()
    }

    private fun setUpViews() {
        binding.apply {
            lifecycleOwner = this@MainActivity
            executePendingBindings()
            startStopScanButton.setOnClickListener {
                checkPermissions()
            }
        }
    }

    private fun setupRecyclerScanView() {
        binding.scanRecycler.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
        }
        val animator = binding.scanRecycler.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun scrollToTopOfRecycleList() {
        scanResultAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                (binding.scanRecycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    itemCount - 1,
                    0
                )
            }
        })
    }

    private fun hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissions() {
        if (isLocationPermissionGranted && isCoarseLocationPermissionGranted) {
            if (isLocationOn && BluetoothService.isEnabled()) {
                isScanning = if (!isScanning) {
                    clearScan()
                    BluetoothService.startScan()
                    true
                } else {
                    BluetoothService.stopScan()
                    false
                }
            } else {
                if (!isLocationOn)
                    requestTurnLocationOn()

                if (!BluetoothService.isEnabled())
                    requestTurnBluetoothOn()
            }
        } else {
            if (!isLocationPermissionGranted or !isCoarseLocationPermissionGranted) requestLocationPermission()
        }
    }

    private fun clearScan() {
        scanResultAdapter.setData(mutableListOf())
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun requestTurnBluetoothOn() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothTurnOnRequest.launch(intent)
    }

    private fun requestTurnLocationOn() {
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

    private fun onBluetoothConnectableDeviceClick(device: BluetoothConnectableDevice) {
        BluetoothService.stopScan()
        isScanning = false
        BluetoothService.setDeviceToConnect(device)
        //navigateToAnother fragment
        deviceFragment.show(supportFragmentManager, "DeviceDialogFragment")
    }

    private fun String.toast() {
        Toast.makeText(this@MainActivity, this, Toast.LENGTH_SHORT).show()
    }

}







