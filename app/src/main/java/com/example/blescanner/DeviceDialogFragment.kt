package com.example.blescanner

import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.blescanner.Device.device
import com.example.blescanner.databinding.DeviceServiceBinding


class DeviceDialogFragment: DialogFragment() {

    private val main = MainActivity()
    private var listServices = mutableListOf<BluetoothGattService>()
    private var led = true
    private var _binding: DeviceServiceBinding? = null
    private val binding get() = _binding!!
    private val sharedPref = Device.activity?.getPreferences(Context.MODE_PRIVATE)


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        _binding = DeviceServiceBinding.inflate(LayoutInflater.from(context))

        binding.device.text = device.values.toString() + " " + device.keys.toString()
        binding.servises.text = Device.services.toString()
        binding.gattCostam.text = Device.characteristics.toString()

        binding.ledButton.setOnClickListener{
            ledControl(Device.services)
        }

        binding.favorite.setImageResource(setIcon())

        binding.favorite.setOnClickListener{

            if(!isFavorite())
                addToFavorite(device)
            else
                deleteFromFavorite()

            Toast.makeText(Device.activity, if(!isFavorite())"Delete from favorite" else "Add to favorite", Toast.LENGTH_LONG).show()
            binding.favorite.setImageResource(setIcon())
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
                Log.v("qwe","Lączenie z: "+gatt.device.name+" "+gatt.device.address)
                device[gatt.device.address]= gatt.device.name
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v("qwe","disconnected from the GATT Server")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("qwe","onServicesDiscovered")
                listServices = gatt?.services as MutableList<BluetoothGattService>
                Log.v("lista","listServices:"+ listServices)
                addInfoToDevice(listServices)
            } else {
                Log.v("qwe", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {

            if(gatt!!.services[3].characteristics[1] == characteristic )
            {
                Device.buttonState = !Device.buttonState
                Log.v("qwe","Device.buttonState: "+ Device.buttonState)
                Device.activity!!.runOnUiThread {
                    Toast.makeText(Device.activity, "Device.buttonState: " + Device.buttonState, Toast.LENGTH_SHORT).show()
                }
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
        Toast.makeText(Device.activity, "Led is "+if(led) "on" else "off", Toast.LENGTH_LONG).show()
        led = !led
    }

    private fun setIcon():Int {
        return if(isFavorite())
            R.drawable.ic_baseline_favorite_24
        else
            R.drawable.ic_baseline_favorite_border_24
    }

    private fun addInfoToDevice(gattServices: List<BluetoothGattService>?) {
        Log.v("qwe","addDeviceServices")
        if (gattServices == null) return
        gattServices.forEach { gattService ->
            val gattCharacteristics = gattService.characteristics
            Device.services.add(gattService)
                gattCharacteristics.forEach { gattCharacteristic ->
                    Device.characteristics.add(gattCharacteristic)
                    }
        }
        Device.activity!!.runOnUiThread {
            Toast.makeText(Device.activity, "Connected", Toast.LENGTH_SHORT).show()
        }

        setNotificationDeviceButtonState()
    }

    private fun clearDevice(){
        Device.services.clear()
        Device.characteristics.clear()
        device.clear()
        Device.address =""
        Device.buttonState = true
        Device.bluetoothGatt = null
    }

    private fun setNotificationDeviceButtonState(){
        Log.v("qwe","setNotificationDeviceButtonState")
        val buttonState = Device.bluetoothGatt!!.services[3].characteristics[1]
        Device.bluetoothGatt!!.setCharacteristicNotification(buttonState,true)

        val descriptor = buttonState.descriptors[0]
        descriptor.value = (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        Device.bluetoothGatt!!.writeDescriptor(descriptor)
    }

    private fun isFavorite():Boolean{
        return sharedPref!!.all.contains(device.keys.toString())
    }

    private fun addToFavorite(device:HashMap<String, String>) {
        with (sharedPref!!.edit()) {
            Log.v("qwe","addToFavorite: "+ device.keys.toString() + device.values.toString())
            putString(device.keys.toString(), device.values.toString())
            commit()
        }
        Log.v("qwe","new ADD sharedPref!!.all: "+ sharedPref.all)
    }

    private fun deleteFromFavorite() {
        Log.v("qwe","deleteFromFavorite: " + device.keys.toString())
        with (sharedPref!!.edit()) {
            remove(device.keys.toString())
            commit()
        }
        Log.v("qwe","new DELETE sharedPref!!.all: "+ sharedPref.all)
    }
}








