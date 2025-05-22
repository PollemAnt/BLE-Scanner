package com.example.blescanner.core

import android.app.Application
import android.content.Context

class BlinkyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }
}