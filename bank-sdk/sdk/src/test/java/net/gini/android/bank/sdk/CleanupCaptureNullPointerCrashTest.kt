package net.gini.android.bank.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.capture.network.GiniCaptureNetworkService
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for verifying that [GiniBank.cleanupCapture] handles null [GiniCapture] instances gracefully.
 *
 * These tests address a crash scenario where calling [cleanupCapture] throws a [NullPointerException]
 * when the [GiniCapture] singleton has not been initialized or has already been cleaned up.
 *
 * **Scenario:** Auto log off or activity recreation can result in [GiniCapture.sInstance] being null
 * when cleanup is attempted.
 *
 * **Expected behavior:** [cleanupCapture] should return gracefully without throwing when the
 * singleton is null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CleanupCaptureNullPointerCrashTest {

    /**
     * Verifies that [cleanupCapture] does not crash when called before [setCaptureConfiguration].
     */
    @Test
    fun `cleanupCapture does not crash with NullPointerException when GiniCapture was never initialized`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Call cleanupCapture without ever calling setCaptureConfiguration
        // This should cause a NullPointerException because GiniCapture.sInstance is null
        GiniBank.cleanupCapture(context)
    }

    /**
     * Verifies that calling [cleanupCapture] multiple times does not crash.
     */
    @Test
    fun `cleanupCapture does not crash with NullPointerException when called twice`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // First, set up the capture configuration with a mock network service
        val mockNetworkService = mockk<GiniCaptureNetworkService>(relaxed = true)
        val captureConfiguration = CaptureConfiguration(
            networkService = mockNetworkService
        )
        GiniBank.setCaptureConfiguration(context, captureConfiguration)

        // First cleanup - should work fine
        GiniBank.cleanupCapture(context)

        // Second cleanup - should not crash with NullPointerException
        // because GiniCapture.sInstance is already null
        GiniBank.cleanupCapture(context)
    }
}
