package net.gini.android.internal.payment.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.gini.android.internal.payment.api.model.PaymentDetails
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class PaymentValidationTest {
    @Test
    fun `emits empty recipient`() = runTest {
        val paymentDetails = PaymentDetails(
            recipient = "",
            amount = "30",
            iban = "1234",
            purpose = "purpose"
        )

        val validation = paymentDetails.validate()

        assertThat(validation).isNotEmpty()
        assertThat(validation).contains(ValidationMessage.Empty(PaymentField.Recipient))
    }

    @Test
    fun `emits empty amount`() = runTest {
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            amount = "",
            iban = "1234",
            purpose = "purpose"
        )

        val validation = paymentDetails.validate()

        assertThat(validation).isNotEmpty()
        assertThat(validation).contains(ValidationMessage.Empty(PaymentField.Amount))
    }

    @Test
    fun `emits invalid amount`() = runTest {
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            amount = "abc",
            iban = "",
            purpose = "purpose"
        )

        val validation = paymentDetails.validate()

        assertThat(validation).isNotEmpty()
        assertThat(validation).contains(ValidationMessage.AmountFormat)
    }

    @Test
    fun `emits empty iban`() = runTest {
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            amount = "2",
            iban = "",
            purpose = "purpose"
        )

        val validation = paymentDetails.validate()

        assertThat(validation).isNotEmpty()
        assertThat(validation).contains(ValidationMessage.Empty(PaymentField.Iban))
    }

    @Test
    fun `validates iban iban`() = runTest {
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            amount = "3",
            iban = "DE91 1000 0000 0123 4567 89",
            purpose = "purpose"
        )

        val validation = paymentDetails.validate()

        assertThat(validation).isEmpty()
    }

    @Test
    fun `emits invalid iban`() = runTest {
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            amount = "3",
            iban = "DE91 ",
            purpose = "purpose"
        )

        val validation = paymentDetails.validate()

        assertThat(validation).isNotEmpty()
        assertThat(validation).contains(ValidationMessage.InvalidIban)
    }
}