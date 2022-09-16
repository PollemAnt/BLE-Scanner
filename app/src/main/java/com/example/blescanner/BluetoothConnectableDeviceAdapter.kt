package com.example.blescanner

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.databinding.ItemScanResultBinding
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

    class BluetoothConnectableDeviceViewHolder(private val binding: ItemScanResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bluetoothConnectableDevice: BluetoothConnectableDevice) {
            with(binding) {
                this.result = bluetoothConnectableDevice.scanResult

                imageviewBlinkyIcon.setImageResource(getDeviceIcon(bluetoothConnectableDevice))

                val distance = calculateDistance(result)
                deviceDistance.text = distance.toString()
                imageviewSignalIcon.setImageResource(getSignalIcon(distance))
            }
        }

        private fun getDeviceIcon(bluetoothConnectableDevice: BluetoothConnectableDevice): Int {
            return if (bluetoothConnectableDevice.name?.contains("Blinky") == true)
                R.drawable.image_blinky
            else if (bluetoothConnectableDevice.scanResult.scanRecord?.serviceUuids?.get(0) == Constants.MESH_SERVICE_PARCEL_UUID || bluetoothConnectableDevice.scanResult.scanRecord?.serviceUuids?.get(
                    0
                ) == Constants.MESH_SILICON_LABS_PARCEL_UUID
            )
                R.drawable.image_silicon_labs
            else
                R.drawable.ic_blank
        }

        private fun calculateDistance(scanResult: ScanResult?): Double =
            10.0.pow((-69 - scanResult!!.rssi) / (10.0 * 2.0))

        private fun getSignalIcon(distance: Double) = when (distance) {
            in 0.0..2.0 -> R.drawable.ic_signal_perfect
            in 2.0..4.0 -> R.drawable.ic_signal_strong
            in 4.0..8.0 -> R.drawable.ic_signal_medium
            else -> R.drawable.ic_signal_low
        }

        companion object {
            fun from(parent: ViewGroup): BluetoothConnectableDeviceViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemScanResultBinding.inflate(layoutInflater, parent, false)
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
