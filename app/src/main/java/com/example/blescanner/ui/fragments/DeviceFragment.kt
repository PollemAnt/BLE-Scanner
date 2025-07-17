package com.example.blescanner.ui.fragments

import android.view.View
import android.widget.Toast
import com.example.blescanner.R
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.databinding.FragmentDeviceBinding
import com.example.blescanner.viewmodel.BaseDeviceViewModel
import com.example.blescanner.viewmodel.DeviceViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeviceFragment : BaseDeviceFragment<FragmentDeviceBinding, DeviceViewModel>() {
    override val layoutRes = R.layout.fragment_device
    override val viewModelClass = DeviceViewModel::class.java

    private val viewModel: DeviceViewModel by viewModel()

    override fun getViewModel(): BaseDeviceViewModel = viewModel

    override fun onDeviceReady() {
        binding.layoutBluetoothDevice.visibility = View.VISIBLE
        binding.device.text = BluetoothService.getConnectedDevice()?.let {
            "${it.name ?: "Unknown"} ${it.address}"
        }

        binding.imageviewFavorite.setImageResource(getFavoriteIcon())
    }

    override fun onClickActions() {
        binding.apply {
            imageviewLed.setOnClickListener { viewModel.diodeControl() }

            imageviewFavorite.setOnClickListener {
                toggleFavorite()
            }

            buttonShowInfo.setOnClickListener {
                viewModel.toggleListsVisibility()
            }

            buttonDisconnect.setOnClickListener {
                viewModel.callDisconnect()
            }
        }
    }

    override fun observeSpecific() {
        viewModel.isDiodeOn.observe(viewLifecycleOwner) { isOn ->
            binding.imageviewLed.setImageResource(
                if (isOn) R.drawable.ic_led_on else R.drawable.ic_led_off
            )
        }

        viewModel.isButtonPressed.observe(viewLifecycleOwner) { isPressed ->
            binding.imageviewButtonState.setImageResource(
                if (isPressed) R.drawable.ic_button_pressed else R.drawable.ic_button_unpressed
            )
        }
    }

    private fun toggleFavorite() {
        val device = BluetoothService.getConnectedDevice() ?: return
        if (viewModel.isFavorite()) {
            viewModel.deleteFromFavorite()
            toast("Deleted from favorites")
        } else {
            viewModel.addToFavorite(device.address, device.name)
            toast("Added to favorites")
        }
        binding.imageviewFavorite.setImageResource(getFavoriteIcon())
    }

    private fun getFavoriteIcon() = if (viewModel.isFavorite())
        R.drawable.ic_favorited else R.drawable.ic_unfavorite

    private fun toast(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }
}









