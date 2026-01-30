package net.gini.android.health.sdk.integratedFlow

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import net.gini.android.core.api.Resource
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.model.toCommonPaymentDetails
import net.gini.android.health.sdk.review.model.withFeedback
import net.gini.android.health.sdk.util.DisplayedScreen
import net.gini.android.health.sdk.util.DisplayedScreen.Companion.toInternalDisplayedScreen
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentComponent.BankPickerRows
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.BackListener
import net.gini.android.internal.payment.utils.FlowBottomSheetsManager
import net.gini.android.internal.payment.utils.PaymentNextStep
import java.io.File
import java.util.Stack

class PaymentFlowViewModel(
    private val savedStateHandle: SavedStateHandle,
    val giniHealth: GiniHealth
) : ViewModel(), FlowBottomSheetsManager, BackListener {

    var paymentDetails: PaymentDetails?
        get() = savedStateHandle.get("paymentDetails")
        set(value) = savedStateHandle.set("paymentDetails", value)

    var documentId: String?
        get() = savedStateHandle.get("documentId")
        set(value) = savedStateHandle.set("documentId", value)

    var paymentFlowConfiguration: PaymentFlowConfiguration?
        get() = savedStateHandle.get("paymentFlowConfiguration")
        set(value) = savedStateHandle.set("paymentFlowConfiguration", value)

    private val backstack: Stack<DisplayedScreen> =
        Stack<DisplayedScreen>().also { it.add(DisplayedScreen.Nothing) }
    private var initialSelectedPaymentProvider: PaymentProviderApp? = null

    override val paymentNextStepFlow = MutableSharedFlow<PaymentNextStep>(
        extraBufferCapacity = 1,
    )
    override val paymentRequestFlow: MutableStateFlow<PaymentRequest?> = MutableStateFlow(null)
    override val giniInternalPaymentModule: GiniInternalPaymentModule =
        giniHealth.giniInternalPaymentModule

    val paymentComponent = giniInternalPaymentModule.paymentComponent

    val paymentNextStep: SharedFlow<PaymentNextStep> = paymentNextStepFlow

    private val _backButtonEvent = MutableSharedFlow<Void?>(extraBufferCapacity = 1)
    val backButtonEvent: SharedFlow<Void?> = _backButtonEvent

    override val shareWithFlowStarted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var externalCacheDir: File? = null

    init {
        viewModelScope.launch {
            giniInternalPaymentModule.eventsFlow.collect { event ->
                handleInternalEvents(event)
            }
        }
        paymentFlowConfiguration?.let {
            paymentComponent.shouldCheckReturningUser = false
            paymentComponent.bankPickerRows = BankPickerRows.TWO
        }
        documentId?.let {
            viewModelScope.launch { giniHealth.setDocumentForReview(it) }
        }
    }

    fun loadPaymentProviderApps() = viewModelScope.launch {
        if (paymentComponent.paymentProviderAppsFlow.value is PaymentProviderAppsState.Loading || paymentComponent.paymentProviderAppsFlow.value is PaymentProviderAppsState.Success) return@launch
        paymentComponent.loadPaymentProviderApps()
    }

    fun addToBackStack(destination: DisplayedScreen) {
        backstack.add(destination)
        setDisplayedScreen()
    }

    fun popBackStack() {
        if (backstack.empty()) {
            return
        }
        backstack.pop()
        setDisplayedScreen()
    }

    fun getLastBackstackEntry() =
        if (backstack.isNotEmpty()) backstack.peek() else DisplayedScreen.Nothing

    private fun setDisplayedScreen() = viewModelScope.launch {
        giniInternalPaymentModule.emitSdkEvent(
            GiniInternalPaymentModule.InternalPaymentEvents.OnScreenDisplayed(
                getLastBackstackEntry().toInternalDisplayedScreen()
            )
        )
    }

    fun paymentProviderAppChanged(paymentProviderApp: PaymentProviderApp): Boolean {
        if (initialSelectedPaymentProvider?.paymentProvider?.id != paymentProviderApp.paymentProvider.id) {
            initialSelectedPaymentProvider = paymentProviderApp
            return true
        }
        return false
    }

    fun checkBankAppInstallState(paymentProviderApp: PaymentProviderApp) {
        if (paymentProviderApp.isInstalled() != initialSelectedPaymentProvider?.isInstalled()) {
            initialSelectedPaymentProvider = paymentProviderApp
        }
    }

    fun updatePaymentDetails(paymentDetails: PaymentDetails) {
        this.paymentDetails = paymentDetails
    }

    fun onPayment() = viewModelScope.launch {
        documentId?.let {
            sendFeedback()
        }
        val documentUri = resolveDocumentUri()

        paymentDetails?.let {
            giniInternalPaymentModule.onPayment(
                documentUri,
                initialSelectedPaymentProvider,
                it.toCommonPaymentDetails()
            )
        }
    }

    fun getPaymentProviderApp() = initialSelectedPaymentProvider

    fun emitShareWithStartedEvent() {
        paymentRequestFlow.value?.let {
            giniInternalPaymentModule.emitSdkEvent(
                GiniInternalPaymentModule.InternalPaymentEvents.OnFinishedWithPaymentRequestCreated(
                    it.id,
                    initialSelectedPaymentProvider?.name ?: ""
                )
            )
        }
        shareWithFlowStarted.tryEmit(true)
    }

    fun onPaymentButtonTapped() {
        checkNextStep(initialSelectedPaymentProvider, externalCacheDir, viewModelScope)
    }

    fun onForwardToSharePdfTapped(fileName: String) {
        documentId?.let {
            sendFeedback()
        }
        sharePdf(
            initialSelectedPaymentProvider,
            externalCacheDir,
            fileName,
            viewModelScope,
            paymentRequestFlow.value
        )
    }

    override suspend fun getPaymentRequest(): PaymentRequest {
        val documentUri = resolveDocumentUri()

        return giniInternalPaymentModule.getPaymentRequest(
            documentUri = documentUri,
            paymentProviderApp = initialSelectedPaymentProvider,
            paymentDetails = paymentDetails?.toCommonPaymentDetails()
        )
    }

    override suspend fun getPaymentRequestDocument(paymentRequest: PaymentRequest): Resource<ByteArray> =
        giniInternalPaymentModule.giniHealthAPI.documentManager.getPaymentRequestDocument(
            paymentRequest.id
        )

    override suspend fun getPaymentRequestImage(paymentRequest: PaymentRequest): Resource<ByteArray> =
        giniInternalPaymentModule.giniHealthAPI.documentManager.getPaymentRequestImage(
            paymentRequest.id
        )

    fun setFlowCancelled() {
        giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnCancelled)
    }

    private fun sendFeedback() {
        viewModelScope.launch {
            try {
                when (val documentResult = giniHealth.documentFlow.value) {
                    is ResultWrapper.Success -> paymentDetails?.extractions?.let { extractionsContainer ->
                        giniHealth.documentManager.sendFeedbackForExtractions(
                            documentResult.value,
                            extractionsContainer.specificExtractions,
                            extractionsContainer.compoundExtractions.withFeedback(paymentDetails!!)
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

    private suspend fun resolveDocumentUri(): String? {
        return when (val documentResult = giniHealth.documentFlow.value) {
            is ResultWrapper.Success -> documentResult.value.uri?.toString()
            is ResultWrapper.Error,
            is ResultWrapper.Loading -> null
        }
    }

    fun setExternalCacheDir(directory: File?) {
        externalCacheDir = directory
    }

    private fun handleInternalEvents(event: GiniInternalPaymentModule.InternalPaymentEvents) {
        when (event) {
            is GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred -> giniHealth.setOpenBankState(
                GiniHealth.PaymentState.Error(event.throwable),
                viewModelScope
            )

            is GiniInternalPaymentModule.InternalPaymentEvents.OnFinishedWithPaymentRequestCreated -> getPaymentProviderApp()?.let {
                giniHealth.setOpenBankState(
                    GiniHealth.PaymentState.Success(
                        net.gini.android.health.sdk.review.model.PaymentRequest(
                            event.paymentRequestId,
                            it
                        )
                    ), viewModelScope
                )
            }

            is GiniInternalPaymentModule.InternalPaymentEvents.NoAction -> giniHealth.setOpenBankState(
                GiniHealth.PaymentState.NoAction,
                viewModelScope
            )

            is GiniInternalPaymentModule.InternalPaymentEvents.OnLoading -> giniHealth.setOpenBankState(
                GiniHealth.PaymentState.Loading,
                viewModelScope
            )

            is GiniInternalPaymentModule.InternalPaymentEvents.OnCancelled -> giniHealth.setOpenBankState(
                GiniHealth.PaymentState.Cancel,
                viewModelScope
            )

            is GiniInternalPaymentModule.InternalPaymentEvents.OnScreenDisplayed -> giniHealth.setDisplayedScreen(
                event.displayedScreen
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    class PaymentFlowViewModelFactory(
        private val giniHealth: GiniHealth,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return PaymentFlowViewModel(handle, giniHealth) as T
        }
    }


    override fun backCalled() {
        _backButtonEvent.tryEmit(null)
    }
}