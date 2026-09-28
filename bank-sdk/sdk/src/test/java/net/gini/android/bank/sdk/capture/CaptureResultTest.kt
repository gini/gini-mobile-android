package net.gini.android.bank.sdk.capture

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Test

/**
 * Tests the [CaptureSDKResult] to [CaptureResult] mapping — the last hop before a result reaches
 * the hosting app.
 */
class CaptureResultTest {

    private val specificExtractions =
        mapOf("paymentDueDate" to mockk<GiniCaptureSpecificExtraction>())
    private val compoundExtractions =
        mapOf("compound" to mockk<GiniCaptureCompoundExtraction>())

    /**
     * The scheduled payment hand-off must survive the mapping with its
     * extractions intact, and must NOT collapse into [CaptureResult.Success] (which would make
     * the host pay immediately).
     */
    @Test
    fun `SchedulePayment maps to CaptureResult SchedulePayment with extractions intact`() {
        val sdkResult = CaptureSDKResult.SchedulePayment(
            specificExtractions,
            compoundExtractions
        )

        val result = sdkResult.toCaptureResult()

        assertThat(result).isInstanceOf(CaptureResult.SchedulePayment::class.java)
        val schedulePayment = result as CaptureResult.SchedulePayment
        assertThat(schedulePayment.specificExtractions).isEqualTo(specificExtractions)
        assertThat(schedulePayment.compoundExtractions).isEqualTo(compoundExtractions)
        @Suppress("DEPRECATION")
        assertThat(schedulePayment.returnReasons).isEmpty()
    }

    @Test
    fun `Success still maps to CaptureResult Success`() {
        val sdkResult = CaptureSDKResult.Success(
            specificExtractions,
            compoundExtractions
        )

        val result = sdkResult.toCaptureResult()

        assertThat(result).isInstanceOf(CaptureResult.Success::class.java)
        val success = result as CaptureResult.Success
        assertThat(success.specificExtractions).isEqualTo(specificExtractions)
        assertThat(success.compoundExtractions).isEqualTo(compoundExtractions)
        @Suppress("DEPRECATION")
        assertThat(success.returnReasons).isEmpty()
    }

    @Test
    fun `Cancel maps to CaptureResult Cancel`() {
        assertThat(CaptureSDKResult.Cancel.toCaptureResult()).isEqualTo(CaptureResult.Cancel)
    }

    @Test
    fun `Empty maps to CaptureResult Empty`() {
        assertThat(CaptureSDKResult.Empty.toCaptureResult()).isEqualTo(CaptureResult.Empty)
    }

    @Test
    fun `EnterManually maps to CaptureResult EnterManually`() {
        assertThat(CaptureSDKResult.EnterManually.toCaptureResult())
            .isEqualTo(CaptureResult.EnterManually)
    }
}
