package com.example.blescanner

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

import com.example.blescanner.databinding.FragmentMeshBinding

class MeshFragment : Fragment() {

    private var _binding: FragmentMeshBinding? = null
    private val binding get() = _binding!!

    private var areListsShown = false

    private val favoriteSharedPrefMeshFragment by lazy {
        requireContext().getSharedPreferences("Favorites mesh devices", 0)
    }

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

        checkIsFragmentReadyToShow()

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    BluetoothService.onBackPressed()
                }
            })
    }

    private fun checkIsFragmentReadyToShow() {
        BluetoothService.selectedDevice!!.isFragmentReadyToShow.observe(viewLifecycleOwner) { isFragmentReadyToShow ->
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
            imageviewFavorite.setImageResource(getFavoriteIcon())
        }
    }

    private fun setContentsVisible() {
        binding.layoutBluetoothDevice.visibility = View.VISIBLE
    }

    private fun setOnClickListeners() {
        binding.apply {

            imageviewFavorite.setOnClickListener {
                changeFavoriteState()
            }

            buttonShowInfo.setOnClickListener {
                if (areListsShown)
                    hideLists()
                else
                    showLists()
            }

            buttonTest.setOnClickListener {
                BluetoothService.test()
            }
        }
    }

    private fun changeFavoriteState() {
        if (!isFavorite())
            addToFavorite(BluetoothService.selectedDevice!!)
        else
            deleteFromFavorite()

        binding.imageviewFavorite.setImageResource(getFavoriteIcon())
    }

    private fun addToFavorite(bluetoothDevice: BluetoothConnectableDevice) {
        with(favoriteSharedPrefMeshFragment!!.edit()) {
            putString(bluetoothDevice.address, bluetoothDevice.name + "no empty")
            commit()
        }
        Toast.makeText(requireContext(), "Add to favorite", Toast.LENGTH_SHORT).show()
    }

    private fun deleteFromFavorite() {
        with(favoriteSharedPrefMeshFragment!!.edit()) {
            remove(BluetoothService.selectedDevice!!.address)
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
        return favoriteSharedPrefMeshFragment!!.all.contains(BluetoothService.selectedDevice!!.address)
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
            "Mesh"
        else
            BluetoothService.selectedDevice!!.name!!
    }

    override fun onDestroyView() {
        Log.v("qwe", "Destroy -> disconnect")
        super.onDestroyView()
        _binding = null
        BluetoothService.disconnectOnFragmentDestroy()
    }
}