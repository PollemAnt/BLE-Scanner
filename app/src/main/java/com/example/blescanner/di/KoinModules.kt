package com.example.blescanner.di

import com.example.blescanner.viewmodel.MeshViewModel
import com.example.blescanner.bluetooth.BluetoothService
import com.example.blescanner.koinlesson.ApiClientFactory
import com.example.blescanner.koinlesson.AppLogger
import com.example.blescanner.koinlesson.BleRepo
import com.example.blescanner.koinlesson.DebugLogger
import com.example.blescanner.koinlesson.DeviceKoinFragment
import com.example.blescanner.koinlesson.DeviceKoinViewModel
import com.example.blescanner.koinlesson.DeviceManager
import com.example.blescanner.koinlesson.FileReader
import com.example.blescanner.koinlesson.IBleRepo
import com.example.blescanner.koinlesson.JsonParser
import com.example.blescanner.koinlesson.Logger
import com.example.blescanner.koinlesson.ProdApiClient
import com.example.blescanner.koinlesson.TestApiClient
import com.example.blescanner.koinlesson.TestBleRepo
import com.example.blescanner.koinlesson.UserSessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { BluetoothService }
    viewModel { MeshViewModel(get()) }
}

val lessonModule = module {
    single { AppLogger() }

    factory { DeviceManager(get()) }

    single(named("prod")) { BleRepo(get(), get()) }
    single<IBleRepo>(named("test")) { TestBleRepo() }

    viewModel { (deviceId: String) -> DeviceKoinViewModel(deviceId, get(named("prod"))) }
}

val toolsModule = module {
    single { JsonParser() }
    single { FileReader(androidContext(), get()) }
}

val parametersOf = module{
    single { AppLogger() }

    factory{ (userId: String) -> UserSessionManager(userId, get()) }
}

val scope = module{

    scope<DeviceKoinFragment> {
        scoped { DeviceManager(get()) }
    }
}

val prodTest = module{
    single { ApiClientFactory(get(named("prod"))) }
    single(named("prod")) { ProdApiClient() }
    single(named("test")) { TestApiClient() }
}

val module5 = module{
    single<Logger>{ DebugLogger() }
}


