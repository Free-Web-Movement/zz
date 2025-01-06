package io.github.freewebmovement.zz

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.freewebmovement.zz.bussiness.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SettingInstrumentedTest {
    @Test
    fun should_test_settings() {
        val app = ApplicationProvider.getApplicationContext<MainApplication>()
        val preference = app.preference
        val setting = Settings(preference)
            assert(setting.localServerPort != 0 && setting.localServerPort > 1024 && setting.localServerPort < 65535)
            var caught = false
            try {
                setting.localServerPort = 100
            } catch (e: Exception) {
                caught = true
            }
            assertEquals(caught, true)
            caught = false
            try {
                setting.localServerPort = 1050
            } catch (e: Exception) {
                caught = true
            }
            assertEquals(caught, false)

            assertEquals(setting.localServerPort, 1050)

            caught = false
            try {
                setting.localServerPort = 0
            } catch (e: Exception) {
                caught = true
            }
            assertEquals(caught, false)
        assert(setting.localServerPort != 0 && setting.localServerPort > 1024 && setting.localServerPort < 65535)
            assertEquals(setting.messagePeriod, 0)
            setting.messagePeriod = 100
            assertEquals(setting.messagePeriod, 100)
            assertEquals(setting.messageTypeSupported, 0)
            setting.messageTypeSupported = 0x11
            assertEquals(setting.messageTypeSupported, 0x11)
            assertEquals(setting.realtimeTypeSupported, 0)
            setting.realtimeTypeSupported = 0x11
            assertEquals(setting.realtimeTypeSupported, 0x11)
    }
}