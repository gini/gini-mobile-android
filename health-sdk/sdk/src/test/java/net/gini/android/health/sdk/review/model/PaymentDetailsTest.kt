package net.gini.android.health.sdk.review.model

import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

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
    fun `Updates specific extractions with payment detail values for feedback`() {
        // Given
        val specificExtractions = mutableMapOf(
            "payment_recipient" to SpecificExtraction(
                "payment_recipient",
                "John Doe",
                "companyname", null, emptyList()
            ),
            "iban" to SpecificExtraction(
                "iban",
                "DE123456789",
                "iban", null, emptyList()
            ),
            "amount_to_pay" to SpecificExtraction(
                "amount_to_pay",
                "1.11:EUR",
                "amount", null, emptyList()
            ),
            "payment_purpose" to SpecificExtraction(
                "payment_purpose",
                "Testing",
                "text", null, emptyList()
            ),
            "payment_state" to SpecificExtraction(
                "payment_state",
                "Payable",
                "text", null, emptyList()
            )
        )

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99:EUR",
            purpose = "Still testing"
        )

        // When
        val updatedExtractions = specificExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals(paymentDetails.recipient, updatedExtractions["payment_recipient"]?.value)
        assertEquals(paymentDetails.iban, updatedExtractions["iban"]?.value)
        assertEquals(paymentDetails.amount, updatedExtractions["amount_to_pay"]?.value)
        assertEquals(paymentDetails.purpose, updatedExtractions["payment_purpose"]?.value)
        // Verify other extractions are preserved
        assertEquals("Payable", updatedExtractions["payment_state"]?.value)
    }

    @Test
    fun `Adds missing payment extraction when updating payment detail values for feedback`() {
        // Given
        val specificExtractions = mutableMapOf(
            "iban" to SpecificExtraction(
                "iban",
                "DE123456789",
                "iban", null, emptyList()
            ),
            "amount_to_pay" to SpecificExtraction(
                "amount_to_pay",
                "1.11:EUR",
                "amount", null, emptyList()
            ),
            "payment_purpose" to SpecificExtraction(
                "payment_purpose",
                "Testing",
                "text", null, emptyList()
            )
        )

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99:EUR",
            purpose = "Still testing"
        )

        // When
        val updatedExtractions = specificExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals(paymentDetails.recipient, updatedExtractions["payment_recipient"]?.value)
        assertEquals("companyname", updatedExtractions["payment_recipient"]?.entity)
        assertEquals(paymentDetails.iban, updatedExtractions["iban"]?.value)
        assertEquals(paymentDetails.amount, updatedExtractions["amount_to_pay"]?.value)
        assertEquals(paymentDetails.purpose, updatedExtractions["payment_purpose"]?.value)
    }

    @Test
    fun `Converts amount_to_pay to backend format when updating payment detail values for feedback`() {
        // Given
        val specificExtractions = mutableMapOf(
            "amount_to_pay" to SpecificExtraction(
                "amount_to_pay",
                "1.11:EUR",
                "amount", null, emptyList()
            )
        )

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99",
            purpose = "Still testing"
        )

        // When
        val updatedExtractions = specificExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals("9.99:EUR", updatedExtractions["amount_to_pay"]?.value)
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

    @Test
    fun `Preserves extraction metadata when updating with feedback`() {
        // Given
        val box = net.gini.android.core.api.models.Box(1, 100.0, 200.0, 300.0, 400.0)
        val specificExtractions = mutableMapOf(
            "iban" to SpecificExtraction(
                "iban",
                "DE123456789",
                "iban",
                box,
                emptyList()
            )
        )

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99",
            purpose = "Still testing"
        )

        // When
        val updatedExtractions = specificExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals("DE987654321", updatedExtractions["iban"]?.value)
        assertEquals(box, updatedExtractions["iban"]?.box)
        assertEquals("iban", updatedExtractions["iban"]?.entity)
    }

    @Test
    fun `Sets correct entity types for payment extractions`() {
        // Given
        val specificExtractions = mutableMapOf<String, SpecificExtraction>()

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99",
            purpose = "Still testing"
        )

        // When
        val updatedExtractions = specificExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals("companyname", updatedExtractions["payment_recipient"]?.entity)
        assertEquals("iban", updatedExtractions["iban"]?.entity)
        assertEquals("amount", updatedExtractions["amount_to_pay"]?.entity)
        assertEquals("text", updatedExtractions["payment_purpose"]?.entity)
    }

    @Test
    fun `Does not modify original specific extractions map`() {
        // Given
        val specificExtractions = mutableMapOf(
            "iban" to SpecificExtraction(
                "iban",
                "DE123456789",
                "iban", null, emptyList()
            )
        )
        val originalIban = specificExtractions["iban"]?.value

        val paymentDetails = PaymentDetails(
            recipient = "Jack Vance",
            iban = "DE987654321",
            amount = "9.99",
            purpose = "Still testing"
        )

        // When
        val updatedExtractions = specificExtractions.withFeedback(paymentDetails)

        // Then
        assertEquals(originalIban, specificExtractions["iban"]?.value)
        assertEquals("DE987654321", updatedExtractions["iban"]?.value)
    }
}


