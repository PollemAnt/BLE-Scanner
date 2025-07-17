package com.example.blescanner.core

import android.app.Application
import android.content.Context
import com.example.blescanner.di.appModule
import org.koin.core.context.startKoin

class BlinkyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        startKoin {
            applicationContext
            modules(appModule)
        }
    }

    companion object {
        lateinit var appContext: Context
    }
}