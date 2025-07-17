import com.example.blescanner.di.appModule
import com.example.blescanner.koinlesson.DeviceKoinFragment
import com.example.blescanner.koinlesson.DeviceKoinViewModel
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest


class DeviceKoinViewModelTest : KoinTest {

    private val testModule = module {

        single { AppLogger() }
        factory { DeviceManager(get()) }

        factory { (deviceId: String) -> DeviceKoinViewModel(deviceId, get()) }
        single<IBleRepo> { BleRepo() }
    }

    @Before
    fun setUp() {
        startKoin { modules(testModule) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `getUserName returns correct result`() {
        val viewModel: DeviceKoinViewModel by inject(){parametersOf("123")}
        val result = viewModel.getUserName()
        assertEquals("TestUser", result)
    }
}