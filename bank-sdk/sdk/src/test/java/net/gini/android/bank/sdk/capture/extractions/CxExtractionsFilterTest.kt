package net.gini.android.bank.sdk.capture.extractions

import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CxExtractionsFilterTest {

    // region filterForCxExtractions — specific extractions

    @Test
    fun `SEPA specific extractions are removed`() {
        val sepaKeys = listOf("iban", "bic", "amountToPay", "paymentRecipient", "paymentReference",
            "paymentPurpose", "instantPayment")
        val input = successResult(
            specific = sepaKeys.associateWith { makeSpecific(it) }
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertTrue(
            "Expected all SEPA specific extractions to be removed",
            filtered.specificExtractions.isEmpty()
        )
    }

    @Test
    fun `payment hints specific extractions are removed`() {
        val input = successResult(
            specific = mapOf(
                "paymentDueDate" to makeSpecific("paymentDueDate"),
                "creditNote" to makeSpecific("creditNote"),
            )
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertTrue(
            "Expected paymentDueDate and creditNote to be removed",
            filtered.specificExtractions.isEmpty()
        )
    }

    @Test
    fun `non-SEPA specific extractions are kept`() {
        val input = successResult(
            specific = mapOf(
                "epsPaymentQRCodeUrl" to makeSpecific("epsPaymentQRCodeUrl"),
                "someOtherExtraction" to makeSpecific("someOtherExtraction"),
            )
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertEquals(2, filtered.specificExtractions.size)
        assertTrue(filtered.specificExtractions.containsKey("epsPaymentQRCodeUrl"))
        assertTrue(filtered.specificExtractions.containsKey("someOtherExtraction"))
    }

    @Test
    fun `SEPA and non-SEPA specific extractions are mixed - only SEPA removed`() {
        val input = successResult(
            specific = mapOf(
                "iban" to makeSpecific("iban"),
                "amountToPay" to makeSpecific("amountToPay"),
                "epsPaymentQRCodeUrl" to makeSpecific("epsPaymentQRCodeUrl"),
                "paymentDueDate" to makeSpecific("paymentDueDate"),
            )
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertEquals(1, filtered.specificExtractions.size)
        assertTrue(filtered.specificExtractions.containsKey("epsPaymentQRCodeUrl"))
    }

    // endregion

    // region filterForCxExtractions — compound extractions

    @Test
    fun `skontoDiscounts compound extraction is removed`() {
        val input = successResult(
            compound = mapOf("skontoDiscounts" to makeCompound("skontoDiscounts"))
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertFalse(filtered.compoundExtractions.containsKey("skontoDiscounts"))
    }

    @Test
    fun `lineItems compound extraction is removed`() {
        val input = successResult(
            compound = mapOf("lineItems" to makeCompound("lineItems"))
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertFalse(filtered.compoundExtractions.containsKey("lineItems"))
    }

    @Test
    fun `crossBorderPayment compound extraction is kept`() {
        val input = successResult(
            compound = mapOf(
                CxExtractionsFilter.CROSS_BORDER_PAYMENT_KEY to makeCompound(
                    CxExtractionsFilter.CROSS_BORDER_PAYMENT_KEY
                )
            )
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertTrue(filtered.compoundExtractions.containsKey(CxExtractionsFilter.CROSS_BORDER_PAYMENT_KEY))
    }

    @Test
    fun `crossBorderPayment kept while skonto and lineItems are removed`() {
        val input = successResult(
            compound = mapOf(
                "skontoDiscounts" to makeCompound("skontoDiscounts"),
                "lineItems" to makeCompound("lineItems"),
                CxExtractionsFilter.CROSS_BORDER_PAYMENT_KEY to makeCompound(
                    CxExtractionsFilter.CROSS_BORDER_PAYMENT_KEY
                ),
            )
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertEquals(1, filtered.compoundExtractions.size)
        assertTrue(filtered.compoundExtractions.containsKey(CxExtractionsFilter.CROSS_BORDER_PAYMENT_KEY))
    }

    // endregion

    // region filterForCxExtractions — returnReasons preserved

    @Test
    fun `returnReasons are cleared after filtering`() {
        val input = successResult(
            specific = mapOf("iban" to makeSpecific("iban")),
        )

        val filtered = CxExtractionsFilter.filterForCxExtractions(input)

        assertTrue("Expected returnReasons to be empty", filtered.returnReasons.isEmpty())
    }

    // endregion

    // region helpers

    private fun makeSpecific(name: String) =
        GiniCaptureSpecificExtraction(name, "value", "text", null, emptyList())

    private fun makeCompound(name: String) =
        GiniCaptureCompoundExtraction(name, emptyList())

    private fun successResult(
        specific: Map<String, GiniCaptureSpecificExtraction> = emptyMap(),
        compound: Map<String, GiniCaptureCompoundExtraction> = emptyMap(),
    ) = CaptureSDKResult.Success(
        specificExtractions = specific,
        compoundExtractions = compound,
        returnReasons = emptyList(),
    )

    // endregion
}
