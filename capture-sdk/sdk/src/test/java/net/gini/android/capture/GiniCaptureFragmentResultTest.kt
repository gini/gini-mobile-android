package net.gini.android.capture

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for how [GiniCaptureFragment] forwards the analysis callbacks to its
 * [GiniCaptureFragmentListener]: the extractions arrive unchanged and the deprecated return
 * reasons are never forwarded.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@Suppress("DEPRECATION")
class GiniCaptureFragmentResultTest {

    private val specificExtractions =
        mutableMapOf("amountToPay" to mockk<GiniCaptureSpecificExtraction>())
    private val compoundExtractions =
        mutableMapOf("lineItems" to mockk<GiniCaptureCompoundExtraction>())

    private lateinit var fragment: GiniCaptureFragment
    private lateinit var listener: RecordingListener

    @Before
    fun setUp() {
        fragment = GiniCaptureFragment.createInstance()
        listener = RecordingListener()
        fragment.setListener(listener)
    }

    @Test
    fun `onExtractionsAvailable finishes with Success and no return reasons`() {
        fragment.onExtractionsAvailable(specificExtractions, compoundExtractions, mutableListOf())

        val result = listener.result as CaptureSDKResult.Success
        assertThat(result.specificExtractions).isEqualTo(specificExtractions)
        assertThat(result.compoundExtractions).isEqualTo(compoundExtractions)
        assertThat(result.returnReasons).isEmpty()
    }

    @Test
    fun `onSchedulePayment finishes with SchedulePayment and no return reasons`() {
        fragment.onSchedulePayment(specificExtractions, compoundExtractions, mutableListOf())

        val result = listener.result as CaptureSDKResult.SchedulePayment
        assertThat(result.specificExtractions).isEqualTo(specificExtractions)
        assertThat(result.compoundExtractions).isEqualTo(compoundExtractions)
        assertThat(result.returnReasons).isEmpty()
    }

    @Test
    fun `onExtractionsAvailable without compound extractions finishes with Success and empty maps`() {
        fragment.onExtractionsAvailable(specificExtractions)

        val result = listener.result as CaptureSDKResult.Success
        assertThat(result.specificExtractions).isEqualTo(specificExtractions)
        assertThat(result.compoundExtractions).isEmpty()
        assertThat(result.returnReasons).isEmpty()
    }

    private class RecordingListener : GiniCaptureFragmentListener {
        var result: CaptureSDKResult? = null

        override fun onFinishedWithResult(result: CaptureSDKResult) {
            this.result = result
        }
    }
}
