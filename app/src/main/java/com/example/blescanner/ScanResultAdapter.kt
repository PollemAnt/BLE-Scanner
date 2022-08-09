package com.example.blescanner

import android.bluetooth.le.ScanResult
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.blescanner.databinding.ScanResultLayoutBinding
import kotlin.math.pow


class ScanResultAdapter(private var list: MutableList<ScanResult>) :
    RecyclerView.Adapter<ScanResultAdapter.ScanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        Log.v("jak", " ScanResultAdapter.onCreateViewHolder()  ")
        return ScanViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        val currentItem = list[position]
        holder.bind(currentItem)
    }

    override fun getItemCount(): Int = list.size

    fun setData(scans: MutableList<ScanResult>) {
        Log.v("jak", " ScanResultAdapter.setData()")
        list = scans
        notifyDataSetChanged()
    }

    class ScanViewHolder(private val binding: ScanResultLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(scanResult: ScanResult) {
            with(binding) {
                this.result = scanResult
                val distance = calculateDistance(scanResult)

                deviceDistance.text = distance.toString()
                val signal = when (distance){
                    in 0.0..2.0 -> R.drawable.ic_network_4_bar
                    in 2.0..4.0 -> R.drawable.ic_network_3_bar
                    in 4.0..8.0 -> R.drawable.ic_network_2_bar
                    else -> R.drawable.ic_network_1_bar
                }

                val blinky = if(!scanResult.device.name.contains("blinky", ignoreCase = true))
                    R.drawable.not_blinky
                else
                    R.drawable.blinky

                signalLogo.setImageResource(signal)
                blinkyLogo.setImageResource(blinky)

                selectButton.setOnClickListener {
                    Device.address = scanResult.device.address
                    Device.name = scanResult.device.name
                    Log.v("qwe", " selectButton.setOnClickListener "+ scanResult.device.address)
                    cardView.setBackgroundColor(Color.parseColor("#bcbcbc"))
                }

            }
        }
        private fun calculateDistance(scanResult: ScanResult): Double =
            10.0.pow((-69 - scanResult.rssi) / (10.0 * 2.0))

        companion object {
            fun from(parent: ViewGroup): ScanViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding =
                    ScanResultLayoutBinding.inflate(layoutInflater, parent, false)
                Log.v("jak2", " ScanViewHolder.from()  ")
                return ScanViewHolder(binding)
            }
        }
    }



}
