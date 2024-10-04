package net.gini.android.health.sdk.review

import android.content.Context
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
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.withFeedback
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.health.sdk.util.toBackendFormat
import net.gini.android.health.sdk.util.withPrev
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.ValidationMessage
import net.gini.android.internal.payment.utils.FlowBottomSheetsManager
import org.slf4j.LoggerFactory
import java.io.File

internal class ReviewViewModel(
    val giniHealth: GiniHealth,
    val configuration: ReviewConfiguration,
    val paymentComponent: PaymentComponent,
    val documentId: String
) : ViewModel(), FlowBottomSheetsManager {

    internal var userPreferences: UserPreferences? = null
    internal var context: Context? = null

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    private val _paymentValidation = MutableStateFlow<List<ValidationMessage>>(emptyList())
    val paymentValidation: StateFlow<List<ValidationMessage>> = _paymentValidation

    private val _lastFullyValidatedPaymentDetails = MutableStateFlow<PaymentDetails?>(null)

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    private val _paymentNextStep = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    val paymentNextStep: SharedFlow<PaymentNextStep> = _paymentNextStep
    override val giniInternalPaymentModule = giniHealth.giniInternalPaymentModule

    override var openWithCounter: Int = 0
    override val paymentNextStepFlow= MutableSharedFlow<net.gini.android.internal.payment.utils.PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow = MutableStateFlow<PaymentRequest?>(null)
    override val shareWithFlowStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _paymentRequestFlow = MutableStateFlow<PaymentRequest?>(null)

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
        startObservingOpenWithCount(viewModelScope, paymentProviderApp.value?.paymentProvider?.id ?: "")
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

    fun validatePaymentDetails(): Boolean = validatePaymentDetails(paymentDetails.value)

    private fun validatePaymentDetails(paymentDetails: PaymentDetails): Boolean {
        val items = paymentDetails.validate()
        _paymentValidation.tryEmit(items)
        _lastFullyValidatedPaymentDetails.tryEmit(paymentDetails)
        return items.isEmpty()
    }

    override suspend fun getPaymentRequest(): PaymentRequest {
        val paymentProviderApp = paymentProviderApp.value
        if (paymentProviderApp == null) {
            LOG.error("Cannot create PaymentRequest: No selected payment provider app")
            throw Exception("Cannot create PaymentRequest: No selected payment provider app")
        }

        return when (val createPaymentRequestResource = giniHealth.documentManager.createPaymentRequest(
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
            is Resource.Success -> PaymentRequest(id = createPaymentRequestResource.data, paymentProviderApp.name)
        }
    }

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> {
        TODO("Not yet implemented")
    }

    fun onPayment() {
        val paymentProviderApp = paymentProviderApp.value
        if (paymentProviderApp == null) {
            LOG.error("No selected payment provider app")
            giniHealth.setOpenBankState(GiniHealth.PaymentState.Error(Exception("No selected payment provider app")), viewModelScope)
            return
        }

        if (paymentProviderApp.installedPaymentProviderApp == null) {
            LOG.error("Payment provider app not installed")
            giniHealth.setOpenBankState(GiniHealth.PaymentState.Error(Exception("Payment provider app not installed")), viewModelScope)
            return
        }

        viewModelScope.launch {
            val valid = validatePaymentDetails(paymentDetails.value)
            if (valid) {
                sendFeedbackAndStartLoading()
                giniHealth.setOpenBankState(
                    try {
                        GiniHealth.PaymentState.Success(getPaymentRequest())
                    } catch (throwable: Throwable) {
                        GiniHealth.PaymentState.Error(throwable)
                    },
                    viewModelScope
                )
            }
        }
    }

    fun onBankOpened() {
        // Schedule on the main dispatcher to allow all collectors to receive the current state before
        // the state is overridden
        viewModelScope.launch(Dispatchers.Main) {
            giniHealth.setOpenBankState(GiniHealth.PaymentState.NoAction, viewModelScope)
        }
    }

    private fun sendFeedback() {
        viewModelScope.launch {
            try {
                when (val documentResult = giniHealth.documentFlow.value) {
                    is ResultWrapper.Success -> paymentDetails.value.extractions?.let { extractionsContainer ->
                        giniHealth.documentManager.sendFeedbackForExtractions(
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
                sendFeedbackAndStartLoading()
                val paymentRequest = try {
                    getPaymentRequest()
                } catch (throwable: Throwable) {
                    giniHealth.setOpenBankState(GiniHealth.PaymentState.Error(throwable), viewModelScope)
                    return@withContext
                }
                _paymentRequestFlow.value = paymentRequest
                val byteArrayResource = async {  giniHealth.documentManager.getPaymentRequestDocument(paymentRequest.id) }.await()
                when (byteArrayResource) {
                    is Resource.Cancelled -> {
                        giniHealth.setOpenBankState(GiniHealth.PaymentState.Error(Exception("Cancelled")), viewModelScope)
                    }
                    is Resource.Error -> {
                        giniHealth.setOpenBankState(GiniHealth.PaymentState.Error(byteArrayResource.exception ?: Exception("Error")), viewModelScope)
                    }
                    is Resource.Success -> {
                        val newFile = externalCacheDir?.createTempPdfFile(byteArrayResource.data, "payment-request")
                        newFile?.let {
                            _paymentNextStep.tryEmit(PaymentNextStep.OpenSharePdf(it))
                        }
                    }
                }
            }
        }
    }

    internal fun setOpenBankStateAfterShareWith() {
        _paymentRequestFlow.value?.let {
            giniHealth.setOpenBankState(GiniHealth.PaymentState.Success(it), viewModelScope)
        }
    }

    private fun sendFeedbackAndStartLoading() {
        giniHealth.setOpenBankState(GiniHealth.PaymentState.Loading, viewModelScope)
        // TODO: first get the payment request and handle error before proceeding
        sendFeedback()
    }

    fun loadPaymentDetails() {
        viewModelScope.launch {
            giniHealth.setDocumentForReview(documentId)
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
        private val giniHealth: GiniHealth,
        private val configuration: ReviewConfiguration,
        private val paymentComponent: PaymentComponent,
        private val documentId: String
    ) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(giniHealth, configuration, paymentComponent, documentId) as T
        }
    }

    companion object {
        const val SHOW_INFO_BAR_MS = 3000L
        private val LOG = LoggerFactory.getLogger(ReviewViewModel::class.java)
        const val SHOW_OPEN_WITH_TIMES = 3
    }
}

private fun PaymentDetails.overwriteEmptyFields(value: PaymentDetails): PaymentDetails = this.copy(
    recipient = if (recipient.trim().isEmpty()) value.recipient else recipient,
    iban = if (iban.trim().isEmpty()) value.iban else iban,
    amount = if (amount.trim().isEmpty()) value.amount else amount,
    purpose = if (purpose.trim().isEmpty()) value.purpose else purpose,
    extractions = extractions ?: value.extractions,
)
