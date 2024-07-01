package net.gini.android.merchant.sdk.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.api.payment.model.overwriteEmptyFields
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.preferences.UserPreferences
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.review.pager.DocumentPageAdapter
import net.gini.android.merchant.sdk.util.FlowBottomSheetsManager
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import net.gini.android.merchant.sdk.util.PaymentNextStep
import net.gini.android.merchant.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.merchant.sdk.util.withPrev
import org.slf4j.LoggerFactory
import java.io.File


internal class ReviewViewModel(val giniMerchant: GiniMerchant, val configuration: ReviewConfiguration, val paymentComponent: PaymentComponent, val documentId: String, private val giniPaymentManager: GiniPaymentManager) : ViewModel(), FlowBottomSheetsManager {

    internal var userPreferences: UserPreferences? = null
    override var openWithPreferences: OpenWithPreferences? = null

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    val paymentValidation: StateFlow<List<ValidationMessage>> = giniPaymentManager.paymentValidation

    private val lastFullyValidatedPaymentDetails = giniPaymentManager.lastFullyValidatedPaymentDetails

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow: MutableStateFlow<PaymentRequest?> = MutableStateFlow(null)

    val paymentNextStep: SharedFlow<PaymentNextStep> = paymentNextStepFlow

    override var openWithCounter: Int = 0

    val isPaymentButtonEnabled: Flow<Boolean> =
        combine(giniMerchant.paymentFlow, paymentDetails) { paymentState, paymentDetails ->
            val noEmptyFields = paymentDetails.recipient.isNotEmpty() && paymentDetails.iban.isNotEmpty() &&
                    paymentDetails.amount.isNotEmpty() && paymentDetails.purpose.isNotEmpty()
            val isLoading = (paymentState is ResultWrapper.Loading)
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
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.NoAction)
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

    fun onForwardToSharePdfTapped(externalCacheDir: File?) {
        sharePdf(paymentProviderApp.value, externalCacheDir, viewModelScope)
    }

    override fun emitSDKEvent(sdkEvent: GiniMerchant.PaymentState) {
        giniMerchant.emitSDKEvent(sdkEvent)
    }

    override fun sendFeedback() {
        viewModelScope.launch {
            giniPaymentManager.sendFeedbackAndStartLoading(paymentDetails.value)
        }
    }

    override suspend fun getPaymentRequest(): PaymentRequest = giniPaymentManager.getPaymentRequest(paymentProviderApp.value, paymentDetails.value)

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> =
        giniMerchant.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id)

    fun onPaymentButtonTapped(externalCacheDir: File?) {
        checkNextStep(paymentProviderApp.value, externalCacheDir, viewModelScope)
    }

    fun emitFinishEvent() {
        paymentRequestFlow.value?.let {
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Success(it, paymentProviderApp.value?.name ?: ""))
        }
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
