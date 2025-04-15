package net.gini.android.internal.payment.review.reviewComponent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.ResultWrapper
import net.gini.android.internal.payment.api.model.overwriteEmptyFields
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.ReviewViewStateLandscape
import net.gini.android.internal.payment.review.ValidationMessage
import net.gini.android.internal.payment.review.validate
import net.gini.android.internal.payment.review.validateIban
import net.gini.android.internal.payment.utils.extensions.sanitizeAmount
import net.gini.android.internal.payment.utils.withPrev
import org.slf4j.LoggerFactory


class ReviewComponent(
    val paymentComponent: PaymentComponent,
    val reviewConfig: ReviewConfiguration,
    val giniInternalPaymentModule: GiniInternalPaymentModule,
    coroutineScope: CoroutineScope
) {
    private val _paymentDetails = MutableStateFlow(PaymentDetails("", "", "", ""))
    val paymentDetails: StateFlow<PaymentDetails> = _paymentDetails

    private val _paymentValidation = MutableStateFlow<List<ValidationMessage>>(emptyList())
    val paymentValidation: StateFlow<List<ValidationMessage>> = _paymentValidation

    private val _lastFullyValidatedPaymentDetails = MutableStateFlow<PaymentDetails?>(null)

    private val _paymentProviderApp = MutableStateFlow<PaymentProviderApp?>(null)
    val paymentProviderApp: StateFlow<PaymentProviderApp?> = _paymentProviderApp

    private val _loadingFlow = MutableStateFlow(true)
    val loadingFlow: StateFlow<Boolean> = _loadingFlow

    val isPaymentButtonEnabled: Flow<Boolean> =
        combine(giniInternalPaymentModule.paymentFlow, paymentDetails) { paymentState, paymentDetails ->
            val noEmptyFields = paymentDetails.recipient.isNotEmpty() && paymentDetails.iban.isNotEmpty() &&
                    paymentDetails.amount.isNotEmpty() && paymentDetails.purpose.isNotEmpty()
            val isLoading = (paymentState is ResultWrapper.Loading)
            !isLoading && noEmptyFields
        }

    private val _reviewViewStateInLandscapeMode =
        MutableStateFlow<ReviewViewStateLandscape>(ReviewViewStateLandscape.EXPANDED)
    val reviewViewStateInLandscapeMode: Flow<ReviewViewStateLandscape> = _reviewViewStateInLandscapeMode
    init {
        coroutineScope.launch {
            _paymentDetails
                .combine(giniInternalPaymentModule.paymentFlow.filter { it !is ResultWrapper.Loading }) { paymentDetails, _ -> paymentDetails }
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
                    if (_lastFullyValidatedPaymentDetails.value?.iban == paymentDetails.iban) {
                        nonEmptyValidationMessages.addAll(validateIban(paymentDetails.iban).filterIsInstance<ValidationMessage.InvalidIban>())
                    }

                    // Emit all new empty validation messages along with other existing validation messages
                    _paymentValidation.value = (newEmptyValidationMessages + nonEmptyValidationMessages)
                }
        }

        coroutineScope.launch {
            giniInternalPaymentModule.paymentFlow.collect { extractedPaymentDetails ->
                _loadingFlow.tryEmit(extractedPaymentDetails is ResultWrapper.Loading)
                if (extractedPaymentDetails is ResultWrapper.Success) {
                    val paymentDetails = paymentDetails.value.overwriteEmptyFields(
                        extractedPaymentDetails.value
                    )
                    _loadingFlow.tryEmit(false)
                    _paymentDetails.value = paymentDetails.copy(
                        amount = paymentDetails.amount.sanitizeAmount()
                    )
                }
            }
        }

        coroutineScope.launch {
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


    fun shouldShowBankSelectionButton() = reviewConfig.selectBankButtonVisible

    fun validatePaymentDetails(paymentDetails: PaymentDetails): Boolean {
        val items = paymentDetails.validate()
        _paymentValidation.tryEmit(items)
        _lastFullyValidatedPaymentDetails.tryEmit(paymentDetails)
        return items.isEmpty()
    }

    fun setReviewViewModeInLandscapeMode(state: ReviewViewStateLandscape) {
        _reviewViewStateInLandscapeMode.value = state
    }

    fun getReviewViewStateInLandscapeMode() = _reviewViewStateInLandscapeMode.value

    companion object {
        private val LOG = LoggerFactory.getLogger(ReviewComponent::class.java)
    }
}