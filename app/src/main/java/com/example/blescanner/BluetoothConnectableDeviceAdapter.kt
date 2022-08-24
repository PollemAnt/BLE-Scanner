package com.example.blescanner

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.databinding.ScanResultItemBinding
import kotlin.math.pow

class BluetoothConnectableDeviceAdapter(
    val onDeviceClicked: (BluetoothConnectableDevice) -> Unit
) : ListAdapter<BluetoothConnectableDevice, BluetoothConnectableDeviceAdapter.BluetoothConnectableDeviceViewHolder>(
    DiffUtilCallback
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BluetoothConnectableDeviceViewHolder {
        return BluetoothConnectableDeviceViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: BluetoothConnectableDeviceViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
        holder.itemView.setOnClickListener {
            onDeviceClicked(currentItem)
        }
    }

    class BluetoothConnectableDeviceViewHolder(private val binding: ScanResultItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bluetoothConnectableDevice: BluetoothConnectableDevice) {
            with(binding) {
                this.result = bluetoothConnectableDevice.scanResult

                val distance = calculateDistance(result)
                deviceDistance.text = distance.toString()

                signalLogo.setImageResource(getSignalIcon(distance))
                blinkyLogo.setImageResource(getDeviceIcon(bluetoothConnectableDevice.name))
            }
        }

        private fun calculateDistance(scanResult: ScanResult?): Double =
            10.0.pow((-69 - scanResult!!.rssi) / (10.0 * 2.0))

        private fun getSignalIcon(distance: Double) = when (distance) {
            in 0.0..2.0 -> R.drawable.ic_signal_perfect
            in 2.0..4.0 -> R.drawable.ic_signal_strong
            in 4.0..8.0 -> R.drawable.ic_signal_medium
            else -> R.drawable.ic_signal_low
        }

        private fun getDeviceIcon(name: String?): Int {
            return if (name?.contains("Blinky") == true)
                R.drawable.ic_blinky
            else
                R.drawable.ic_device_unknown
        }

        companion object {
            fun from(parent: ViewGroup): BluetoothConnectableDeviceViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ScanResultItemBinding.inflate(layoutInflater, parent, false)
                return BluetoothConnectableDeviceViewHolder(binding)
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
