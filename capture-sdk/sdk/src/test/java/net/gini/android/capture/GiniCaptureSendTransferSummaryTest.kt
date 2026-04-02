package net.gini.android.capture

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [GiniCapture.sendTransferSummary] with a [Map] of field names to values.
 *
 * Covers:
 *  - CX path: fields wrapped under compoundExtractions["crossBorderPayment"] with empty specificExtractions
 *  - SEPA path: fields sent as flat specificExtractions
 *  - No-op when GiniCapture is not initialised
 *  - The existing typed SEPA overload routes to flat specificExtractions (regression)
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GiniCaptureSendTransferSummaryTest {

    private lateinit var mockNetworkService: GiniCaptureNetworkService
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockNetworkService = mockk()
        every { mockNetworkService.sendFeedback(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    // region CX path

    @Test
    fun `CX - sendFeedback is called exactly once`() {
        buildGiniCaptureWith(ProductTag.CxExtractions)

        GiniCapture.sendTransferSummary(mapOf("creditorName" to "Acme Ltd", "currency" to "GBP"))

        verify(exactly = 1) { mockNetworkService.sendFeedback(any(), any(), any()) }
    }

    @Test
    fun `CX - flat specificExtractions are empty`() {
        buildGiniCaptureWith(ProductTag.CxExtractions)
        val specificSlot = slot<Map<String, GiniCaptureSpecificExtraction>>()
        every { mockNetworkService.sendFeedback(capture(specificSlot), any(), any()) } just Runs

        GiniCapture.sendTransferSummary(mapOf("creditorName" to "Acme Ltd"))

        assertThat(specificSlot.captured).isEmpty()
    }

    @Test
    fun `CX - all confirmed fields are placed under crossBorderPayment compound extraction`() {
        buildGiniCaptureWith(ProductTag.CxExtractions)
        val compoundSlot = slot<Map<String, GiniCaptureCompoundExtraction>>()
        every { mockNetworkService.sendFeedback(any(), capture(compoundSlot), any()) } just Runs

        val fields = mapOf("creditorName" to "Acme Ltd", "currency" to "GBP", "instructedAmount" to "100.00:GBP")
        GiniCapture.sendTransferSummary(fields)

        val cbp = compoundSlot.captured["crossBorderPayment"]
        assertThat(cbp).isNotNull()
        assertThat(cbp!!.specificExtractionMaps).hasSize(1)

        val row = cbp.specificExtractionMaps[0]
        assertThat(row).hasSize(fields.size)
        fields.forEach { (name, value) ->
            assertThat(row[name]?.value).isEqualTo(value)
            assertThat(row[name]?.entity).isEqualTo(name)
        }
    }

    @Test
    fun `CX - no extractions outside of crossBorderPayment are produced`() {
        buildGiniCaptureWith(ProductTag.CxExtractions)
        val compoundSlot = slot<Map<String, GiniCaptureCompoundExtraction>>()
        every { mockNetworkService.sendFeedback(any(), capture(compoundSlot), any()) } just Runs

        GiniCapture.sendTransferSummary(mapOf("creditorName" to "Acme Ltd"))

        assertThat(compoundSlot.captured.keys).containsExactly("crossBorderPayment")
    }

    // endregion

    // region SEPA path (via generic Map overload)

    @Test
    fun `SEPA - sendFeedback is called exactly once`() {
        buildGiniCaptureWith(ProductTag.SepaExtractions)

        GiniCapture.sendTransferSummary(mapOf("iban" to "DE89370400440532013000", "amountToPay" to "950.00:EUR"))

        verify(exactly = 1) { mockNetworkService.sendFeedback(any(), any(), any()) }
    }

    @Test
    fun `SEPA - fields are sent as flat specificExtractions`() {
        buildGiniCaptureWith(ProductTag.SepaExtractions)
        val specificSlot = slot<Map<String, GiniCaptureSpecificExtraction>>()
        every { mockNetworkService.sendFeedback(capture(specificSlot), any(), any()) } just Runs

        val fields = mapOf("iban" to "DE89370400440532013000", "amountToPay" to "950.00:EUR")
        GiniCapture.sendTransferSummary(fields)

        fields.forEach { (name, value) ->
            assertThat(specificSlot.captured[name]?.value).isEqualTo(value)
        }
    }

    @Test
    fun `SEPA - crossBorderPayment compound extraction is NOT produced`() {
        buildGiniCaptureWith(ProductTag.SepaExtractions)
        val compoundSlot = slot<Map<String, GiniCaptureCompoundExtraction>>()
        every { mockNetworkService.sendFeedback(any(), capture(compoundSlot), any()) } just Runs

        GiniCapture.sendTransferSummary(mapOf("iban" to "DE89370400440532013000"))

        assertThat(compoundSlot.captured.containsKey("crossBorderPayment")).isFalse()
    }

    // endregion

    // region no-op guard

    @Test
    fun `no-op when GiniCapture is not initialised`() {
        GiniCaptureHelper.setGiniCaptureInstance(null)

        // Should not throw
        GiniCapture.sendTransferSummary(mapOf("iban" to "DE89370400440532013000"))

        verify(exactly = 0) { mockNetworkService.sendFeedback(any(), any(), any()) }
    }

    // endregion

    // region regression — typed SEPA overload

    @Test
    fun `typed SEPA overload still routes to flat specificExtractions`() {
        buildGiniCaptureWith(ProductTag.SepaExtractions)
        val specificSlot = slot<Map<String, GiniCaptureSpecificExtraction>>()
        every { mockNetworkService.sendFeedback(capture(specificSlot), any(), any()) } just Runs

        GiniCapture.sendTransferSummary(
            "Acme GmbH",
            "REF-001",
            "Invoice March",
            "DE89370400440532013000",
            "COBADEFFXXX",
            Amount(java.math.BigDecimal("950.00"), AmountCurrency.EUR),
            null
        )

        assertThat(specificSlot.captured["iban"]?.value).isEqualTo("DE89370400440532013000")
        assertThat(specificSlot.captured["paymentRecipient"]?.value).isEqualTo("Acme GmbH")
    }

    // endregion

    // region helpers

    private fun buildGiniCaptureWith(productTag: ProductTag) {
        GiniCapture.newInstance(context)
            .setGiniCaptureNetworkService(mockNetworkService)
            .setProductTag(productTag)
            .build()
    }

    // endregion
}
