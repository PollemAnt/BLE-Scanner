package com.example.blescanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.blescanner.databinding.FragmentDeviceBinding

class DeviceFragment : Fragment() {
    private var _binding: FragmentDeviceBinding? = null
    private val binding get() = _binding!!

    private var areListsShown = false

    private val favoriteSharedPref by lazy {
        requireContext().getSharedPreferences("Favorites bluetooth devices", 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentDeviceBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkIsFragmentReadyToShow()
        setImageBasedOnObserveVariables()
        onBackPressed()
    }

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    BluetoothService.onBackPressed()
                }
            })
    }

    private fun setImageBasedOnObserveVariables() {
        BluetoothService.connectedDevice?.isDiodeOn?.observe(viewLifecycleOwner) { isDiodeOnStartValue ->

            if (isDiodeOnStartValue)
                binding.imageviewLed.setImageResource(R.drawable.ic_led_on)
            else
                binding.imageviewLed.setImageResource(R.drawable.ic_led_off)
        }

        BluetoothService.connectedDevice?.isButtonPressed?.observe(viewLifecycleOwner) { isButtonPressed ->
            if (isButtonPressed)
                binding.imageviewButtonState.setImageResource(R.drawable.ic_button_pressed)
            else
                binding.imageviewButtonState.setImageResource(R.drawable.ic_button_unpressed)
        }
    }

    private fun checkIsFragmentReadyToShow() {
        BluetoothService.connectedDevice?.isFragmentReadyToShow?.observe(viewLifecycleOwner) { isFragmentReadyToShow ->
            if (isFragmentReadyToShow) {
                BluetoothService.connectedDevice!!.readDiodeStatus()
                binding.isDeviceConnected.visibility = View.INVISIBLE
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
            device.text = getDeviceName() + " " + BluetoothService.connectedDevice!!.address
            imageviewFavorite.setImageResource(getFavoriteIcon())
        }
    }

    private fun setContentsVisible() {
        binding.layoutBluetoothDevice.visibility = View.VISIBLE
    }

    private fun setOnClickListeners() {
        binding.apply {

            imageviewLed.setOnClickListener {
                BluetoothService.diodeControl()
            }

            imageviewFavorite.setOnClickListener {
                changeFavoriteState()
            }

            buttonDisconnect.setOnClickListener {
                BluetoothService.callDisconnect()
            }

            buttonShowInfo.setOnClickListener {
                if (areListsShown)
                    hideLists()
                else
                    showLists()
            }
        }
    }

    private fun changeFavoriteState() {
        if (!isFavorite())
            addToFavorite(BluetoothService.connectedDevice!!)
        else
            deleteFromFavorite()

        binding.imageviewFavorite.setImageResource(getFavoriteIcon())
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
            remove(BluetoothService.connectedDevice!!.address)
            commit()
        }
        Toast.makeText(requireContext(), "Delete from favorite", Toast.LENGTH_SHORT).show()
    }

    private fun getFavoriteIcon(): Int {
        return if (isFavorite())
            R.drawable.ic_favorited
        else
            R.drawable.ic_unfavorite
    }

    private fun isFavorite(): Boolean {
        return favoriteSharedPref!!.all.contains(BluetoothService.connectedDevice!!.address)
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
        return if (BluetoothService.connectedDevice!!.name == null)
            "Unknown"
        else
            BluetoothService.connectedDevice!!.name!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}








