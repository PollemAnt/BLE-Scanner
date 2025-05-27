package com.example.blescanner.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.blescanner.R
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.databinding.ActivityMainBinding
import com.example.blescanner.ui.navigation.MainNavigator
import com.example.blescanner.utils.PermissionHelper.handleLocationPermissionsResult
import com.example.blescanner.utils.PermissionHelper.hasPermission
import com.example.blescanner.utils.PermissionHelper.isLocationEnabled
import com.example.blescanner.utils.PermissionHelper.requestBluetoothEnable
import com.example.blescanner.utils.PermissionHelper.requestBluetoothPermissions
import com.example.blescanner.utils.PermissionHelper.requestLocationEnable
import com.example.blescanner.utils.PermissionHelper.requestLocationPermissions
import com.siliconlab.bluetoothmesh.adk.BluetoothMesh
import com.siliconlab.bluetoothmesh.adk.configuration.BluetoothMeshConfiguration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mainNavigator: MainNavigator

    private var isScanning = false
        set(value) {
            field = value
            binding.buttonStartStopScan.text =
                if (value) resources.getString(R.string.stop_scan) else resources.getString(R.string.start_scan)
        }
    private var deviceType: String? = ""

    private val isLocationPermissionGranted
        get() = hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

    private val isCoarseLocationPermissionGranted
        get() = hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

    private val isBluetoothPermissionGranted
        get() = hasPermission(this, Manifest.permission.BLUETOOTH)

    private val isBluetoothScanPermissionGranted
        get() = hasPermission(this, Manifest.permission.BLUETOOTH_SCAN)

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
        handleLocationPermissionsResult(this, permissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BluetoothMesh.getInstance() == null)
            BluetoothMesh.initialize(this, BluetoothMeshConfiguration())

        setUpViews()
        setBottomNavigationView()
        supportActionBar?.hide()
    }

    private fun setUpViews() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        mainNavigator = MainNavigator(this, binding)
        setContentView(binding.root)
        setIsScanningValue()
        setFragmentContainerView()
        setOnClickListeners()
    }

    private fun setIsScanningValue() {
        BluetoothService.isScanning.observe(this) { value ->
            isScanning = value
        }
    }

    private fun setFragmentContainerView() {
        mainNavigator.showNetworkFragment()
        BluetoothService.deviceType.observe(this) { deviceType ->
            when (deviceType) {
                "Blinky",
                "Mesh" -> {
                    this.deviceType = deviceType
                    navigationShowDeviceFragment()
                }

                "Unknown" -> deviceIsUnknown()
                null -> navigationShowScannerFragment()
            }
        }
        BluetoothMeshNetwork.isSubnetSelected.observe(this) { isSubnetSelected ->
            if (isSubnetSelected)
                mainNavigator.showSubnetFragment()
            else
                mainNavigator.showNetworkFragment()
        }
    }

    private fun navigationShowDeviceFragment() {
        binding.bottomNavigationView.selectedItemId = R.id.Device
    }

    private fun deviceIsUnknown() {
        Toast.makeText(this@MainActivity, "Select Blinky or Mesh device!", Toast.LENGTH_SHORT)
            .show()
        BluetoothService.startScan()
    }

    private fun navigationShowScannerFragment() {
        binding.bottomNavigationView.selectedItemId = R.id.Scanner
    }


    private fun setOnClickListeners() {
        binding.apply {
            buttonStartStopScan.setOnClickListener {
                checkPermissions()
            }
            buttonShowScanner.setOnClickListener {
                navigationShowScannerFragment()
            }
            buttonShowNetwork.setOnClickListener {
                navigationShowDeviceFragment()
            }
        }
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
                    requestLocationEnable(this)

                if (!BluetoothService.isEnabled())
                    requestBluetoothEnable(bluetoothTurnOnRequest)
            }
        } else {
            if (!isLocationPermissionGranted or !isCoarseLocationPermissionGranted) requestLocationPermissions(
                locationPermissionRequest
            )
            else if (!isBluetoothScanPermissionGranted or !isBluetoothPermissionGranted) requestBluetoothPermissions(
                requestBluetoothScanConnectPermissions
            )
        }
    }

    private fun setBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Network ->
                    mainNavigator.showNetworkFragment()

                R.id.Scanner ->
                    mainNavigator.showScannerFragment()

                R.id.Device -> {
                    if (deviceType == "Blinky")
                        mainNavigator.showDeviceFragment()
                    else
                        mainNavigator.showMeshFragment()
                }
            }
            true
        }
    }
}