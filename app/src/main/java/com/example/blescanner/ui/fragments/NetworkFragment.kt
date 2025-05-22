package com.example.blescanner.ui.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.ui.adapters.SubnetRecyclerViewAdapter
import com.example.blescanner.databinding.FragmentNetworkBinding
import com.example.blescanner.viewmodel.NetworkViewModel
import com.siliconlab.bluetoothmesh.adk.BluetoothMesh
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet


class NetworkFragment : Fragment() {

    private var _binding: FragmentNetworkBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NetworkViewModel

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

        viewModel = ViewModelProvider(this)[NetworkViewModel::class.java]

        observeViewModel()
        viewModel.loadData()
        setOnClickListeners()
        checkIsNetworkCreated()
        setupRecyclerScanView()
        onBackPressed()
    }

    private fun observeViewModel() {
        viewModel.subnets.observe(viewLifecycleOwner) { list ->
            subnetRecyclerViewAdapter.setData(list)
        }

        viewModel.networkName.observe(viewLifecycleOwner) { name ->
            binding.networkName.text = name
        }

        viewModel.isNetworkEmpty.observe(viewLifecycleOwner) { isEmpty ->
            if (isEmpty) showEmptyNetworkFragment()
            else showNetworkContents()
        }

        viewModel.isNetworkCreated.observe(viewLifecycleOwner) {
            viewModel.loadData()
        }
    }

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner
        ) {
            viewModel.onBackPressed()
        }
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
                viewModel.createDefaultNetwork()
            }

            imageviewBin.setOnClickListener {
                viewModel.removeNetwork()
            }

            buttonAddSubnet.setOnClickListener {
                val subnetName =
                    editTextSubnetName.text.toString().replace("\\n".toRegex(), "").trim()
                viewModel.createSubnet(subnetName)
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