package com.example.blescanner.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.core.BlinkyApplication.Companion.appContext

class DeviceViewModel : BaseDeviceViewModel() {

    private val _isDiodeOn = MutableLiveData<Boolean>()
    val isDiodeOn: LiveData<Boolean> = _isDiodeOn

    private val _isButtonPressed = MutableLiveData<Boolean>()
    val isButtonPressed: LiveData<Boolean> = _isButtonPressed

    override val prefs: SharedPreferences =
        appContext.getSharedPreferences("Favorites bluetooth devices", Context.MODE_PRIVATE)

    init {
        val device = BluetoothService.getConnectedDevice()

        device?.isDiodeOn?.observeForever {
            _isDiodeOn.postValue(it)
        }

        device?.isButtonPressed?.observeForever {
            _isButtonPressed.postValue(it)
        }
    }

    fun diodeControl() {
        BluetoothService.diodeControl()
    }

    override fun onBackPressed() {
        BluetoothService.disconnect()
    }

    override fun onCleared() {
        BluetoothService.getConnectedDevice()?.isDiodeOn?.removeObserver { }
        BluetoothService.getConnectedDevice()?.isButtonPressed?.removeObserver { }
        super.onCleared()
    }
}
