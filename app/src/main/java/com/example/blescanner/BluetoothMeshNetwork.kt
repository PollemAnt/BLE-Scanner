package com.example.blescanner

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.siliconlab.bluetoothmesh.adk.BluetoothMesh
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.configuration_control.ConfigurationControl
import com.siliconlab.bluetoothmesh.adk.configuration_control.FactoryResetCallback
import com.siliconlab.bluetoothmesh.adk.connectable_device.ConnectableDevice
import com.siliconlab.bluetoothmesh.adk.data_model.network.Network
import com.siliconlab.bluetoothmesh.adk.data_model.network.NetworkCreationException
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetCreationException
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetRemovalCallback
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetRemovalResult
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisionerConfiguration
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisionerConnection
import com.siliconlab.bluetoothmesh.adk.provisioning.ProvisioningCallback

object BluetoothMeshNetwork {

    private var network: Network? = null
    var currentSubnet: Subnet? = null
    private var provisionerConfiguration = ProvisionerConfiguration()

    private val _isNetworkCreated: MutableLiveData<Boolean> = MutableLiveData(false)
    val isNetworkCreated: LiveData<Boolean> = _isNetworkCreated

    private val _isSelectedSubnet: MutableLiveData<Boolean> = MutableLiveData(false)
    val isSubnetSelected: LiveData<Boolean> = _isSelectedSubnet

    private val _listOfSubnet: MutableLiveData<List<Subnet>> =
        MutableLiveData(emptyList())
    val listOfSubnet: LiveData<List<Subnet>> = _listOfSubnet

    private val _listOfNodes: MutableLiveData<List<Node>> =
        MutableLiveData(emptyList())
    val listOfNodes: LiveData<List<Node>> = _listOfNodes

    fun getSubnetsList() {
        val networks = BluetoothMesh.getInstance().networks
        if (networks.isNotEmpty()) {
            network = BluetoothMesh.getInstance().networks.first()
            _listOfSubnet.value = getSubnets()
        }
    }

    private fun getSubnets() = network!!.subnets.toList()

    fun getNodesList() {
        _listOfNodes.value = getNodes()
    }

    private fun getNodes() = currentSubnet?.nodes?.toList()

    fun createDefaultStructure() {
        createDefaultNetwork()
        createDefaultSubnet()
    }

    private fun createDefaultNetwork() {
        val networks = BluetoothMesh.getInstance().networks
        if (networks.isEmpty()) {
            try {
                network = BluetoothMesh.getInstance().createNetwork("Default Network")
            } catch (e: NetworkCreationException) {
                Log.v("qwe", e.toString())
            }
        } else {
            network = networks.iterator().next()
        }
    }

    private fun createDefaultSubnet() {
        val subnets = getSubnets()
        if (subnets.isEmpty()) {
            try {
                currentSubnet = network!!.createSubnet("Default Subnet")
            } catch (e: SubnetCreationException) {
                Log.v("qwe", e.toString())
            }
        } else {
            currentSubnet = subnets.first()
        }
        _listOfSubnet.value = getSubnets()
        _isNetworkCreated.postValue(true)
    }

    fun createNewSubnet(name: String) {
        if (name.isEmpty()) {
            "Write Subnet name".toast()
        } else {
            try {
                network?.createSubnet(name)
            } catch (e: SubnetCreationException) {
                if (network!!.subnets.size >= 4)
                    "Network: ${network!!.name} is full".toast()

                Log.v("qwe", e.toString())
            }
            _listOfSubnet.value = getSubnets()
        }
    }

    fun prepareProvision() {
        provisionerConfiguration.apply {
            isGettingDeviceCompositionData = true
            isEnablingProxy = true
            isKeepingProxyConnection = true
        }
        provision()
    }

    private fun provision() {
        if (currentSubnet != null) {
            val provisionerConnection =
                ProvisionerConnection(BluetoothService.connectedDevice!!, currentSubnet!!)
            provisionerConnection.provisionerOOB
            provisionerConnection.provision(
                provisionerConfiguration,
                null, provisioningCallback
            )
        } else {
            "Select Subnet first".toast()
        }
    }

    private val provisioningCallback = object : ProvisioningCallback {
        override fun success(
            device: ConnectableDevice?,
            subnet: Subnet?,
            node: Node?
        ) {
            "Provision success!".toast()
            _listOfNodes.value = getNodes()
        }

        override fun error(
            device: ConnectableDevice,
            subnet: Subnet,
            error: ErrorType
        ) {
            Log.v("qwe", "ProvisioningCallback error , $subnet : $error")
        }
    }

    fun removeNetwork() {
        network!!.subnets.toList().forEach { subnet ->
            removeSubnet(subnet)
        }
        network!!.removeOnlyFromLocalStructure()
        _isNetworkCreated.postValue(false)
    }

    fun removeSubnet(subnet: Subnet) {
        if (subnet.nodes.size > 0) {
            subnet.nodes.forEach { node ->
                factoryResetDeviceTask(node)
            }
        }
        subnet.removeSubnet(object : SubnetRemovalCallback {
            override fun success(subnet: Subnet?) {
                if (subnet == currentSubnet)
                    currentSubnet = null
                _listOfSubnet.value = getSubnets()
            }

            override fun error(
                subnet: Subnet?,
                result: SubnetRemovalResult?,
                errorType: ErrorType?
            ) {
                Log.v(
                    "qwe",
                    "SubnetRemovalCallback error. result: $result  errorType: $errorType"
                )
            }
        })
    }

    fun removeNode(node: Node) {
        try {
            factoryResetDeviceTask(node)
        } catch (e: SubnetCreationException) {
            Log.v("qwe", e.toString())
        }
    }

    private fun factoryResetDeviceTask(node: Node) {
        val configurationControl = ConfigurationControl(node)
        configurationControl.factoryReset(object : FactoryResetCallback {
            override fun success(node: Node) {
                node.removeOnlyFromLocalStructure()
                _listOfSubnet.value = getSubnets()
                _listOfNodes.value = getNodes()
            }

            override fun error(node: Node, error: ErrorType) {
                Log.v("qwe", "factoryResetDevice ${node.name} $error")
                return
            }
        })
    }

    fun onSubnetClicked(subnet: Subnet) {
        currentSubnet = subnet
        _isSelectedSubnet.value = true
    }

    fun onBackPressed() {
        _isSelectedSubnet.value = false
    }

    private fun String.toast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                BlinkyApplication.appContext,
                this,
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }
}





