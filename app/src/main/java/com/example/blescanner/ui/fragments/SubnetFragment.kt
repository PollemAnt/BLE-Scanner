package com.example.blescanner.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.example.blescanner.ui.adapters.NodeRecyclerViewAdapter
import com.example.blescanner.databinding.FragmentSubnetBinding
import com.example.blescanner.viewmodel.SubnetViewModel

class SubnetFragment : Fragment() {

    private var _binding: FragmentSubnetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SubnetViewModel

    private val nodeRecyclerViewAdapter = NodeRecyclerViewAdapter(mutableListOf())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentSubnetBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SubnetViewModel::class.java]

        observeViewModel()
        //showContents()
        viewModel.loadData()
        setupRecyclerScanView()

        BluetoothMeshNetwork.listOfNodes.observe(viewLifecycleOwner) { listOfNodes ->
            nodeRecyclerViewAdapter.setData(listOfNodes)
        }
        onBackPressed()
    }

    private fun observeViewModel() {
        viewModel.nodes.observe(viewLifecycleOwner) { nodeList ->
            nodeRecyclerViewAdapter.setData(nodeList)
        }

        viewModel.subnetName.observe(viewLifecycleOwner) { name ->
            binding.subnetName.text = name
        }
    }

    /*private fun showContents() {
        binding.subnetName.text = BluetoothMeshNetwork.currentSubnet?.name
    }*/

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) { viewModel.onBackPressed() }

    }

    private fun setupRecyclerScanView() {
        binding.nodesRecyclerView.apply {
            adapter = nodeRecyclerViewAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.VERTICAL,
                false
            )
        }
        val animator = binding.nodesRecyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}