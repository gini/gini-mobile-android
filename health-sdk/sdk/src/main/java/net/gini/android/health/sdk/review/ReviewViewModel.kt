package net.gini.android.health.sdk.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.toCommonPaymentDetails
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewComponent
import org.slf4j.LoggerFactory

internal class ReviewViewModel(
    val giniHealth: GiniHealth,
    val configuration: ReviewConfiguration,
    val paymentComponent: PaymentComponent,
    val documentId: String,
    val shouldShowCloseButton: Boolean,
    val reviewFragmentListener: ReviewFragmentListener
) : ViewModel() {

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

    val giniInternalPaymentModule = giniHealth.giniInternalPaymentModule

    private val _documentPages = MutableStateFlow<DocumentPagesResult>(DocumentPagesResult.Loading)
    val documentPages: StateFlow<DocumentPagesResult> = _documentPages

    init {
        viewModelScope.launch {
            giniHealth.paymentFlow.collect { extractedPaymentDetails ->
                if (extractedPaymentDetails is ResultWrapper.Success) {
                    val paymentDetails = paymentDetails.value.overwriteEmptyFields(
                        extractedPaymentDetails.value
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
            reviewComponent.paymentDetails.collect { paymentDetails ->
                _paymentDetails.value = _paymentDetails.value.copy(recipient = paymentDetails.recipient, iban = paymentDetails.iban, amount = paymentDetails.amount, purpose = paymentDetails.purpose)
            }
        }

        viewModelScope.launch {
            giniHealth.documentFlow.collect { documentResult ->
                when (documentResult) {
                    is ResultWrapper.Success -> {
                        val pages = (1..documentResult.value.pageCount).map { pageNumber ->
                            val image = giniHealth.documentManager.getPageImage(documentId, pageNumber)
                            DocumentPageAdapter.Page(image, pageNumber)
                        }
                        _documentPages.value = DocumentPagesResult.Success(pages)
                    }
                    is ResultWrapper.Error -> {
                        _documentPages.value = DocumentPagesResult.Error
                    }
                    else -> {
                        _documentPages.value = DocumentPagesResult.Error
                    }
                }
            }
        }
    }

    /**
     * In case one of the images in the document returned Error, try to fetch it again and insert it at the corresponding location.
     *
     * We will not get to this case if the document fetch returned Error, since we don't display the retry button for the image.
     *
     */

    fun reloadImage(pageNumber: Int) {
        viewModelScope.launch {
            val image = giniHealth.documentManager.getPageImage(documentId, pageNumber)
            if (_documentPages.value is DocumentPagesResult.Success) {
                with(_documentPages.value as DocumentPagesResult.Success) {
                    val pages = this.pagesList.toMutableList().apply {
                        removeAt(pageNumber - 1)    // page number is the number in the document, which starts with 1. subtract 1 to match the list ordering
                        add(pageNumber - 1, DocumentPageAdapter.Page(image, pageNumber))
                    }
                    _documentPages.value = DocumentPagesResult.Success(pages)
                }
            }
        }
    }

    fun retryDocumentReview() {
        viewModelScope.launch {
            giniHealth.retryDocumentReview()
        }
    }

    class Factory(
        private val giniHealth: GiniHealth,
        private val configuration: ReviewConfiguration,
        private val paymentComponent: PaymentComponent,
        private val documentId: String,
        private val shouldShowCloseButton: Boolean,
        private val reviewFragmentListener: ReviewFragmentListener
    ) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(giniHealth, configuration, paymentComponent, documentId, shouldShowCloseButton, reviewFragmentListener) as T
        }
    }

    companion object {
        const val SHOW_INFO_BAR_MS = 3000L
        private val LOG = LoggerFactory.getLogger(ReviewViewModel::class.java)
    }
}

internal sealed interface DocumentPagesResult {
    data class Success(val pagesList: List<DocumentPageAdapter.Page>): DocumentPagesResult
    data object Error: DocumentPagesResult

    data object Loading: DocumentPagesResult
}

private fun PaymentDetails.overwriteEmptyFields(value: PaymentDetails): PaymentDetails = this.copy(
    recipient = if (recipient.trim().isEmpty()) value.recipient else recipient,
    iban = if (iban.trim().isEmpty()) value.iban else iban,
    amount = if (amount.trim().isEmpty()) value.amount else amount,
    purpose = if (purpose.trim().isEmpty()) value.purpose else purpose,
    extractions = extractions ?: value.extractions,
)
