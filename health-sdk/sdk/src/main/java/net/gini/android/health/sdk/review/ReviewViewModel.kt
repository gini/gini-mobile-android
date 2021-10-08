package net.gini.android.health.sdk.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.PaymentProvider
import net.gini.android.core.api.models.PaymentRequestInput
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.review.bank.BankApp
import net.gini.android.health.sdk.review.error.NoBankSelected
import net.gini.android.health.sdk.review.error.NoProviderForPackageName
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.PaymentRequest
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.withFeedback
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.health.sdk.util.toBackendFormat

internal class ReviewViewModel(internal val giniHealth: GiniHealth) : ViewModel() {

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    private val _paymentValidation = MutableSharedFlow<List<ValidationMessage>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val paymentValidation: SharedFlow<List<ValidationMessage>> = _paymentValidation

    var selectedBank: BankApp? = null

    val isPaymentButtonEnabled: Flow<Boolean> = giniHealth.openBankState
        .combine(paymentDetails) { paymentState, paymentDetails ->
            val noEmptyFields = paymentDetails.recipient.isNotEmpty() && paymentDetails.iban.isNotEmpty() &&
                    paymentDetails.amount.isNotEmpty() && paymentDetails.purpose.isNotEmpty()
            val isLoading = (paymentState is GiniHealth.PaymentState.Loading)
            !isLoading && noEmptyFields
        }

    init {
        viewModelScope.launch {
            giniHealth.paymentFlow.collect { extractedPaymentDetails ->
                if (extractedPaymentDetails is ResultWrapper.Success) {
                    _paymentDetails.value = paymentDetails.value.overwriteEmptyFields(extractedPaymentDetails.value.copy(
                        amount = extractedPaymentDetails.value.amount.adjustToLocalDecimalSeparation()
                    ))
                }
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

    private fun validatePaymentDetails(): Boolean {
        val items = paymentDetails.value.validate()
        _paymentValidation.tryEmit(items)
        return items.isEmpty()
    }

    private suspend fun getPaymentProviderForPackage(packageName: String): PaymentProvider {
        return giniHealth.giniHealthAPI.documentManager.getPaymentProviders().find { it.packageName == packageName }
            ?: throw NoProviderForPackageName(packageName)
    }

    private suspend fun getPaymentRequest(bank: BankApp): PaymentRequest {
        return PaymentRequest(
            id = giniHealth.giniHealthAPI.documentManager.createPaymentRequest(
                PaymentRequestInput(
                    paymentProvider = getPaymentProviderForPackage(bank.packageName).id,
                    recipient = paymentDetails.value.recipient,
                    iban = paymentDetails.value.iban,
                    amount = "${paymentDetails.value.amount.toBackendFormat()}:EUR",
                    bic = null,
                    purpose = paymentDetails.value.purpose,
                )
            ),
            bankApp = bank
        )
    }

    fun onPayment() {
        viewModelScope.launch {
            val valid = validatePaymentDetails()
            if (valid) {
                giniHealth.setOpenBankState(GiniHealth.PaymentState.Loading)
                sendFeedback()
                giniHealth.setOpenBankState(try {
                    selectedBank?.let { bankApp ->
                        GiniHealth.PaymentState.Success(getPaymentRequest(bankApp))
                    } ?: GiniHealth.PaymentState.Error(NoBankSelected())
                } catch (throwable: Throwable) {
                    GiniHealth.PaymentState.Error(throwable)
                })
            }
        }
    }

    fun onBankOpened() {
        giniHealth.setOpenBankState(GiniHealth.PaymentState.NoAction)
    }

    private fun sendFeedback() {
        viewModelScope.launch {
            try {
                when (val documentResult = giniHealth.documentFlow.value) {
                    is ResultWrapper.Success -> paymentDetails.value.extractions?.let { extractionsContainer ->
                        giniHealth.giniHealthAPI.documentManager.sendFeedback(
                            documentResult.value,
                            extractionsContainer.specificExtractions.withFeedback(paymentDetails.value),
                            extractionsContainer.compoundExtractions
                        )
                    }
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
}

private fun PaymentDetails.overwriteEmptyFields(value: PaymentDetails): PaymentDetails = this.copy(
    recipient = if (recipient.trim().isEmpty()) value.recipient else recipient,
    iban = if (iban.trim().isEmpty()) value.iban else iban,
    amount = if (amount.trim().isEmpty()) value.amount else amount,
    purpose = if (purpose.trim().isEmpty()) value.purpose else purpose,
    extractions = extractions ?: value.extractions,
)

internal fun getReviewViewModelFactory(giniHealth: GiniHealth) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReviewViewModel(giniHealth) as T
    }
}