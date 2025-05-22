package com.example.blescanner.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.blescanner.data.models.BluetoothConnectableDevice
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.R
import com.example.blescanner.core.BlinkyApplication.Companion.appContext
import com.example.blescanner.databinding.FragmentMeshBinding
import com.example.blescanner.viewmodel.MeshViewModel
import com.example.blescanner.viewmodel.NetworkViewModel

class MeshFragment : Fragment() {

    private var _binding: FragmentMeshBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MeshViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentMeshBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MeshViewModel::class.java]

        if (!viewModel.isDeviceConnected()) {
            binding.progressCircular.visibility = View.INVISIBLE
        }

        observeViewModel()
        onBackPressed()
    }

    private fun observeViewModel() {
        viewModel.isFragmentReadyToShow?.observe(viewLifecycleOwner) { isReady ->
            if (isReady) {
                binding.isDeviceConnected.visibility = View.INVISIBLE
                binding.progressCircular.visibility = View.INVISIBLE
                showContents()
            }
        }

        viewModel.deviceName.observe(viewLifecycleOwner) { name ->
            val address = BluetoothService.connectedDevice?.address ?: ""
            binding.device.text = (name ?: "Mesh") + " $address"
        }

        viewModel.areListsShown.observe(viewLifecycleOwner) {
            if (it) showLists() else hideLists()
        }

        viewModel.services.observe(viewLifecycleOwner) { services ->
            binding.servicesList.text = services.joinToString()
        }

        viewModel.characteristics.observe(viewLifecycleOwner) { characteristic ->
            binding.characteristicsList.text =
                characteristic.joinToString()
        }
    }

    private fun showContents() {
        binding.layoutBluetoothDevice.visibility = View.VISIBLE
        viewModel.updateLists()
        setOnClickListeners()
        binding.imageviewFavorite.setImageResource(getFavoriteIcon())
    }

    private fun setOnClickListeners() {
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

    private fun toggleFavoriteState() {
        val device = BluetoothService.connectedDevice ?: return
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
        return if (isFavorite())
            R.drawable.ic_favorited
        else
            R.drawable.ic_unfavorite
    }

    private fun isFavorite(): Boolean {
        return viewModel.isFavorite()
    }

    private fun hideLists() {
        binding.apply {
            buttonShowInfo.text = "Show info"
            servicesList.visibility = View.GONE
            characteristicsList.visibility = View.GONE
        }
    }

    private fun showLists() {
        binding.apply {
            servicesList.visibility = View.VISIBLE
            characteristicsList.visibility = View.VISIBLE
            buttonShowInfo.text = "Hide info"
        }
    }


    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner
        ) { viewModel.onBackPressed() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}