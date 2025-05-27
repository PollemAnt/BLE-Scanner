package com.example.blescanner.ui.fragments

import android.view.View
import android.widget.Toast
import com.example.blescanner.R
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.databinding.FragmentMeshBinding
import com.example.blescanner.viewmodel.MeshViewModel

class MeshFragment : BaseDeviceFragment<FragmentMeshBinding, MeshViewModel>() {
    override val layoutRes = R.layout.fragment_mesh
    override val viewModelClass = MeshViewModel::class.java

    override fun onDeviceReady() {
        binding.layoutBluetoothDevice.visibility = View.VISIBLE
        viewModel.updateLists()

        binding.device.text = BluetoothService.getConnectedDevice()?.let {
            "${viewModel.deviceName.value ?: "Mesh"} ${it.address}"
        }

        binding.imageviewFavorite.setImageResource(getFavoriteIcon())
    }

    override fun onClickActions() {
        binding.apply {

            imageviewFavorite.setOnClickListener {
                toggleFavoriteState()
            }

            buttonShowInfo.setOnClickListener {
                viewModel.toggleListsVisibility()
            }

            buttonProvision.setOnClickListener {
                viewModel.prepareProvision()
            }

            buttonDisconnect.setOnClickListener {
                viewModel.callDisconnect()
            }
        }
    }

    override fun observeSpecific() {
        viewModel.deviceName.observe(viewLifecycleOwner) { name ->
            val address = BluetoothService.getConnectedDevice()?.address ?: ""
            binding.device.text = (name ?: "Mesh") + " $address"
        }

        viewModel.services.observe(viewLifecycleOwner) { services ->
            binding.servicesList.text = services.joinToString()
        }

        viewModel.characteristics.observe(viewLifecycleOwner) { characteristic ->
            binding.characteristicsList.text =
                characteristic.joinToString()
        }
    }

    private fun toggleFavoriteState() {
        val device = BluetoothService.getConnectedDevice() ?: return
        if (viewModel.isFavorite()) {
            viewModel.deleteFromFavorite()
            Toast.makeText(requireContext(), "Delete from favorite", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.addToFavorite(device.address, device.name)
            Toast.makeText(requireContext(), "Add to favorite", Toast.LENGTH_SHORT).show()
        }

        binding.imageviewFavorite.setImageResource(getFavoriteIcon())
    }

    private fun getFavoriteIcon(): Int {
        return if (viewModel.isFavorite())
            R.drawable.ic_favorited
        else
            R.drawable.ic_unfavorite
    }
}