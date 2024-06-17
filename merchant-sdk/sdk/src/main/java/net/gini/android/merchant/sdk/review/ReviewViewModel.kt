package net.gini.android.merchant.sdk.review

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.preferences.UserPreferences
import net.gini.android.merchant.sdk.review.model.PaymentDetails
import net.gini.android.merchant.sdk.review.model.ResultWrapper
import net.gini.android.merchant.sdk.review.model.overwriteEmptyFields
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.review.pager.DocumentPageAdapter
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import net.gini.android.merchant.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.merchant.sdk.util.extensions.createTempPdfFile
import net.gini.android.merchant.sdk.util.withPrev
import org.slf4j.LoggerFactory
import java.io.File


internal class ReviewViewModel(val giniMerchant: GiniMerchant, val configuration: ReviewConfiguration, val paymentComponent: PaymentComponent, val documentId: String, private val giniPaymentManager: GiniPaymentManager) : ViewModel() {

    internal var userPreferences: UserPreferences? = null
    internal var openWithPreferences: OpenWithPreferences? = null

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    val paymentValidation: StateFlow<List<ValidationMessage>> = giniPaymentManager.paymentValidation

    private val lastFullyValidatedPaymentDetails = giniPaymentManager.lastFullyValidatedPaymentDetails

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    private val _paymentNextStep = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    val paymentNextStep: SharedFlow<PaymentNextStep> = _paymentNextStep

    private var openWithCounter: Int = 0

    val isPaymentButtonEnabled: Flow<Boolean> =
        combine(giniMerchant.openBankState, paymentDetails) { paymentState, paymentDetails ->
            val noEmptyFields = paymentDetails.recipient.isNotEmpty() && paymentDetails.iban.isNotEmpty() &&
                    paymentDetails.amount.isNotEmpty() && paymentDetails.purpose.isNotEmpty()
            val isLoading = (paymentState is GiniMerchant.PaymentState.Loading)
            !isLoading && noEmptyFields
        }

