package com.example.blescanner

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.blescanner.databinding.DeviceServiceBinding


class DeviceDialogFragment: DialogFragment() {

    private val main = MainActivity()
    private var listServices = mutableListOf<BluetoothGattService>()
    private var led = true
    private var isFavorite = false
    private var _binding: DeviceServiceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DeviceServiceBinding.inflate(LayoutInflater.from(context))

        binding.device.text = Device.device.toString()
        binding.servises.text = Device.services.toString()
        binding.gattCostam.text = Device.characteristics.toString()

        binding.ledButton.setOnClickListener{
            ledControl(Device.services)
        }
        binding.favorite.setOnClickListener{
            isFavorite =!isFavorite
            binding.favorite.setImageResource(setIcon())
            Toast.makeText(Device.activity, "Eliminacion fallida.", Toast.LENGTH_LONG).show()
        }
        return AlertDialog.Builder(requireActivity()).setView(binding.root).create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun connect(address: String?): Boolean {
        main.initialize()
        clearDevice()
        main.bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                Log.v("qwe", "connect to the GATT server on the device")
                Device.bluetoothGatt = device.connectGatt(main.baseContext, true, bluetoothGattCallback)
                Log.v("qwe", "connectGatt powinno dzialac")
                return true
            } catch (exception: IllegalArgumentException) {
                Log.v("qwe", "Device not found with provided address.  Unable to connect.")
                return false
            }
        } ?: run {
            Log.v("qwe", "BluetoothAdapter not initialized")
            return false
        }
    }


    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.v("qwe", "Start  onConnectionStateChange ")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v("qwe","successfully connected to the GATT Server")
                val discoverservis = gatt.discoverServices()
                Log.v("qwe", "Czy discoverServices sie udal? = $discoverservis")
                Log.v("qwe","lączenie z: "+gatt.device.name+" "+gatt.device.address)
                Device.device[gatt.device.name]= gatt.device.address
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v("qwe","disconnected from the GATT Server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("qwe","onServicesDiscovered")
                listServices = gatt?.services as MutableList<BluetoothGattService>
                Log.v("lista","listServices:"+ listServices)
                addServicesToDevice(listServices)

            } else {
                Log.v("qwe", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if(gatt!!.services[3].characteristics[1] == characteristic )
            {
                Device.buttonState = !Device.buttonState
                Toast.makeText(Device.activity, "Eliminacion fallida.", Toast.LENGTH_LONG).show()
            }
            super.onCharacteristicChanged(gatt, characteristic)
        }
    }


    private fun ledControl(list: MutableList<BluetoothGattService>){
        Log.v("qwe", "ledControl ")
        val characteristic = list[3].characteristics[0]
        val byteArray = if(led) byteArrayOf(0x01) else byteArrayOf(0x00)
        characteristic.value = byteArray
        Device.bluetoothGatt?.writeCharacteristic(characteristic)
        led = !led
        Toast.makeText(Device.activity, "Eliminacion fallida.", Toast.LENGTH_LONG).show()
    }

    private fun setIcon():Int {
        return if(!isFavorite)
            R.drawable.ic_baseline_favorite_border_24
        else
            R.drawable.ic_baseline_favorite_24
    }

    private fun addServicesToDevice(gattServices: List<BluetoothGattService>?) {
        Log.v("qwe","addDeviceServices")
        if (gattServices == null) return
        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            Device.services.add(gattService)
                gattCharacteristics.forEach { gattCharacteristic ->
                    Device.characteristics.add(gattCharacteristic)
                    }
        }
        setNotificationDeviceButtonState()
    }

    private fun clearDevice(){
        Device.services.clear()
        Device.characteristics.clear()
        Device.device.clear()
        Device.bluetoothGatt = null
    }

    private fun setNotificationDeviceButtonState(){
        val buttonState = Device.bluetoothGatt!!.services[3].characteristics[1]
        Device.bluetoothGatt!!.setCharacteristicNotification(buttonState,true)

        val descriptor = buttonState.descriptors[0]
        descriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        Device.bluetoothGatt!!.writeDescriptor(descriptor)
    }



    fun saveData(device: HashMap<String, String>) {

    }

    fun getData() {

    }
}








