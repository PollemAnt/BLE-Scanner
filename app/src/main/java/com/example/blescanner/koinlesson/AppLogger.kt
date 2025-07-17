package com.example.blescanner.koinlesson

import android.util.Log
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject

class AppLogger {
    fun log(message: String) {
        Log.d("AppLogger", message)
    }
}

interface Logger
class DebugLogger : Logger

class LoggerFragment : Fragment(){
    private val logger: Logger by inject()
}