package com.example.blescanner.koinlesson

import androidx.fragment.app.Fragment
import org.koin.android.ext.android.getKoin
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

class DeviceKoinFragment : Fragment() {
    private val viewModel: DeviceKoinViewModel by viewModel{
        parametersOf("Device_1")
    }
    private val deviceScope = getKoin().createScope("device_scope", named<DeviceKoinFragment>())

    private val deviceManager : DeviceManager by deviceScope.inject()

    override fun onDestroyView() {
        super.onDestroyView()
        deviceScope.close()
    }
}