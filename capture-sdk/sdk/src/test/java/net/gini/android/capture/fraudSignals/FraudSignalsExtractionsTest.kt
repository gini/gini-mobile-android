package net.gini.android.capture.fraudSignals

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import org.junit.Test

class FraudSignalsExtractionsTest {

    private val lineItems = GiniCaptureCompoundExtraction("lineItems", emptyList())
    private val fraudSignals =
        GiniCaptureCompoundExtraction(FraudSignalsExtractions.FRAUD_SIGNALS_KEY, emptyList())

    @Test
    fun `removes fraudSignals and keeps the other compound extractions`() {
        val result = FraudSignalsExtractions.withoutFraudSignals(
            mapOf("lineItems" to lineItems, "fraudSignals" to fraudSignals)
        )

        assertThat(result).containsExactly("lineItems", lineItems)
    }

    @Test
    fun `returns an equal map when there are no fraudSignals`() {
        val input = mapOf("lineItems" to lineItems)

        assertThat(FraudSignalsExtractions.withoutFraudSignals(input)).isEqualTo(input)
    }

    @Test
    fun `returns a mutable map even when zero or one entries are left`() {
        val noneLeft = FraudSignalsExtractions.withoutFraudSignals(mapOf("fraudSignals" to fraudSignals))
        val oneLeft = FraudSignalsExtractions.withoutFraudSignals(
            mapOf("lineItems" to lineItems, "fraudSignals" to fraudSignals)
        )

        (noneLeft as MutableMap)["skontoDiscounts"] = lineItems
        (oneLeft as MutableMap).remove("lineItems")

        assertThat(noneLeft.keys).containsExactly("skontoDiscounts")
        assertThat(oneLeft).isEmpty()
    }

    @Test
    fun `returns an empty mutable map for a null input`() {
        val result = FraudSignalsExtractions.withoutFraudSignals(null)

        (result as MutableMap)["lineItems"] = lineItems

        assertThat(result.keys).containsExactly("lineItems")
    }

    @Test
    fun `does not change the input map`() {
        val input = mutableMapOf("lineItems" to lineItems, "fraudSignals" to fraudSignals)

        FraudSignalsExtractions.withoutFraudSignals(input)

        assertThat(input.keys).containsExactly("lineItems", "fraudSignals")
    }
}
