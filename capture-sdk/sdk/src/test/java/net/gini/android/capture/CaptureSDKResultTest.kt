package net.gini.android.capture

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Test

/**
 * Unit tests for the deprecated `returnReasons` shells of [CaptureSDKResult.Success] and
 * [CaptureSDKResult.SchedulePayment]: the list defaults to empty, the deprecated three-argument
 * constructor is still available and Java callers get a two-argument constructor.
 */
@Suppress("DEPRECATION")
class CaptureSDKResultTest {

    private val specificExtractions =
        mapOf("amountToPay" to mockk<GiniCaptureSpecificExtraction>())
    private val compoundExtractions =
        mapOf("lineItems" to mockk<GiniCaptureCompoundExtraction>())

    @Test
    fun `Success carries the extractions and an empty return reasons list by default`() {
        val result = CaptureSDKResult.Success(specificExtractions, compoundExtractions)

        assertThat(result.specificExtractions).isEqualTo(specificExtractions)
        assertThat(result.compoundExtractions).isEqualTo(compoundExtractions)
        assertThat(result.returnReasons).isEmpty()
    }

    @Test
    fun `SchedulePayment carries the extractions and an empty return reasons list by default`() {
        val result = CaptureSDKResult.SchedulePayment(specificExtractions, compoundExtractions)

        assertThat(result.specificExtractions).isEqualTo(specificExtractions)
        assertThat(result.compoundExtractions).isEqualTo(compoundExtractions)
        assertThat(result.returnReasons).isEmpty()
    }

    @Test
    fun `deprecated three-argument constructors are still available`() {
        val success = CaptureSDKResult.Success(specificExtractions, compoundExtractions, emptyList())
        val schedulePayment =
            CaptureSDKResult.SchedulePayment(specificExtractions, compoundExtractions, emptyList())

        assertThat(success.returnReasons).isEmpty()
        assertThat(schedulePayment.returnReasons).isEmpty()
    }

    @Test
    fun `Java callers get a two-argument constructor`() {
        val successConstructor =
            CaptureSDKResult.Success::class.java.getConstructor(Map::class.java, Map::class.java)
        val schedulePaymentConstructor =
            CaptureSDKResult.SchedulePayment::class.java.getConstructor(Map::class.java, Map::class.java)

        val success = successConstructor.newInstance(specificExtractions, compoundExtractions)
        val schedulePayment =
            schedulePaymentConstructor.newInstance(specificExtractions, compoundExtractions)

        assertThat(success.returnReasons).isEmpty()
        assertThat(schedulePayment.returnReasons).isEmpty()
    }
}
