package com.example.blescanner.koinlesson

import androidx.fragment.app.Fragment
import org.koin.core.parameter.parametersOf
import org.koin.android.ext.android.inject

class UserSessionManager(userId :String, private val logger: AppLogger) {
}

class UserFragment: Fragment() {
    private val logger: AppLogger by inject()
    private val userSession : UserSessionManager by inject{ parametersOf("user_id")}
}