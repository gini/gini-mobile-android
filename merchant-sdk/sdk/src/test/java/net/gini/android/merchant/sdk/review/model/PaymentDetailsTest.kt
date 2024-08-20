package net.gini.android.merchant.sdk.review.model

import com.google.common.truth.Truth.assertThat
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.overwriteEmptyFields
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
    fun `Overwrites empty fields`() {
        // Given
        val paymentDetails = PaymentDetails(
            recipient = "",
            iban = "iban",
            amount = "30",
            purpose = "payment"
        )

        val nonEmptyPaymentFields = PaymentDetails(
            recipient = "recipient",
            iban = "iban",
            amount = "30",
            purpose = "payment"
        )

        // When
        val newPaymentDetails = paymentDetails.overwriteEmptyFields(nonEmptyPaymentFields)

        // Then
        assertThat(newPaymentDetails.recipient).isEqualTo("recipient")
    }

    @Test
    fun `Keeps original value if field not empty`() {
        // Given
        val paymentDetails = PaymentDetails(
            recipient = "",
            iban = "iban",
            amount = "30",
            purpose = "payment"
        )

        val nonEmptyPaymentFields = PaymentDetails(
            recipient = "recipient",
            iban = "iban2",
            amount = "30",
            purpose = "payment"
        )

        // When
        val newPaymentDetails = paymentDetails.overwriteEmptyFields(nonEmptyPaymentFields)

        // Then
        assertThat(newPaymentDetails.iban).isEqualTo("iban")
    }
}