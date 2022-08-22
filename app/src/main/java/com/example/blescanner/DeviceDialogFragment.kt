package com.example.blescanner

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.blescanner.databinding.DeviceFragmentBinding

class DeviceDialogFragment : DialogFragment() {
    private var _binding: DeviceFragmentBinding? = null
    private val binding get() = _binding!!
    private val visible = 0x00000000
    private val invisible = 0x00000004
    private var isDiodeOnOnClickValue = false
    private var areListsShown = false
    private val favoriteSharedPref by lazy {
        requireContext().getSharedPreferences("Favorites bluetooth devices", 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding =
            DeviceFragmentBinding.inflate(LayoutInflater.from(context))

        BluetoothService.isFragmentReadyToShow.observe(this) { isFragmentReadyToShow ->
            if (isFragmentReadyToShow) {
                binding.progressCircular.visibility = invisible
                areListsShown = false
                showContents()
            }
            BluetoothService.isDiodeOn.observe(this) { isDiodeOnStartValue ->
                isDiodeOnOnClickValue = isDiodeOnStartValue

                if (isDiodeOnStartValue)
                    binding.ledImageview.setImageResource(R.drawable.led_on)
                else
                    binding.ledImageview.setImageResource(R.drawable.led_off)
            }

            BluetoothService.isButtonPressed.observe(this) { isButtonPressed ->

                if (isButtonPressed)
                    binding.buttonImageview.setImageResource(R.drawable.ic_baseline_radio_button_checked_24)
                else
                    binding.buttonImageview.setImageResource(R.drawable.ic_baseline_radio_button_unchecked_24)
            }
        }
        return AlertDialog.Builder(
            requireActivity()
        ).setView(binding.root).create()
    }

    private fun showContents() {
        setContentsVisible()
        binding.apply {
            device.text =
                BluetoothService.selectedDevice!!.name + " " + BluetoothService.selectedDevice!!.address

            showInfoButton.setOnClickListener {
                Log.v("qwe", "areListsShown ON Click:  $areListsShown")
                if (areListsShown) {
                    showInfoButton.text = "Show info"
                    hideLists()
                    areListsShown = !areListsShown
                } else {
                    showServicesList()
                    showCharacteristicsList()
                    showInfoButton.text = "Hide info"
                    areListsShown = !areListsShown
                }
            }

            favoriteImageview.setImageResource(setIcon(favoriteImageview.id))

            ledImageview.setOnClickListener {
                BluetoothService.diodeControl()
                ledImageview.setImageResource(setIcon(ledImageview.id))
            }
            favoriteImageview.setOnClickListener {
                if (!isFavorite())
                    addToFavorite(BluetoothService.selectedDevice!!)
                else
                    deleteFromFavorite()

                favoriteImageview.setImageResource(setIcon(favoriteImageview.id))
            }
        }
    }

    private fun setContentsVisible() {
        binding.apply {
            ledImageview.visibility = visible
            buttonImageview.visibility = visible
            favoriteImageview.visibility = visible
            fragmentTitle.visibility = visible
            device.visibility = visible
            services.visibility = visible
            servicesList.visibility = visible
            characteristics.visibility = visible
            characteristicsList.visibility = visible
            showInfoButton.visibility = visible
        }
    }

    private fun showCharacteristicsList() {
        binding.characteristics.text = "Characteristics: "
        binding.characteristicsList.text =
            BluetoothService.listOfCharacteristic.toString().drop(1).dropLast(1)
    }

    private fun showServicesList() {
        binding.services.text = "Services: "
        binding.servicesList.text = BluetoothService.listOfServices.toString().drop(1).dropLast(1)
    }

    private fun hideLists() {
        binding.apply {
            services.text = ""
            characteristics.text = ""
            servicesList.text = ""
            characteristicsList.text = ""
        }
    }

    private fun setIcon(id: Int): Int {
        return if (id == binding.favoriteImageview.id) {
            if (isFavorite())
                R.drawable.ic_baseline_favorite_24
            else
                R.drawable.ic_baseline_favorite_border_24
        } else
            if (isDiodeOnOnClickValue) {
                R.drawable.led_off
            } else
                R.drawable.led_on
    }

    private fun isFavorite(): Boolean {
        return favoriteSharedPref!!.all.contains(BluetoothService.selectedDevice!!.address)
    }

    private fun addToFavorite(bluetoothDevice: BluetoothConnectableDevice) {
        with(favoriteSharedPref!!.edit()) {
            putString(bluetoothDevice.address, bluetoothDevice.name)
            commit()
        }
        Toast.makeText(requireContext(), "Add to favorite", Toast.LENGTH_SHORT).show()
    }

    private fun deleteFromFavorite() {
        with(favoriteSharedPref!!.edit()) {
            remove(BluetoothService.selectedDevice!!.address)
            commit()
        }
        Toast.makeText(requireContext(), "Delete from favorite", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        Log.v("qwe", "Destroy -> disconnect")
        super.onDestroyView()
        _binding = null
        BluetoothService.disconnectOnFragmentDestroy()
    }
}








