package com.example.blescanner

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.databinding.ScanResultItemBinding
import kotlin.math.pow

class ScanResultAdapter(
    private var scanResult: List<BluetoothConnectableDevice>,
    val onItemClicked: (BluetoothConnectableDevice) -> Unit
) : ListAdapter<BluetoothConnectableDevice, ScanResultAdapter.ScanViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        return ScanViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        val currentItem = scanResult[position]
        holder.bind(currentItem)
        holder.itemView.setOnClickListener {
            onItemClicked(currentItem)
        }
    }

    fun setData(scans: List<BluetoothConnectableDevice>) {
        scanResult = scans
        submitList(scans)
    }

    class ScanViewHolder(private val binding: ScanResultItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bluetoothConnectableDevice: BluetoothConnectableDevice) {
            with(binding) {
                this.result = bluetoothConnectableDevice.scanResult
                val distance = calculateDistance(result)

                deviceDistance.text = distance.toString()
                val signal = when (distance) {
                    in 0.0..2.0 -> R.drawable.ic_network_4_bar
                    in 2.0..4.0 -> R.drawable.ic_network_3_bar
                    in 4.0..8.0 -> R.drawable.ic_network_2_bar
                    else -> R.drawable.ic_network_1_bar
                }
                if (bluetoothConnectableDevice.scanResult.device.name != null) {
                    val blinky =
                        if (!bluetoothConnectableDevice.scanResult.device.name.contains(
                                "blinky",
                                ignoreCase = true
                            )
                        )
                            R.drawable.not_blinky
                        else
                            R.drawable.blinky

                    blinkyLogo.setImageResource(blinky)
                } else
                    blinkyLogo.setImageResource(R.drawable.ic_baseline_device_unknown_24)
                signalLogo.setImageResource(signal)
            }
        }

        private fun calculateDistance(scanResult: ScanResult?): Double =
            10.0.pow((-69 - scanResult!!.rssi) / (10.0 * 2.0))

        companion object {
            fun from(parent: ViewGroup): ScanViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ScanResultItemBinding.inflate(layoutInflater, parent, false)
                return ScanViewHolder(binding)
            }
        }
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<BluetoothConnectableDevice>() {

        override fun areItemsTheSame(
            oldItem: BluetoothConnectableDevice,
            newItem: BluetoothConnectableDevice
        ): Boolean {
            return oldItem.scanResult.device.address == newItem.scanResult.device.address
        }

        override fun areContentsTheSame(
            oldItem: BluetoothConnectableDevice,
            newItem: BluetoothConnectableDevice
        ): Boolean {
            return oldItem.scanResult.rssi == newItem.scanResult.rssi
        }
    }
}
