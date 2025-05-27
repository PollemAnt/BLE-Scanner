package com.example.blescanner.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.blescanner.R
import com.example.blescanner.viewmodel.BaseDeviceViewModel

abstract class BaseDeviceFragment<T : ViewDataBinding, VM : BaseDeviceViewModel> : Fragment() {

    protected lateinit var binding: T
    protected abstract val layoutRes: Int
    protected abstract val viewModelClass: Class<VM>
    protected lateinit var viewModel: VM

    abstract fun onDeviceReady()
    abstract fun onClickActions()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[viewModelClass]
        observeCommon()
        observeSpecific()
        onBackPressed()
    }

    open fun observeSpecific() {}

    private fun observeCommon() {
        viewModel.isFragmentReadyToShow.observe(viewLifecycleOwner) {
            if (it) {
                binding.root.findViewById<View>(R.id.is_device_connected).visibility = View.GONE
                binding.root.findViewById<View>(R.id.progress_circular).visibility = View.GONE
                onDeviceReady()
                onClickActions()
            }
        }

        viewModel.areListsShown.observe(viewLifecycleOwner) { shown ->
            binding.root.findViewById<View>(R.id.services_list).visibility =
                if (shown) View.VISIBLE else View.GONE
            binding.root.findViewById<View>(R.id.characteristics_list).visibility =
                if (shown) View.VISIBLE else View.GONE
            val btn = binding.root.findViewById<Button>(R.id.button_show_info)
            btn.text = if (shown) "Hide info" else "Show info"
        }
    }

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            viewModel.onBackPressed()
        }
    }
}
