package com.example.blescanner.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.siliconlab.bluetoothmesh.adk.BluetoothMesh
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet

class NetworkViewModel : ViewModel() {

    val subnets: LiveData<List<Subnet>> = BluetoothMeshNetwork.listOfSubnet
    val isNetworkCreated: LiveData<Boolean> = BluetoothMeshNetwork.isNetworkCreated

    private val _networkName = MutableLiveData<String>()
    val networkName: LiveData<String> = _networkName

    private val _isNetworkEmpty = MutableLiveData<Boolean>()
    val isNetworkEmpty: LiveData<Boolean> = _isNetworkEmpty

    fun loadData() {
        BluetoothMeshNetwork.getSubnetsList()

        val networks = BluetoothMesh.getInstance().networks
        _isNetworkEmpty.value = networks.isEmpty()

        if (networks.isNotEmpty()) {
            _networkName.value = networks.first().name
        }
    }

    fun onBackPressed() {
        BluetoothMeshNetwork.onBackPressed()
    }

    fun createDefaultNetwork() {
        BluetoothMeshNetwork.createDefaultStructure()
    }

    fun removeNetwork() {
        BluetoothMeshNetwork.removeNetwork()
    }

    fun createSubnet(name: String) {
        if (name.isNotBlank()) {
            BluetoothMeshNetwork.createNewSubnet(name)
        }
    }
}