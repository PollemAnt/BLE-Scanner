package com.example.blescanner.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.blescanner.bluetooth.BluetoothService
import androidx.core.content.edit

abstract class BaseDeviceViewModel : ViewModel() {

    protected abstract val prefs: SharedPreferences

    private val _isFragmentReadyToShow = MutableLiveData<Boolean>()
    val isFragmentReadyToShow: LiveData<Boolean> get() = _isFragmentReadyToShow

    private val _areListsShown = MutableLiveData<Boolean>()
    val areListsShown: LiveData<Boolean> get() = _areListsShown

    init {
        BluetoothService.getConnectedDevice()?.isFragmentReadyToShow?.observeForever {
            _isFragmentReadyToShow.postValue(it)
        }
    }

    fun toggleListsVisibility() {
        _areListsShown.value = !(_areListsShown.value ?: false)
    }

    open fun callDisconnect() {
        BluetoothService.disconnect()
    }

    open fun onBackPressed() {}

    fun isFavorite(): Boolean {
        return BluetoothService.getConnectedDevice()?.address?.let { prefs.contains(it) } ?: false
    }

    fun addToFavorite(address: String, name: String?) {
        prefs.edit { putString(address, name ?: "Unknown") }
    }

    fun deleteFromFavorite() {
        BluetoothService.getConnectedDevice()?.address?.let {
            prefs.edit { remove(it) }
        }
    }

    override fun onCleared() {
        BluetoothService.getConnectedDevice()?.isFragmentReadyToShow?.removeObserver { }
        super.onCleared()
    }
}

