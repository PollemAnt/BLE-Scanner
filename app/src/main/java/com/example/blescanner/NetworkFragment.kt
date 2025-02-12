package com.example.blescanner

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.databinding.FragmentNetworkBinding
import com.siliconlab.bluetoothmesh.adk.BluetoothMesh
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet


class NetworkFragment : Fragment() {

    private var _binding: FragmentNetworkBinding? = null
    private val binding get() = _binding!!

    private val subnetRecyclerViewAdapter =
        SubnetRecyclerViewAdapter(mutableListOf(), ::onSubnetItemClick)

    private fun onSubnetItemClick(subnet: Subnet) {
        BluetoothMeshNetwork.onSubnetClicked(subnet)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentNetworkBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BluetoothMeshNetwork.getSubnetsList()
        showContents()
        setOnClickListeners()
        checkIsNetworkCreated()
        setupRecyclerScanView()

        BluetoothMeshNetwork.listOfSubnet.observe(viewLifecycleOwner) { listOfSubnet ->
            subnetRecyclerViewAdapter.setData(listOfSubnet)
        }
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

    private fun showContents() {
        if (BluetoothMesh.getInstance().networks.isEmpty())
            showEmptyNetworkFragment()
        else
            showNetworkContents()
    }

    private fun showEmptyNetworkFragment() {
        binding.apply {
            networkPanel.visibility = View.GONE
            addSubnetPanel.visibility = View.GONE
            buttonCreateNetwork.visibility = View.VISIBLE
            networkFragmentTitle.text = "There are no networks!"
        }
    }

    private fun showNetworkContents() {
        binding.apply {
            editTextSubnetName.text = null
            networkFragmentTitle.text = "Network: "
            networkName.text = BluetoothMesh.getInstance().networks.first().name

            networkPanel.visibility = View.VISIBLE
            addSubnetPanel.visibility = View.VISIBLE
            buttonCreateNetwork.visibility = View.GONE
        }
        closeEditText()
    }

    private fun setOnClickListeners() {
        binding.apply {

            buttonCreateNetwork.setOnClickListener {
                BluetoothMeshNetwork.createDefaultStructure()
            }

            imageviewBin.setOnClickListener {
                BluetoothMeshNetwork.removeNetwork()
            }

            buttonAddSubnet.setOnClickListener {
                val subnetName =
                    editTextSubnetName.text.toString().replace("\\n".toRegex(), "").trim()
                BluetoothMeshNetwork.createNewSubnet(subnetName)
                editTextSubnetName.text = null
                hideKeyboard()
            }
        }
    }

    private fun closeEditText() {
        binding.editTextSubnetName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun hideKeyboard() {
        (activity!!.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?)?.apply {
            hideSoftInputFromWindow(binding.editTextSubnetName.windowToken, 0)
        }
    }

    private fun checkIsNetworkCreated() {
        BluetoothMeshNetwork.isNetworkCreated.observe(viewLifecycleOwner) { isNetworkCreated ->
            showContents()
        }
    }

    private fun setupRecyclerScanView() {
        binding.subnetRecyclerView.apply {
            adapter = subnetRecyclerViewAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.VERTICAL,
                false
            )
        }
        val animator = binding.subnetRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}