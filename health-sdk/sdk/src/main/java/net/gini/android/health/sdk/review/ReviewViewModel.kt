package net.gini.android.health.sdk.review

import android.content.Context
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
import kotlinx.coroutines.withContext
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreference.PreferredBankApp
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.bank.BankApp
import net.gini.android.health.sdk.review.bank.getValidBankApps
import net.gini.android.health.sdk.review.error.NoBankSelected
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.PaymentRequest
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.withFeedback
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.health.sdk.util.toBackendFormat
import net.gini.android.health.sdk.util.withPrev

internal class ReviewViewModel(internal val giniHealth: GiniHealth) : ViewModel() {

    internal var userPreferences: UserPreferences? = null

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    private val _paymentValidation = MutableStateFlow<List<ValidationMessage>>(emptyList())
    val paymentValidation: StateFlow<List<ValidationMessage>> = _paymentValidation

    private val _lastFullyValidatedPaymentDetails = MutableStateFlow<PaymentDetails?>(null)

    private val _bankApps = MutableStateFlow<BankAppsState>(BankAppsState.Loading)
    val bankApps: StateFlow<BankAppsState> = _bankApps

    private var _selectedBank = MutableStateFlow<BankApp?>(null)
    var selectedBank: StateFlow<BankApp?> = _selectedBank

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    val isPaymentButtonEnabled: Flow<Boolean> =
        combine(giniHealth.openBankState, paymentDetails, selectedBank) { paymentState, paymentDetails, selectedBank ->
            val noEmptyFields = paymentDetails.recipient.isNotEmpty() && paymentDetails.iban.isNotEmpty() &&
                    paymentDetails.amount.isNotEmpty() && paymentDetails.purpose.isNotEmpty()
            val isLoading = (paymentState is GiniHealth.PaymentState.Loading)
            !isLoading && noEmptyFields && selectedBank != null
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
                    val newEmptyValidationMessages = paymentDetails.validate().filterIsInstance<ValidationMessage.Empty>()

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

    suspend fun getBankApps(context: Context) {
        _bankApps.value = BankAppsState.Loading
        withContext(viewModelScope.coroutineContext) {
            _bankApps.value = try {
                when (val paymentProvidersResource = giniHealth.giniHealthAPI.documentManager.getPaymentProviders()) {
                    is Resource.Cancelled -> BankAppsState.Error(Exception("Cancelled"))
                    is Resource.Error -> BankAppsState.Error(
                        paymentProvidersResource.exception ?: Exception(
                            paymentProvidersResource.message
                        )
                    )
                    is Resource.Success -> BankAppsState.Success(
                        context.packageManager.getValidBankApps(
                            paymentProvidersResource.data,
                            context
                        )
                    )
                }
            } catch (e: Exception) {
                BankAppsState.Error(e)
            }
        }
    }

    fun initSelectedBank() {
        if (_selectedBank.value == null) {
            _selectedBank.value = (_bankApps.value as? BankAppsState.Success)?.bankApps?.let { bankApps ->
                userPreferences?.get(PreferredBankApp())?.let { preferredBank ->
                    bankApps.firstOrNull { it.packageName == preferredBank.value } ?: bankApps.firstOrNull()
                } ?: bankApps.firstOrNull()
            }
        }
    }

    fun setSelectedBank(selectedBank: BankApp) {
        _selectedBank.value = selectedBank
        userPreferences?.set(PreferredBankApp(selectedBank.packageName))
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

    private suspend fun getPaymentRequest(bank: BankApp): PaymentRequest {
        return when(val createPaymentRequestResource = giniHealth.giniHealthAPI.documentManager.createPaymentRequest(
            PaymentRequestInput(
                paymentProvider = bank.paymentProvider.id,
                recipient = paymentDetails.value.recipient,
                iban = paymentDetails.value.iban,
                amount = "${paymentDetails.value.amount.toBackendFormat()}:EUR",
                bic = null,
                purpose = paymentDetails.value.purpose,
            )
        )) {
            is Resource.Cancelled -> throw Exception("Cancelled")
            is Resource.Error -> throw Exception(createPaymentRequestResource.exception)
            is Resource.Success -> PaymentRequest(id = createPaymentRequestResource.data, bankApp = bank)
        }
    }

    fun onPayment() {
        viewModelScope.launch {
            val valid = validatePaymentDetails(paymentDetails.value)
            if (valid) {
                giniHealth.setOpenBankState(GiniHealth.PaymentState.Loading)
                // TODO: first get the payment request and handle error before proceeding
                sendFeedback()
                giniHealth.setOpenBankState(try {
                    _selectedBank.value?.let { bankApp ->
                        GiniHealth.PaymentState.Success(getPaymentRequest(bankApp))
                    } ?: GiniHealth.PaymentState.Error(NoBankSelected())
                } catch (throwable: Throwable) {
                    GiniHealth.PaymentState.Error(throwable)
                })
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

    sealed class BankAppsState {
        object Loading : BankAppsState()
        class Success(val bankApps: List<BankApp>) : BankAppsState()
        class Error(val throwable: Throwable) : BankAppsState()
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

internal fun getReviewViewModelFactory(giniHealth: GiniHealth) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReviewViewModel(giniHealth) as T
    }
}
