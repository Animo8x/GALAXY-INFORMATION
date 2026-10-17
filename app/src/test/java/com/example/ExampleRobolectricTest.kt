package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.HardwareRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name matches Galaxy Info`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Galaxy Info", appName)
    }

    @Test
    fun `verify hardware repository extracts ram and storage safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HardwareRepository(context)

        val ramInfo = repository.getRamInfo()
        assertNotNull(ramInfo)
        assertTrue(ramInfo.totalBytes >= 0)
        assertTrue(ramInfo.usagePercentage in 0f..100f)

        val storageInfo = repository.getStorageInfo()
        assertNotNull(storageInfo)
        assertTrue(storageInfo.totalBytes >= 0)
        assertTrue(storageInfo.usagePercentage in 0f..100f)

        val batteryInfo = repository.getBatteryInfo()
        assertNotNull(batteryInfo)
        assertTrue(batteryInfo.levelPercentage in 0..100)

        // Test formatting helpers
        val formattedGb = HardwareRepository.formatBytes(2L * 1024 * 1024 * 1024)
        assertTrue(formattedGb.contains("GB"))

        val formattedMb = HardwareRepository.formatBytes(500L * 1024 * 1024)
        assertTrue(formattedMb.contains("MB"))
    }
}
