package com.example.blescanner

import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.blescanner.databinding.DeviceServiceBinding


class DeviceDialogFragment : DialogFragment() {

    private var _binding: DeviceServiceBinding? = null
    private val binding get() = _binding!!
    private val sharedPref by lazy {
        requireContext().getSharedPreferences("Favorites bluetooth devices", 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DeviceServiceBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(requireActivity()).setView(binding.root).create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            device.text = bluetoothDevice.name + " " + bluetoothDevice.address
            servicesList.text = listOfServices.toString()
            characteristicsList.text = listOfCharacteristic.toString()
            favorite.setImageResource(setIcon())

            diodeButton.setOnClickListener {
                readDiodeStatus()
            }
            favorite.setOnClickListener {
                if (!isFavorite())
                    addToFavorite(bluetoothDevice)
                else
                    deleteFromFavorite()

                favorite.setImageResource(setIcon())
            }
        }
    }

    private fun setIcon(): Int {
        return if (isFavorite())
            R.drawable.ic_baseline_favorite_24
        else
            R.drawable.ic_baseline_favorite_border_24
    }

    private fun isFavorite(): Boolean {
        return sharedPref!!.all.contains(bluetoothDevice.address)
    }

    private fun addToFavorite(bluetoothDevice: BluetoothDevice) {
        with(sharedPref!!.edit()) {
            putString(bluetoothDevice.address, bluetoothDevice.name)
            commit()
        }
        Toast.makeText(requireContext(), "Add to favorite", Toast.LENGTH_SHORT).show()
    }

    private fun deleteFromFavorite() {
        with(sharedPref!!.edit()) {
            remove(bluetoothDevice.address)
            commit()
        }
        Toast.makeText(requireContext(), "Delete from favorite", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}








