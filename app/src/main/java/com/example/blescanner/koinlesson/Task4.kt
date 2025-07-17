package com.example.blescanner.koinlesson

import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import org.koin.core.scope.get
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin

class BluetoothBroadcastReceiver : BroadcastReceiver() {

    private val bluetoothManager: BluetoothManager by lazy{
        getKoin().get()
    }

    override fun onReceive(context: Context, intent: Intent) {}
}

val bluetoothModule = module {
    single { BluetoothManager::class.java }
}

class MyForegroundService  : Service() {

    private val bluetoothManager: BluetoothManager by lazy{
        getKoin().get()
    }

    override fun onBind(p0: Intent?): IBinder? {return null}
}