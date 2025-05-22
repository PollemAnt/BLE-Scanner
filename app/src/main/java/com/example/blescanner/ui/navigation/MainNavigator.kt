package com.example.blescanner.ui.navigation

import android.view.View
import com.example.blescanner.ui.fragments.DeviceFragment
import com.example.blescanner.R
import com.example.blescanner.databinding.ActivityMainBinding
import com.example.blescanner.ui.MainActivity
import com.example.blescanner.ui.fragments.BluetoothDeviceScannerFragment
import com.example.blescanner.ui.fragments.MeshFragment
import com.example.blescanner.ui.fragments.NetworkFragment
import com.example.blescanner.ui.fragments.SubnetFragment

class MainNavigator(private val activity: MainActivity, private val binding: ActivityMainBinding) {

    private val bluetoothDeviceScannerFragment = BluetoothDeviceScannerFragment()
    private val deviceFragment = DeviceFragment()
    private val meshFragment = MeshFragment()
    private val networkFragment = NetworkFragment()
    private val subnetFragment = SubnetFragment()

    fun showNetworkFragment() {
        binding.apply {
            buttonShowNetwork.visibility = View.GONE
            buttonShowScanner.visibility = View.GONE
            buttonStartStopScan.visibility = View.GONE
        }
        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, networkFragment)
            commit()
        }
    }

    fun showSubnetFragment() {
        binding.apply {
            buttonShowNetwork.visibility = View.GONE
            buttonShowScanner.visibility = View.VISIBLE
            buttonShowScanner.text = "Add device"
            buttonStartStopScan.visibility = View.GONE
        }
        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, subnetFragment)
            commit()
        }
    }

    fun showScannerFragment() {
        binding.apply {
            buttonShowScanner.visibility = View.GONE
            buttonStartStopScan.visibility = View.VISIBLE
            buttonShowNetwork.visibility = View.GONE
        }
        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, bluetoothDeviceScannerFragment)
            commit()
        }
    }

    fun showDeviceFragment() {
       binding.apply {
            buttonShowScanner.visibility = View.GONE
            buttonStartStopScan.visibility = View.GONE
            buttonShowNetwork.visibility = View.GONE
        }
        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, deviceFragment)
            commit()
        }
    }

    fun showMeshFragment() {
        binding.apply {
            buttonShowScanner.visibility = View.GONE
            buttonStartStopScan.visibility = View.GONE
            buttonShowNetwork.visibility = View.GONE
        }
        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container_view, meshFragment)
            commit()
        }
    }
}