package net.gini.android.health.sdk.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.PaymentRequest
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.withFeedback
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.health.sdk.util.toBackendFormat
import net.gini.android.health.sdk.util.withPrev

internal class ReviewViewModel(val giniHealth: GiniHealth, val paymentProviderApp: PaymentProviderApp, val configuration: ReviewConfiguration) : ViewModel() {

    internal var userPreferences: UserPreferences? = null

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    private val _paymentValidation = MutableStateFlow<List<ValidationMessage>>(emptyList())
    val paymentValidation: StateFlow<List<ValidationMessage>> = _paymentValidation

    private val _lastFullyValidatedPaymentDetails = MutableStateFlow<PaymentDetails?>(null)

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    val isPaymentButtonEnabled: Flow<Boolean> =
        combine(giniHealth.openBankState, paymentDetails) { paymentState, paymentDetails ->
            val noEmptyFields = paymentDetails.recipient.isNotEmpty() && paymentDetails.iban.isNotEmpty() &&
                    paymentDetails.amount.isNotEmpty() && paymentDetails.purpose.isNotEmpty()
            val isLoading = (paymentState is GiniHealth.PaymentState.Loading)
            !isLoading && noEmptyFields
        }

    init {
        viewModelScope.launch {
            giniHealth.paymentFlow.collect { extractedPaymentDetails ->
                if (extractedPaymentDetails is ResultWrapper.Success) {
                    val paymentDetails = paymentDetails.value.overwriteEmptyFields(
                        extractedPaymentDetails.value.copy(
                            amount = extractedPaymentDetails.value.amount.adjustToLocalDecimalSeparation()
                        )
                    )
                    _paymentDetails.value = paymentDetails
                }
            }
        }
        viewModelScope.launch {
            delay(SHOW_INFO_BAR_MS)
            _isInfoBarVisible.value = false
        }
        viewModelScope.launch {
            // Validate payment details only if extracted payment details are not being loaded
            _paymentDetails
                .combine(giniHealth.paymentFlow.filter { it !is ResultWrapper.Loading }) { paymentDetails, _ -> paymentDetails }
                .withPrev()
                .collect { (prevPaymentDetails, paymentDetails) ->
                    // Get all validation messages except emptiness validations
                    val nonEmptyValidationMessages = _paymentValidation.value
                        .filter { it !is ValidationMessage.Empty }
                        .toMutableList()

                    // Check payment details for emptiness
                    val newEmptyValidationMessages =
                        paymentDetails.validate().filterIsInstance<ValidationMessage.Empty>()

                    // Clear IBAN error, if IBAN changed
                    if (prevPaymentDetails != null && prevPaymentDetails.iban != paymentDetails.iban) {
                        nonEmptyValidationMessages.remove(ValidationMessage.InvalidIban)
                    }

                    // If the IBAN is the same as the last validated one, then revalidate it to restore the validation
                    // message if needed
                    if (_lastFullyValidatedPaymentDetails.value?.iban == paymentDetails.iban) {
                        nonEmptyValidationMessages.addAll(validateIban(paymentDetails.iban).filterIsInstance<ValidationMessage.InvalidIban>())
                    }

                    // Emit all new empty validation messages along with other existing validation messages
                    _paymentValidation.tryEmit(newEmptyValidationMessages + nonEmptyValidationMessages)
                }
        }
    }

    fun getPages(document: Document): List<DocumentPageAdapter.Page> {
        return (1..document.pageCount).map { pageNumber ->
            DocumentPageAdapter.Page(document.id, pageNumber)
        }
    }

    fun setRecipient(recipient: String) {
        _paymentDetails.value = paymentDetails.value.copy(recipient = recipient)
    }

    fun setIban(iban: String) {
        _paymentDetails.value = paymentDetails.value.copy(iban = iban)
    }

    fun setAmount(amount: String) {
        _paymentDetails.value = paymentDetails.value.copy(amount = amount)
    }

