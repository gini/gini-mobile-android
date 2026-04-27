package net.gini.android.bank.sdk.capture

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ProductTag
import net.gini.android.capture.network.GiniCaptureNetworkService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that verify [CaptureConfiguration] properties are correctly forwarded to [GiniCapture]
 * when [GiniBank.setCaptureConfiguration] is called.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CaptureConfigurationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockNetworkService = mockk<GiniCaptureNetworkService>(relaxed = true)

    @After
    fun tearDown() {
        GiniBank.cleanupCapture(context)
    }

    @Test
    fun `productTag defaults to SepaExtractions`() {
        GiniBank.setCaptureConfiguration(
            context,
            CaptureConfiguration(networkService = mockNetworkService)
        )

        assertEquals(ProductTag.SepaExtractions, GiniCapture.getInstance().productTag)
    }

    @Test
    fun `productTag CxExtractions is forwarded to GiniCapture`() {
        GiniBank.setCaptureConfiguration(
            context,
            CaptureConfiguration(
                networkService = mockNetworkService,
                productTag = ProductTag.CxExtractions
            )
        )

        assertEquals(ProductTag.CxExtractions, GiniCapture.getInstance().productTag)
    }
}
