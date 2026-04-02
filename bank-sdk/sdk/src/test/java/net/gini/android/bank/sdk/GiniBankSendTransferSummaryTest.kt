package net.gini.android.bank.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.ProductTag
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
 * Unit tests for [GiniBank.sendTransferSummary] with a [Map] of field names to values.
 *
 * Mirrors the iOS `CXTransferSummaryTests` suite.
 *
 * Covers:
 *  - CX path: fields wrapped under compoundExtractions["crossBorderPayment"] with empty specificExtractions
 *  - SEPA path via map: fields sent as flat specificExtractions
 *  - Typed SEPA overload unchanged (regression)
 *  - No-op when GiniCapture is not initialised
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GiniBankSendTransferSummaryTest {

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
        GiniBank.cleanupCapture(context)
    }

    // region CX path

    @Test
    fun `CX - sendFeedback is called exactly once`() {
        buildGiniBankWith(ProductTag.CxExtractions)

        GiniBank.sendTransferSummary(mapOf("creditorName" to "Acme Ltd", "currency" to "GBP"))

        verify(exactly = 1) { mockNetworkService.sendFeedback(any(), any(), any()) }
    }

    @Test
    fun `CX - flat specificExtractions are empty`() {
        buildGiniBankWith(ProductTag.CxExtractions)
        val specificSlot = slot<Map<String, GiniCaptureSpecificExtraction>>()
        every { mockNetworkService.sendFeedback(capture(specificSlot), any(), any()) } just Runs

        GiniBank.sendTransferSummary(mapOf("creditorName" to "Acme Ltd"))

        assertThat(specificSlot.captured).isEmpty()
    }

    @Test
    fun `CX - all confirmed fields are placed under crossBorderPayment compound extraction`() {
        buildGiniBankWith(ProductTag.CxExtractions)
        val compoundSlot = slot<Map<String, GiniCaptureCompoundExtraction>>()
        every { mockNetworkService.sendFeedback(any(), capture(compoundSlot), any()) } just Runs

        val fields = mapOf(
            "creditorName" to "Acme Ltd",
            "currency" to "GBP",
            "instructedAmount" to "100.00:GBP",
            "creditorCountry" to "GB",
            "creditorAccountNumber" to "GB29NWBK60161331926819"
        )
        GiniBank.sendTransferSummary(fields)

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
        buildGiniBankWith(ProductTag.CxExtractions)
        val compoundSlot = slot<Map<String, GiniCaptureCompoundExtraction>>()
        every { mockNetworkService.sendFeedback(any(), capture(compoundSlot), any()) } just Runs

        GiniBank.sendTransferSummary(mapOf("creditorName" to "Acme Ltd"))

        assertThat(compoundSlot.captured.keys).containsExactly("crossBorderPayment")
    }

    // endregion

    // region SEPA path (via generic Map overload)

    @Test
    fun `SEPA map - fields are sent as flat specificExtractions with correct values and entity types`() {
        buildGiniBankWith(ProductTag.SepaExtractions)
        val specificSlot = slot<Map<String, GiniCaptureSpecificExtraction>>()
        every { mockNetworkService.sendFeedback(capture(specificSlot), any(), any()) } just Runs

        val fields = mapOf(
            "amountToPay" to "950.00:EUR",
            "paymentRecipient" to "Acme GmbH",
            "paymentReference" to "REF-001",
            "paymentPurpose" to "Invoice March",
            "iban" to "DE89370400440532013000",
            "bic" to "COBADEFFXXX",
            "instantPayment" to "false",
        )
        GiniBank.sendTransferSummary(fields)

        val expectedEntities = mapOf(
            "amountToPay" to "amount",
            "paymentRecipient" to "companyname",
            "paymentReference" to "reference",
            "paymentPurpose" to "reference",
            "iban" to "iban",
            "bic" to "bic",
            "instantPayment" to "instantPayment",
        )
        fields.forEach { (name, value) ->
            assertThat(specificSlot.captured[name]?.value).isEqualTo(value)
            assertThat(specificSlot.captured[name]?.entity).isEqualTo(expectedEntities[name])
        }
    }

    @Test
    fun `SEPA map - crossBorderPayment compound extraction is NOT produced`() {
        buildGiniBankWith(ProductTag.SepaExtractions)
        val compoundSlot = slot<Map<String, GiniCaptureCompoundExtraction>>()
        every { mockNetworkService.sendFeedback(any(), capture(compoundSlot), any()) } just Runs

        GiniBank.sendTransferSummary(mapOf("iban" to "DE89370400440532013000"))

        assertThat(compoundSlot.captured.containsKey("crossBorderPayment")).isFalse()
    }

    // endregion

    // region regression — typed SEPA overload unchanged

    @Test
    fun `typed SEPA overload still routes to flat specificExtractions`() {
        buildGiniBankWith(ProductTag.SepaExtractions)
        val specificSlot = slot<Map<String, GiniCaptureSpecificExtraction>>()
        every { mockNetworkService.sendFeedback(capture(specificSlot), any(), any()) } just Runs

        GiniBank.sendTransferSummary(
            paymentRecipient = "Acme GmbH",
            paymentReference = "REF-001",
            paymentPurpose = "Invoice March",
            iban = "DE89370400440532013000",
            bic = "COBADEFFXXX",
            amount = Amount(java.math.BigDecimal("950.00"), AmountCurrency.EUR),
            instantPayment = null
        )

        assertThat(specificSlot.captured["iban"]?.value).isEqualTo("DE89370400440532013000")
        assertThat(specificSlot.captured["paymentRecipient"]?.value).isEqualTo("Acme GmbH")
        assertThat(specificSlot.captured["amountToPay"]?.value).isEqualTo("950.00:EUR")
    }

    @Test
    fun `typed SEPA overload does NOT produce crossBorderPayment compound extraction`() {
        buildGiniBankWith(ProductTag.SepaExtractions)
        val compoundSlot = slot<Map<String, GiniCaptureCompoundExtraction>>()
        every { mockNetworkService.sendFeedback(any(), capture(compoundSlot), any()) } just Runs

        GiniBank.sendTransferSummary(
            paymentRecipient = "Acme GmbH",
            paymentReference = "REF-001",
            paymentPurpose = "Invoice March",
            iban = "DE89370400440532013000",
            bic = "COBADEFFXXX",
            amount = Amount(java.math.BigDecimal("950.00"), AmountCurrency.EUR),
            instantPayment = null
        )

        assertThat(compoundSlot.captured.containsKey("crossBorderPayment")).isFalse()
    }

    // endregion

    // region helpers

    private fun buildGiniBankWith(productTag: ProductTag) {
        val captureConfiguration = CaptureConfiguration(
            networkService = mockNetworkService,
            productTag = productTag
        )
        GiniBank.setCaptureConfiguration(context, captureConfiguration)
    }

    // endregion
}