    fun setPurpose(purpose: String) {
        _paymentDetails.value = paymentDetails.value.copy(purpose = purpose)
    }

    private fun validatePaymentDetails(paymentDetails: PaymentDetails): Boolean {
        val items = paymentDetails.validate()
        _paymentValidation.tryEmit(items)
        _lastFullyValidatedPaymentDetails.tryEmit(paymentDetails)
        return items.isEmpty()
    }

    private suspend fun getPaymentRequest(): PaymentRequest {
        return when (val createPaymentRequestResource = giniHealth.giniHealthAPI.documentManager.createPaymentRequest(
            PaymentRequestInput(
                paymentProvider = paymentProviderApp.paymentProvider.id,
                recipient = paymentDetails.value.recipient,
                iban = paymentDetails.value.iban,
                amount = "${paymentDetails.value.amount.toBackendFormat()}:EUR",
                bic = null,
                purpose = paymentDetails.value.purpose,
            )
        )) {
            is Resource.Cancelled -> throw Exception("Cancelled")
            is Resource.Error -> throw Exception(createPaymentRequestResource.exception)
            is Resource.Success -> PaymentRequest(id = createPaymentRequestResource.data, paymentProviderApp)
        }
    }

    fun onPayment() {
        if (paymentProviderApp.installedPaymentProviderApp == null) {
            giniHealth.setOpenBankState(GiniHealth.PaymentState.Error(Exception("Payment provider app not installed")))
            return
        }

        viewModelScope.launch {
            val valid = validatePaymentDetails(paymentDetails.value)
            if (valid) {
                giniHealth.setOpenBankState(GiniHealth.PaymentState.Loading)
                // TODO: first get the payment request and handle error before proceeding
                sendFeedback()
                giniHealth.setOpenBankState(
                    try {
                        GiniHealth.PaymentState.Success(getPaymentRequest())
                    } catch (throwable: Throwable) {
                        GiniHealth.PaymentState.Error(throwable)
                    }
                )
            }
        }
    }

    fun onBankOpened() {
        // Schedule on the main dispatcher to allow all collectors to receive the current state before
        // the state is overridden
        viewModelScope.launch(Dispatchers.Main) {
            giniHealth.setOpenBankState(GiniHealth.PaymentState.NoAction)
        }
    }

    private fun sendFeedback() {
        viewModelScope.launch {
            try {
                when (val documentResult = giniHealth.documentFlow.value) {
                    is ResultWrapper.Success -> paymentDetails.value.extractions?.let { extractionsContainer ->
                        giniHealth.giniHealthAPI.documentManager.sendFeedbackForExtractions(
                            documentResult.value,
                            extractionsContainer.specificExtractions,
                            extractionsContainer.compoundExtractions.withFeedback(paymentDetails.value)
                        )
                    }

                    is ResultWrapper.Error -> {}
                    is ResultWrapper.Loading -> {}
                }
            } catch (ignored: Throwable) {
                // Ignored since we don't want to interrupt the flow because of feedback failure
            }
        }
    }

    fun retryDocumentReview() {
        viewModelScope.launch {
            giniHealth.retryDocumentReview()
        }
    }

    class Factory(private val giniHealth: GiniHealth, private val paymentProviderApp: PaymentProviderApp, private val configuration: ReviewConfiguration) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(giniHealth, paymentProviderApp, configuration) as T
        }
    }

    companion object {
        const val SHOW_INFO_BAR_MS = 3000L
    }
}

private fun PaymentDetails.overwriteEmptyFields(value: PaymentDetails): PaymentDetails = this.copy(
    recipient = if (recipient.trim().isEmpty()) value.recipient else recipient,
    iban = if (iban.trim().isEmpty()) value.iban else iban,
    amount = if (amount.trim().isEmpty()) value.amount else amount,
    purpose = if (purpose.trim().isEmpty()) value.purpose else purpose,
    extractions = extractions ?: value.extractions,
)
