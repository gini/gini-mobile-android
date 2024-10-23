package net.gini.android.health.sdk.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.models.Document
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.PaymentRequest
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.toCommonPaymentDetails
import net.gini.android.health.sdk.review.model.withFeedback
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.adjustToLocalDecimalSeparation
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewComponent
import net.gini.android.internal.payment.utils.FlowBottomSheetsManager
import net.gini.android.internal.payment.utils.PaymentNextStep
import org.slf4j.LoggerFactory
import java.io.File

internal class ReviewViewModel(
    val giniHealth: GiniHealth,
    val configuration: ReviewConfiguration,
    val paymentComponent: PaymentComponent,
    val documentId: String,
) : ViewModel(), FlowBottomSheetsManager {

    internal var userPreferences: UserPreferences? = null

    val reviewComponent: ReviewComponent = ReviewComponent(
        paymentComponent = paymentComponent,
        reviewConfig = configuration,
        giniInternalPaymentModule = giniHealth.giniInternalPaymentModule,
        coroutineScope = viewModelScope
    )

    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    override val giniInternalPaymentModule = giniHealth.giniInternalPaymentModule

    override var openWithCounter: Int = 0
    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow = MutableStateFlow<net.gini.android.internal.payment.api.model.PaymentRequest?>(null)
    override val shareWithFlowStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _paymentRequestFlow = MutableStateFlow<PaymentRequest?>(null)

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
                    giniInternalPaymentModule.setPaymentDetails(paymentDetails.toCommonPaymentDetails())
                }
            }
        }
        viewModelScope.launch {
            delay(SHOW_INFO_BAR_MS)
            _isInfoBarVisible.value = false
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
        viewModelScope.launch {
            giniInternalPaymentModule.eventsFlow.collect { event ->
                handleInternalEvents(event)
            }
        }
        viewModelScope.launch {
            reviewComponent.paymentDetails.collect { paymentDetails ->
                _paymentDetails.value = _paymentDetails.value.copy(recipient = paymentDetails.recipient, iban = paymentDetails.iban, amount = paymentDetails.amount, purpose = paymentDetails.purpose)
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

    override suspend fun getPaymentRequest(): net.gini.android.internal.payment.api.model.PaymentRequest = paymentComponent.paymentModule.getPaymentRequest(paymentProviderApp.value, paymentDetails.value.toCommonPaymentDetails())
    override suspend fun getPaymentRequestDocument(paymentRequest: net.gini.android.internal.payment.api.model.PaymentRequest) = giniInternalPaymentModule.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id)

    fun onPayment() = viewModelScope.launch {
        giniHealth.giniInternalPaymentModule.onPayment(
            paymentProviderApp.value,
            paymentDetails.value.toCommonPaymentDetails()
        )
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

    fun onPaymentButtonTapped(externalCacheDir: File?) {
        checkNextStep(paymentProviderApp.value, externalCacheDir, viewModelScope)
    }

    fun onForwardToSharePdfTapped(externalCacheDir: File?) {
        sendFeedbackAndStartLoading()
        sharePdf(paymentProviderApp.value, externalCacheDir, viewModelScope)
    }

    fun retryDocumentReview() {
        viewModelScope.launch {
            giniHealth.retryDocumentReview()
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

    private fun handleInternalEvents(event: GiniInternalPaymentModule.InternalPaymentEvents) {
        when (event) {
            is GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred -> giniHealth.setOpenBankState(GiniHealth.PaymentState.Error(event.throwable), viewModelScope)
            is GiniInternalPaymentModule.InternalPaymentEvents.OnFinishedWithPaymentRequestCreated -> paymentProviderApp.value?.let { giniHealth.setOpenBankState(GiniHealth.PaymentState.Success(PaymentRequest(event.paymentRequestId, it)), viewModelScope) }
            is GiniInternalPaymentModule.InternalPaymentEvents.NoAction -> giniHealth.setOpenBankState(GiniHealth.PaymentState.NoAction, viewModelScope)
            is GiniInternalPaymentModule.InternalPaymentEvents.OnLoading -> giniHealth.setOpenBankState(GiniHealth.PaymentState.Loading, viewModelScope)
            else -> {}
        }
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
    }
}

private fun PaymentDetails.overwriteEmptyFields(value: PaymentDetails): PaymentDetails = this.copy(
    recipient = if (recipient.trim().isEmpty()) value.recipient else recipient,
    iban = if (iban.trim().isEmpty()) value.iban else iban,
    amount = if (amount.trim().isEmpty()) value.amount else amount,
    purpose = if (purpose.trim().isEmpty()) value.purpose else purpose,
    extractions = extractions ?: value.extractions,
)
