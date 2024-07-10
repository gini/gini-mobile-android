package net.gini.android.merchant.sdk.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.Document
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.api.payment.model.PaymentRequest
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.preferences.UserPreferences
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.review.pager.DocumentPageAdapter
import net.gini.android.merchant.sdk.review.reviewComponent.ReviewComponent
import net.gini.android.merchant.sdk.util.FlowBottomSheetsManager
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import net.gini.android.merchant.sdk.util.PaymentNextStep
import org.slf4j.LoggerFactory
import java.io.File


internal class ReviewViewModel(val giniMerchant: GiniMerchant, val configuration: ReviewConfiguration, val paymentComponent: PaymentComponent, val documentId: String, private val giniPaymentManager: GiniPaymentManager) : ViewModel(), FlowBottomSheetsManager {

    internal var userPreferences: UserPreferences? = null
    override var openWithPreferences: OpenWithPreferences? = null

    var reviewComponent: ReviewComponent

    private var _isInfoBarVisible = MutableStateFlow(true)
    var isInfoBarVisible: StateFlow<Boolean> = _isInfoBarVisible

    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow: MutableStateFlow<PaymentRequest?> = MutableStateFlow(null)

    override val shareWithFlowStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val paymentNextStep: SharedFlow<PaymentNextStep> = paymentNextStepFlow

    override var openWithCounter: Int = 0

    init {
        viewModelScope.launch {
            delay(SHOW_INFO_BAR_MS)
            _isInfoBarVisible.value = false
        }

        reviewComponent = ReviewComponent(paymentComponent, configuration, giniMerchant, giniPaymentManager, viewModelScope)
    }

    fun getPages(document: Document): List<DocumentPageAdapter.Page> {
        return (1..document.pageCount).map { pageNumber ->
            DocumentPageAdapter.Page(document.id, pageNumber)
        }
    }

    fun onPayment() {
        viewModelScope.launch {
            giniPaymentManager.onPayment(reviewComponent.paymentProviderApp.value, reviewComponent.paymentDetails.value)
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
        sharePdf(reviewComponent.paymentProviderApp.value, externalCacheDir, viewModelScope)
    }

    override fun emitSDKEvent(sdkEvent: GiniMerchant.PaymentState) {
        giniMerchant.emitSDKEvent(sdkEvent)
    }

    override fun sendFeedback() {
        viewModelScope.launch {
            giniPaymentManager.sendFeedbackAndStartLoading(reviewComponent.paymentDetails.value)
        }
    }

    override suspend fun getPaymentRequest(): PaymentRequest = giniPaymentManager.getPaymentRequest(reviewComponent.paymentProviderApp.value, reviewComponent.paymentDetails.value)

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> =
        giniMerchant.giniHealthAPI.documentManager.getPaymentRequestDocument(paymentRequest.id)

    fun onPaymentButtonTapped(externalCacheDir: File?) {
        checkNextStep(reviewComponent.paymentProviderApp.value, externalCacheDir, viewModelScope)
    }

    fun emitShareWithStartedEvent() {
        paymentRequestFlow.value?.let {
            giniMerchant.emitSDKEvent(GiniMerchant.PaymentState.Success(it, reviewComponent.paymentProviderApp.value?.name ?: ""))
        }
        shareWithFlowStarted.tryEmit(true)
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
