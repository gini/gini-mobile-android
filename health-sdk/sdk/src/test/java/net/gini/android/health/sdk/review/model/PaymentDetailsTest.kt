package net.gini.android.health.sdk.review.model

import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.internal.payment.review.reviewFragment.model.PaymentDetails
import net.gini.android.internal.payment.review.reviewFragment.model.getPaymentExtraction
import net.gini.android.internal.payment.review.reviewFragment.model.toPaymentDetails
import net.gini.android.internal.payment.review.reviewFragment.model.withFeedback
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Created by Alpár Szotyori on 01.02.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */
class PaymentDetailsTest {

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `Updates payment extractions with payment detail values for feedback`() {
        // Given
        val compoundExtractions = mutableMapOf("payment" to CompoundExtraction(
            "payment",
            listOf(mapOf(
                "payment_recipient" to SpecificExtraction(
                    "payment_recipient",
                    "John Doe",
                    "", null, emptyList()
                ),
                "iban" to SpecificExtraction(
                    "iban",
                    "DE123456789",
                    "", null, emptyList()
                ),
                "amount_to_pay" to SpecificExtraction(
                    "amount_to_pay",
                    "1.11:EUR",
                    "", null, emptyList()
                ),
                "payment_purpose" to SpecificExtraction(
                    "payment_purpose",
                    "Testing",
                    "", null, emptyList()
                ),
                "payment_state" to SpecificExtraction(
                    "payment_state",
                    "Payable",
                    "", null, emptyList()
                )
            ))
        ))

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99:EUR",
            purpose = "Still testing"
        )

        // When
        compoundExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals(paymentDetails.recipient, compoundExtractions.getPaymentExtraction("payment_recipient")?.value)
        assertEquals(paymentDetails.iban, compoundExtractions.getPaymentExtraction("iban")?.value)
        assertEquals(paymentDetails.amount, compoundExtractions.getPaymentExtraction("amount_to_pay")?.value)
        assertEquals(paymentDetails.purpose, compoundExtractions.getPaymentExtraction("payment_purpose")?.value)
    }

    @Test
    fun `Adds missing payment extraction when updating payment detail values for feedback`() {
        // Given
        val compoundExtractions = mutableMapOf("payment" to CompoundExtraction(
            "payment",
            listOf(mapOf(
                "iban" to SpecificExtraction(
                    "iban",
                    "DE123456789",
                    "", null, emptyList()
                ),
                "amount_to_pay" to SpecificExtraction(
                    "amount_to_pay",
                    "1.11:EUR",
                    "", null, emptyList()
                ),
                "payment_purpose" to SpecificExtraction(
                    "payment_purpose",
                    "Testing",
                    "", null, emptyList()
                )
            ))
        ))

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99:EUR",
            purpose = "Still testing"
        )

        // When
        compoundExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals(paymentDetails.recipient, compoundExtractions.getPaymentExtraction("payment_recipient")?.value)
        assertEquals(paymentDetails.iban, compoundExtractions.getPaymentExtraction("iban")?.value)
        assertEquals(paymentDetails.amount, compoundExtractions.getPaymentExtraction("amount_to_pay")?.value)
        assertEquals(paymentDetails.purpose, compoundExtractions.getPaymentExtraction("payment_purpose")?.value)
    }

    @Test
    fun `Converts amount_to_pay to backend format when updating payment detail values for feedback`() {
        // Given
        val compoundExtractions = mutableMapOf("payment" to CompoundExtraction(
            "payment",
            listOf(mapOf(
                "amount_to_pay" to SpecificExtraction(
                    "amount_to_pay",
                    "1.11:EUR",
                    "", null, emptyList()
                )
            ))
        ))

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99",
            purpose = "Still testing"
        )

        // When
        compoundExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals("9.99:EUR", compoundExtractions.getPaymentExtraction("amount_to_pay")?.value)
    }

    @Test
    fun `Converts amount_to_pay extraction value to number`() {
        // Given
        val extractionsContainer = ExtractionsContainer(
            emptyMap(),
            mutableMapOf("payment" to CompoundExtraction(
                "payment",
                listOf(mapOf(
                    "amount_to_pay" to SpecificExtraction(
                        "amount_to_pay",
                        "1.11:EUR",
                        "", null, emptyList()
                    )
                ))
            ))
        )

        // When
        val paymentDetails = extractionsContainer.toPaymentDetails()

        // Then
        assertEquals("1.11", paymentDetails.amount)
    }
}