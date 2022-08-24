package com.example.blescanner

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.blescanner.databinding.DeviceFragmentBinding

class DeviceDialogFragment : DialogFragment() {
    private var _binding: DeviceFragmentBinding? = null
    private val binding get() = _binding!!

    private var isDiodeOnOnClickValue = false
    private var areListsShown = false

    private val favoriteSharedPref by lazy {
        requireContext().getSharedPreferences("Favorites bluetooth devices", 0)
    }

    //TEMPORARY
    private val meshDialogFragment = MeshDialogFragment()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding =
            DeviceFragmentBinding.inflate(LayoutInflater.from(context))

        setImageBasedOnObserveVariables()
        checkIsFragmentReadyToShow()

        return AlertDialog.Builder(
            requireActivity()
        ).setView(binding.root).create()
    }

    private fun setImageBasedOnObserveVariables() {
        BluetoothService.isDiodeOn.observe(this) { isDiodeOnStartValue ->
            isDiodeOnOnClickValue = isDiodeOnStartValue

            if (isDiodeOnStartValue)
                binding.imageviewLed.setImageResource(R.drawable.ic_led_on)
            else
                binding.imageviewLed.setImageResource(R.drawable.ic_led_off)
        }

        BluetoothService.isButtonPressed.observe(this) { isButtonPressed ->

            if (isButtonPressed)
                binding.imageviewButtonState.setImageResource(R.drawable.ic_button_pressed)
            else
                binding.imageviewButtonState.setImageResource(R.drawable.ic_button_unpressed)
        }
    }

    private fun checkIsFragmentReadyToShow() {
        BluetoothService.isFragmentReadyToShow.observe(this) { isFragmentReadyToShow ->
            if (isFragmentReadyToShow) {
                binding.progressCircular.visibility = View.INVISIBLE
                areListsShown = false
                showContents()
            }
        }
    }

    private fun showContents() {
        setContentsVisible()
        setOnClickListeners()
        binding.apply {
            device.text = getDeviceName() + " " + BluetoothService.selectedDevice!!.address
            imageviewFavorited.setImageResource(getFavoriteIcon())
        }
    }

    private fun setContentsVisible() {
        binding.layoutBluetoothDevice.visibility = View.VISIBLE
    }

    private fun setOnClickListeners() {
        binding.apply {

            imageviewLed.setOnClickListener {
                BluetoothService.diodeControl()
                imageviewLed.setImageResource(getLedIcon())
            }

            imageviewFavorited.setOnClickListener {
                changeFavoriteState()
            }

            buttonShowInfo.setOnClickListener {
                if (areListsShown)
                    hideLists()
                else
                    showLists()
            }

            //TEMPORARY
            buttonGoToMesh.setOnClickListener {
                meshDialogFragment.show(parentFragmentManager, "mesh")
            }
        }
    }

    private fun getLedIcon(): Int {
        return if (isDiodeOnOnClickValue) {
            R.drawable.ic_led_off
        } else
            R.drawable.ic_led_on
    }

    private fun changeFavoriteState() {
        if (!isFavorite())
            addToFavorite(BluetoothService.selectedDevice!!)
        else
            deleteFromFavorite()

        binding.imageviewFavorited.setImageResource(getFavoriteIcon())
    }

    private fun getFavoriteIcon(): Int {
        return if (isFavorite())
            R.drawable.ic_favorited
        else
            R.drawable.ic_unfavorited
    }

    private fun hideLists() {
        binding.apply {
            buttonShowInfo.text = "Show info"
            areListsShown = false
            servicesList.visibility = View.GONE
            characteristicsList.visibility = View.GONE
        }
    }

    private fun showLists() {
        setServicesList()
        setCharacteristicsList()
        areListsShown = true
        binding.apply {
            servicesList.visibility = View.VISIBLE
            characteristicsList.visibility = View.VISIBLE
            buttonShowInfo.text = "Hide info"
        }
    }

    private fun setServicesList() {
        binding.servicesList.text = BluetoothService.listOfServices.toString().drop(1).dropLast(1)
    }

    private fun setCharacteristicsList() {
        binding.characteristicsList.text =
            BluetoothService.listOfCharacteristic.toString().drop(1).dropLast(1)
    }

    private fun getDeviceName(): String {
        return if (BluetoothService.selectedDevice!!.name == null)
            "Unknown"
        else
            BluetoothService.selectedDevice!!.name!!
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








