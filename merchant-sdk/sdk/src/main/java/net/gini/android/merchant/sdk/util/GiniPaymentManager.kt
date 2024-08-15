package net.gini.android.merchant.sdk.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.android.core.api.Resource
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.api.payment.model.toPaymentRequest
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.review.ValidationMessage
import net.gini.android.merchant.sdk.review.validate
import org.slf4j.LoggerFactory

/**
 * Handles the validation of payment details, creation of payment request, sending feedback and emitting open bank event.
 */
internal class GiniPaymentManager(
    val giniMerchant: GiniMerchant?
) {
    private val _paymentValidation = MutableStateFlow<List<ValidationMessage>>(emptyList())
    val paymentValidation: StateFlow<List<ValidationMessage>> = _paymentValidation

    private val _lastFullyValidatedPaymentDetails = MutableStateFlow<PaymentDetails?>(null)
    val lastFullyValidatedPaymentDetails: MutableStateFlow<PaymentDetails?> = _lastFullyValidatedPaymentDetails

    private fun validatePaymentDetails(paymentDetails: PaymentDetails): Boolean {
        val items = paymentDetails.validate()
        _paymentValidation.tryEmit(items)
        _lastFullyValidatedPaymentDetails.tryEmit(paymentDetails)
        return items.isEmpty()
    }

    suspend fun onPayment(paymentProviderApp: PaymentProviderApp?, paymentDetails: PaymentDetails) {
        if (giniMerchant == null) {
            LOG.error("GiniMerchant instance must be set")
            throw Exception("Cannot initiate payment: No GiniMerchant instance set")
        }
        if (paymentProviderApp == null) {
            LOG.error("No selected payment provider app")
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Error(Exception("No selected payment provider app")))
            return
        }

        if (paymentProviderApp.installedPaymentProviderApp == null) {
            LOG.error("Payment provider app not installed")
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Error(Exception("Payment provider app not installed")))
            return
        }

        val valid = validatePaymentDetails(paymentDetails)
        if (valid) {
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Loading)
            giniMerchant.emitSDKEvent(
                try {
                    GiniMerchant.PaymentState.Success(getPaymentRequest(paymentProviderApp, paymentDetails), paymentProviderApp.name)
                } catch (throwable: Throwable) {
                    GiniMerchant.PaymentState.Error(throwable)
                }
            )
        }
    }

    suspend fun getPaymentRequest(paymentProviderApp: PaymentProviderApp?, paymentDetails: PaymentDetails): PaymentRequest {
        if (giniMerchant == null) {
            LOG.error("Cannot create PaymentRequest: No GiniMerchant instance set")
            throw Exception("Cannot create PaymentRequest: No GiniMerchant instance set")
        }
        if (paymentProviderApp == null) {
            LOG.error("Cannot create PaymentRequest: No selected payment provider app")
            throw Exception("Cannot create PaymentRequest: No selected payment provider app")
        }

        var paymentRequestId: String? = null

        return when (val createPaymentRequestResource = giniMerchant.giniHealthAPI.documentManager.createPaymentRequest(
            PaymentRequestInput(
                paymentProvider = paymentProviderApp.paymentProvider.id,
                recipient = paymentDetails.recipient,
                iban = paymentDetails.iban,
                amount = "${paymentDetails.amount.toBackendFormat()}:EUR",
                bic = null,
                purpose = paymentDetails.purpose,
            )
        ).mapSuccess {
            paymentRequestId = it.data
            giniMerchant.giniHealthAPI.documentManager.getPaymentRequest(it.data)
        }) {
            is Resource.Cancelled -> throw Exception("Cancelled")
            is Resource.Error -> throw Exception(createPaymentRequestResource.exception)
            is Resource.Success -> paymentRequestId?.let {
                createPaymentRequestResource.data.toPaymentRequest(it)
            } ?: throw Exception("Payment request ID is null")
        }
    }

    fun emitPaymentValidation(paymentValidationList: List<ValidationMessage>) {
        _paymentValidation.tryEmit(paymentValidationList)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GiniPaymentManager::class.java)
    }
}