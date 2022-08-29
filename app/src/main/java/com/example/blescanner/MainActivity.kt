package com.example.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.blescanner.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val bluetoothDeviceScannerFragment = BluetoothDeviceScannerFragment()
    private val dialogFragment = DeviceFragment()
    private val meshFragment = MeshFragment()

    private var isScanning = false
        set(value) {
            field = value
            binding.buttonStartStopScan.text =
                if (value) resources.getString(R.string.stop_scan) else resources.getString(R.string.start_scan)
        }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private val isCoarseLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    private val isBluetoothPermissionGranted
        get() = hasPermission(Manifest.permission.BLUETOOTH)

    private val isBluetoothScanPermissionGranted
        get() = hasPermission(Manifest.permission.BLUETOOTH_SCAN)

    private var bluetoothScanPermissionState: Boolean? = null

    private val isLocationOn
        get() = isLocationEnabled(this)

    private val bluetoothTurnOnRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val requestBluetoothScanConnectPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
            }
            else -> {
                Toast.makeText(this@MainActivity, "No location access granted!", Toast.LENGTH_SHORT)
                    .show()
                Log.v("permission", "locationPermissionRequest: No location access granted.")
            }
        }
    }

    private fun hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setUpViews()
    }

    private fun setUpViews() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setIsScanningValue()
        setFragmentContainerView()
        binding.apply {
            buttonStartStopScan.setOnClickListener {
                checkPermissions()
            }
        }
        binding.apply {
            buttonGoBack.setOnClickListener {
                showScannerFragment()
            }
        }
    }

    private fun setIsScanningValue() {
        BluetoothService.isScanning.observe(this) { value ->
            isScanning = value
        }
    }

    private fun setFragmentContainerView() {
        showScannerFragment()
        BluetoothService.typeOfSelectedDevice.observe(this) { deviceType ->
            Log.v("qwe", deviceType.toString())
            when (deviceType) {
                "Blinky" -> showDeviceFragment()
                "Mesh" -> showMeshFragment()
                "Unknown" -> deviceIsUnknown()
                null -> showScannerFragment()
            }
        }
    }

    private fun showScannerFragment() {
        Log.v("qwe", "showScannerFragment")
        binding.buttonGoBack.visibility = View.GONE
        binding.buttonStartStopScan.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, bluetoothDeviceScannerFragment)
            commit()
        }
    }

    private fun showDeviceFragment() {
        binding.buttonGoBack.visibility = View.VISIBLE
        binding.buttonStartStopScan.visibility = View.GONE
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, dialogFragment)
            commit()
        }
    }

    private fun showMeshFragment() {
        binding.buttonGoBack.visibility = View.VISIBLE
        binding.buttonStartStopScan.visibility = View.GONE
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, meshFragment)
            commit()
        }
    }

    private fun deviceIsUnknown() {
        Toast.makeText(this@MainActivity, "Select Blinky or Mesh device!", Toast.LENGTH_SHORT)
            .show()
        BluetoothService.startScan()
    }

    private fun checkPermissions() {

        bluetoothScanPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            isBluetoothScanPermissionGranted
        else
            true

        if (isLocationPermissionGranted && isCoarseLocationPermissionGranted && isBluetoothPermissionGranted && bluetoothScanPermissionState!!) {
            if (isLocationOn && BluetoothService.isEnabled()) {
                isScanning = if (!isScanning) {
                    BluetoothService.clearScanList()
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
            else if (!isBluetoothScanPermissionGranted or !isBluetoothPermissionGranted) requestBluetoothPermission()
        }
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

    private fun requestTurnBluetoothOn() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothTurnOnRequest.launch(intent)
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
        }
    }
}







