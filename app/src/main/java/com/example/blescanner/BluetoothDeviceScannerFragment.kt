package com.example.blescanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.databinding.FragmentRecycleListOfDevicesBinding


class BluetoothDeviceScannerFragment : Fragment() {

    private lateinit var binding: FragmentRecycleListOfDevicesBinding

    private val bluetoothConnectableDeviceAdapter =
        BluetoothConnectableDeviceAdapter(::onBluetoothConnectableDeviceClick)

    private fun onBluetoothConnectableDeviceClick(device: BluetoothConnectableDevice) {
        BluetoothService.stopScan()
        BluetoothService.setDeviceToConnect(device)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            FragmentRecycleListOfDevicesBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerScanView()

        BluetoothService.connectableDevices.observe(viewLifecycleOwner) { scanResults ->
            bluetoothConnectableDeviceAdapter.submitList(scanResults)
        }
        scrollToTopOfRecycleList()
    }

    private fun setupRecyclerScanView() {
        binding.recyclerBluetoothDevices.apply {
            adapter = bluetoothConnectableDeviceAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.VERTICAL,
                false
            )
        }
        val animator = binding.recyclerBluetoothDevices.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun scrollToTopOfRecycleList() {
        bluetoothConnectableDeviceAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                (binding.recyclerBluetoothDevices.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    itemCount - 1,
                    0
                )
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BluetoothService.stopScan()
    }
}