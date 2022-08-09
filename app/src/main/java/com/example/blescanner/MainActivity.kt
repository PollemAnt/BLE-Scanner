package com.example.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var bluetoothAdapter: BluetoothAdapter? = null
    private var deviceDialogFragment : DeviceDialogFragment? = null

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        Log.v("qwe", "$bluetoothAdapter")
        Log.v("jak", "initialize 138 ")
        if (bluetoothAdapter == null) {
            Log.v("qwe", "BA jest NULl")
            val bluetoothAdapter: BluetoothAdapter by lazy {
                val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothManager.adapter}
            Log.v("qwe", "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private val isBluetoothPermissionGranted
        get() = hasPermission(Manifest.permission.BLUETOOTH)

    private val isBluetoothOn
        get() = bluetoothAdapter!!.isBluetoothEnabled()

    private val isLocationOn
        get() = isLocationEnabled()

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread {
                binding.bleScan.text =
                    if (value) resources.getString(R.string.stop_scan) else resources.getString(R.string.start_scan)
            }
        }
    private var isConnected = true
        set(value) {
            field = value
            runOnUiThread {
                binding.connect.text =
                    if (value) resources.getString(R.string.connect) else resources.getString(R.string.get_info)
            }
        }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                Log.v("asd", "64Intent: $intent")
            }
        }
    private val bleScanner by lazy {
        bluetoothAdapter!!.bluetoothLeScanner
    }

    private val scanSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()
    } else {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null && ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val indexQuery =
                    scanResults.indexOfFirst { it.device.address == result.device.address }
                if (indexQuery != -1) {
                    scanResults[indexQuery] = result
                    scanResultAdapter.notifyItemChanged(indexQuery)
                } else {
                    scanResults.add(result)
                    scanResults.sortByDescending { it.rssi }
                    val predicate = Predicate { x: ScanResult -> x.device.name == null }
                    removeItems(scanResults, predicate)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.v("asd", "onScanFailed: code $errorCode")
        }
    }

    private val scanResults = mutableListOf<ScanResult>()
    private lateinit var scanResultAdapter: ScanResultAdapter



        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
                Log.v("jak", "onCreate 130 ")
        initialize()
        checkActivation()
        setUpAdapter()
                Log.v("jak", "onCreate 134 ")
            Device.activity = this@MainActivity
        binding.apply {
            lifecycleOwner = this@MainActivity
            executePendingBindings()
            bleScan.setOnClickListener {
                Log.v("jak", "onCreate 140:bleScanner.setOnClickListener ")
                checkPermissions()
            }
            Log.v("jak", " onCreate() 142 ")
            recyclerClear.setOnClickListener {
                Log.v("jak", "onCreate 140: recyclerClear.setOnClickListener ")
                if (isScanning) stopScan()
                scanResultAdapter.setData(mutableListOf())
            }
            connect.setOnClickListener {
                Log.v("qwe", "deviceInfo.setOnClickListener")
                if (isScanning) stopScan()
                deviceDialogFragment = DeviceDialogFragment()
                isConnected = !isConnected
                if(!isConnected)
                deviceDialogFragment!!.connect(Device.address)
                else
                deviceDialogFragment!!.show(supportFragmentManager, "customDialog")
            }
        }


    }

    /*
    fun prepConnect(): Boolean {
        deviceDialogFragment = DeviceDialogFragment()
        Log.v("jak", "deviceDialogFragment "+ deviceDialogFragment)
        val connectStatus = deviceDialogFragment!!.connect(Device.address)
        return connectStatus
    }*/

    private fun checkPermissions() {
        checkActivation()
        Log.v("jak", " checkPermissions() 152 ")
        if (isLocationPermissionGranted && isBluetoothPermissionGranted) {
            if (isLocationOn && isBluetoothOn) {
                startScan()
            } else {
                if (!isLocationOn) activateLocation()
                if (!isBluetoothOn) activateBluetooth()
            }
        } else {
            if (!isLocationPermissionGranted) requestLocationPermission()
            if (!isBluetoothPermissionGranted) requestBluetoothPermission()
        }
    }

    private fun checkActivation(): Boolean {
        Log.v("jak", "checkActivation 167")
        return checkLocationPermission() && checkBluetoothPermission()
    }

    private fun checkBluetoothPermission(): Boolean {
        Log.v("jak", " checkBluetoothPermission() 172 ")
        return when (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        )) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    private fun requestBluetoothPermission() {
        Log.v("jak", " requestBluetoothPermission() 182 ")
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.v("asd", "${it.key} = ${it.value}")
                }
            }

        //ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), BLUETOOTH_REQUEST_CODE)

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

    private fun activateBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startForResult.launch(intent)
    }

    private fun checkLocationPermission(): Boolean {
        Log.v("jak", " checkLocationPermission() 219 ")
        return when (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )) {
            PackageManager.PERMISSION_GRANTED -> true
            else -> false
        }
    }

    private fun requestLocationPermission() {
        Log.v("jak", " requestLocationPermission() 229 ")
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

    private fun activateLocation() {
        Log.v("jak", " activateLocation() 244 ")
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
       // startActivity(locationIntent)

        //val intent = Intent(this, ScanResultAdapter::class.java)
        startActivity(intent)
    }

    private fun setUpAdapter() {
        scanResultAdapter = ScanResultAdapter(scanResults)
        Log.v("jak", "setUpAdapter 251 ")
        Log.v("jak", "scanResults: "+ scanResults)
        setupRecyclerScanView()
    }

    private fun setupRecyclerScanView() {
        binding.scanRecycler.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }
        Log.v("jak", "setupRecyclerScanView 265 ")
        val animator = binding.scanRecycler.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun startScan() {
        if (isScanning) {
            stopScan()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v("jak", " startScan() 304 ")
                scanResults.clear()
                Log.v("jak", "scanResult: "+ scanResults.toString())
                scanResultAdapter.setData(scanResults)
                bleScanner.startScan(null, scanSettings, scanCallback)
                isScanning = true
            }
        }
    }

    private fun stopScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            Log.v("jak", " stopScan() 319 ")
            bleScanner.stopScan(scanCallback)
            isScanning = false
        }
    }

    private fun String.toast() {
        Toast.makeText(applicationContext, this, Toast.LENGTH_SHORT).show()
    }

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Context.isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun BluetoothAdapter.isBluetoothEnabled(): Boolean {
        return this.isEnabled
    }

    private fun <T> removeItems(list: MutableList<T>, predicate: Predicate<T>) {
        val newList: MutableList<T> = ArrayList()
        list.filter { predicate.test(it) }.forEach { newList.add(it) }
        list.removeAll(newList)
    }

}







