package com.example.blescanner.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.blescanner.bluetooth.BluetoothMeshNetwork
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node

class SubnetViewModel : ViewModel() {

    val nodes: LiveData<List<Node>> = BluetoothMeshNetwork.listOfNodes

    private val _subnetName = MutableLiveData<String>()
    val subnetName: LiveData<String> = _subnetName

    fun loadData() {
        BluetoothMeshNetwork.getNodesList()
        _subnetName.value = BluetoothMeshNetwork.currentSubnet?.name ?: "Unnamed Subnet"
    }

    fun onBackPressed(){
        BluetoothMeshNetwork.onBackPressed()
    }
}