    init {
        viewModelScope.launch {
            giniMerchant.paymentFlow.collect { extractedPaymentDetails ->
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
                .combine(giniMerchant.paymentFlow.filter { it !is ResultWrapper.Loading }) { paymentDetails, _ -> paymentDetails }
                .withPrev()
                .collect { (prevPaymentDetails, paymentDetails) ->
                    // Get all validation messages except emptiness validations
                    val nonEmptyValidationMessages = paymentValidation.value
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
                    if (lastFullyValidatedPaymentDetails.value?.iban == paymentDetails.iban) {
                        nonEmptyValidationMessages.addAll(validateIban(paymentDetails.iban).filterIsInstance<ValidationMessage.InvalidIban>())
                    }

                    // Emit all new empty validation messages along with other existing validation messages
                    giniPaymentManager.emitPaymentValidation(newEmptyValidationMessages + nonEmptyValidationMessages)
                }
        }
        viewModelScope.launch {
            paymentComponent.selectedPaymentProviderAppFlow.collect { selectedPaymentProviderAppState ->
                when (selectedPaymentProviderAppState) {
                    is SelectedPaymentProviderAppState.AppSelected -> {
                        _paymentProviderApp.value = selectedPaymentProviderAppState.paymentProviderApp
                    }

                    SelectedPaymentProviderAppState.NothingSelected -> {
                        LOG.error("No selected payment provider app")
                    }
                }
            }
        }
    }

    fun startObservingOpenWithCount() {
        paymentProviderApp.value?.paymentProvider?.id?.let { paymentProviderAppId ->
            viewModelScope.launch {
                openWithPreferences?.getLiveCountForPaymentProviderId(paymentProviderAppId)?.collectLatest {
                    openWithCounter = it ?: 0
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

    fun onPayment() {
        viewModelScope.launch {
            giniPaymentManager.onPayment(paymentProviderApp.value, paymentDetails.value)
        }
    }

    fun onBankOpened() {
        // Schedule on the main dispatcher to allow all collectors to receive the current state before
        // the state is overridden
        viewModelScope.launch(Dispatchers.Main) {
            giniMerchant.setOpenBankState(GiniMerchant.PaymentState.NoAction)
        }
    }

    fun loadPaymentDetails() {
        viewModelScope.launch {
            giniMerchant.setDocumentForReview(documentId)
        }
    }

    fun retryDocumentReview() {
        viewModelScope.launch {
            giniMerchant.retryDocumentReview()
        }
    }

    fun incrementOpenWithCounter() = viewModelScope.launch {
        paymentProviderApp.value?.paymentProvider?.id?.let {  paymentProviderAppId ->
            openWithPreferences?.incrementCountForPaymentProviderId(paymentProviderAppId)
        }
    }

    fun onPaymentButtonTapped(externalCacheDir: File?) {
        if (paymentProviderApp.value?.paymentProvider?.gpcSupported() == true) {
            if (paymentProviderApp.value?.isInstalled() == true) _paymentNextStep.tryEmit(PaymentNextStep.RedirectToBank)
            else _paymentNextStep.tryEmit(PaymentNextStep.ShowInstallApp)
            return
        }
        if (openWithCounter < SHOW_OPEN_WITH_TIMES) {
            _paymentNextStep.tryEmit(PaymentNextStep.ShowOpenWithSheet)
        } else {
            _paymentNextStep.tryEmit(PaymentNextStep.SetLoadingVisibility(true))
            getFileAsByteArray(externalCacheDir)
        }
    }

    fun onForwardToSharePdfTapped(externalCacheDir: File?) {
        _paymentNextStep.tryEmit(PaymentNextStep.SetLoadingVisibility(true))
        getFileAsByteArray(externalCacheDir)
    }

    @VisibleForTesting
    internal fun getFileAsByteArray(externalCacheDir: File?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                giniPaymentManager.sendFeedbackAndStartLoading(paymentDetails.value)
                val paymentRequest = try {
                    giniPaymentManager.getPaymentRequest(paymentProviderApp.value, paymentDetails.value)
                } catch (throwable: Throwable) {
                    giniMerchant.setOpenBankState(GiniMerchant.PaymentState.Error(throwable))
                    return@withContext
                }
                val byteArrayResource = async {  giniMerchant.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id) }.await()
                when (byteArrayResource) {
                    is Resource.Cancelled -> {
                        giniMerchant.setOpenBankState(GiniMerchant.PaymentState.Error(Exception("Cancelled")))
                    }
                    is Resource.Error -> {
                        giniMerchant.setOpenBankState(GiniMerchant.PaymentState.Error(byteArrayResource.exception ?: Exception("Error")))
                    }
                    is Resource.Success -> {
                        giniMerchant.setOpenBankState(GiniMerchant.PaymentState.Success(paymentRequest))
                        val newFile = externalCacheDir?.createTempPdfFile(byteArrayResource.data, "payment-request")
                        newFile?.let {
                            _paymentNextStep.tryEmit(PaymentNextStep.OpenSharePdf(it))
                        }
                    }
                }
            }
        }
    }

    sealed class PaymentNextStep {
        object RedirectToBank: PaymentNextStep()
        object ShowOpenWithSheet: PaymentNextStep()
        object ShowInstallApp: PaymentNextStep()
        data class OpenSharePdf(val file: File): PaymentNextStep()
        data class SetLoadingVisibility(val isVisible: Boolean): PaymentNextStep()
    }

    class Factory(
        private val giniMerchant: GiniMerchant,
        private val configuration: ReviewConfiguration,
        private val paymentComponent: PaymentComponent,
        private val documentId: String,
        private val giniPayment: GiniPaymentManager = GiniPaymentManager(giniMerchant)) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(giniMerchant, configuration, paymentComponent, documentId, giniPayment) as T
        }
    }

    companion object {
        const val SHOW_INFO_BAR_MS = 3000L
        private val LOG = LoggerFactory.getLogger(ReviewViewModel::class.java)
        const val SHOW_OPEN_WITH_TIMES = 3
    }
}
