package net.gini.android.health.sdk.integratedFlow

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsFragmentHealthBinding
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.review.ReviewFragmentListener
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.toCommonPaymentDetails
import net.gini.android.health.sdk.util.DisplayedScreen
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.bankselection.BankSelectionBottomSheet
import net.gini.android.internal.payment.moreinformation.MoreInformationFragment
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentComponentBottomSheet.PaymentComponentBottomSheet
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewBottomSheet.ReviewBottomSheet
import net.gini.android.internal.payment.review.reviewComponent.ReviewFields
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.internal.payment.utils.PaymentNextStep
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.add
import net.gini.android.internal.payment.utils.extensions.createShareWithPendingIntent
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.isValidPdfName
import net.gini.android.internal.payment.utils.extensions.showInstallAppBottomSheet
import net.gini.android.internal.payment.utils.extensions.showOpenWithBottomSheet
import net.gini.android.internal.payment.utils.extensions.startSharePdfIntent
import net.gini.android.internal.payment.utils.extensions.wrappedWithGiniPaymentThemeAndLocale
import org.jetbrains.annotations.VisibleForTesting

/**
 * Configuration for the payment flow.
 */
@Parcelize
data class PaymentFlowConfiguration(
    /**
     * If set to `true`, the [ReviewBottomSheet] will be shown before initiating payment.
     * If set to `false`, the payment request process will be executed under the hood before redirecting to the selected payment provider
     *
     * Default value is `false`
     *
     * Note: only taken into consideration for payments initiated without [documentId].
     */
    val shouldShowReviewBottomDialog: Boolean = false,

    /**
     * If set to `true`, the errors will be handled internally and snackbars will be shown for errors.
     * If set to `false`, errors will be ignored by the [PaymentFragment] and the [ReviewBottomSheet]. In this case the flows exposed by [GiniHealth] should be observed for errors.
     *
     * Default value is `true`.
     */
    val shouldHandleErrorsInternally: Boolean = true,

    /**
     * Set to `true` to show a close button.
     *
     * Default value is `false`.
     *
     * Note: Only taken into consideration for payments initiated with [documentId]
     */
    val showCloseButtonOnReviewFragment: Boolean = false

): Parcelable

/**
 * The [PaymentFragment] provides a container for all screens that should be displayed for the user
 * during the payment process (eg. [PaymentComponentBottomSheet], [BankSelectionBottomSheet], [ReviewBottomSheet]).
 *
 * It handles the display logic for all screens. A new instance can be created using the [GiniHealth.getPaymentFragmentWithDocument] method for
 * payments with [documentId] and using the [GiniHealth.getPaymentFragmentWithoutDocument] for payments without a document.
 */
class PaymentFragment private constructor(
    private val viewModelFactory: ViewModelProvider.Factory? = null) : Fragment() {

    constructor(): this(null)
    private var binding: GhsFragmentHealthBinding by autoCleared()
    private val viewModel by viewModels<PaymentFlowViewModel> {
        viewModelFactory ?: object : ViewModelProvider.Factory {}
    }
    private var snackbar: Snackbar? = null
    private var shareWithEventBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.emitShareWithStartedEvent()
        }
    }

    /**
     * Listener for the [ReviewView] of the [ReviewBottomSheet].
     * Forwards [Pay] button tap events and
     * show [BankSelectionBottomSheet] button tap events.
     *
     * Internal use only.
     */
    @VisibleForTesting
    internal var reviewViewListener: ReviewViewListener = object: ReviewViewListener {
        override fun onPaymentButtonTapped(paymentDetails: net.gini.android.internal.payment.api.model.PaymentDetails) {
            viewModel.updatePaymentDetails(PaymentDetails(recipient = paymentDetails.recipient, iban = paymentDetails.iban, amount = paymentDetails.amount, purpose = paymentDetails.purpose))
            viewModel.onPaymentButtonTapped()
        }

        override fun onSelectBankButtonTapped() {
            viewModel.paymentComponent.listener?.onBankPickerClicked()
        }
    }

    /**
     * Listener for the [ReviewFragment], to get close button tap event
     * and show [BankSelectionBottomSheet] button tap event.
     *
     * Internal use only.
     */
    @VisibleForTesting
    internal var reviewFragmentListener: ReviewFragmentListener = object: ReviewFragmentListener {
        override fun onCloseReview() {
            viewModel.setFlowCancelled()
        }

        override fun onToTheBankButtonClicked(
            paymentProviderName: String,
            paymentDetails: PaymentDetails,
        ) {
            viewModel.paymentDetails = paymentDetails
            viewModel.onPaymentButtonTapped()
        }
    }

    /**
     * Listener for the [PaymentComponent].
     * Forwards show [MoreInformationFragment] tap events, show [BankSelectionBottomSheet] tap events
     * and [Pay] button tap events.
     */
    private val paymentComponentListener = object: PaymentComponent.Listener {
        override fun onMoreInformationClicked() {
            viewModel.addToBackStack(DisplayedScreen.MoreInformationFragment)
            childFragmentManager.add(
                containerId = binding.ghsFragmentContainerView.id,
                fragment = MoreInformationFragment.newInstance(viewModel.paymentComponent, viewModel),
                addToBackStack = true
            )
        }

        override fun onBankPickerClicked() {
            viewModel.paymentComponent.let {
                viewModel.addToBackStack(DisplayedScreen.BankSelectionBottomSheet)
                BankSelectionBottomSheet.newInstance(it, backListener = viewModel).show(childFragmentManager, BankSelectionBottomSheet::class.java.name)
            }
        }

        override fun onPayInvoiceClicked(documentId: String?) {
            handlePayFlow()
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniPaymentThemeAndLocale(inflater, viewModel.paymentComponent.getGiniPaymentLanguage(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GhsFragmentHealthBinding.inflate(inflater, container, false)

        // If ReviewFragment will be shown, it will hande the opening of the bank app. Otherwise, listen for the openBankState event and handle it here
        binding.setStateListeners()

        viewModel.paymentComponent.listener = paymentComponentListener
        viewModel.loadPaymentProviderApps()
        viewModel.setExternalCacheDir(requireContext().externalCacheDir)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    requireActivity().registerReceiver(shareWithEventBroadcastReceiver, IntentFilter().also { it.addAction(GiniInternalPaymentModule.SHARE_WITH_INTENT_FILTER) },
                        Context.RECEIVER_NOT_EXPORTED)
                }
                launch {
                    viewModel.paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
                }
                launch {
                    viewModel.paymentComponent.selectedPaymentProviderAppFlow.collect {
                        if (it is SelectedPaymentProviderAppState.AppSelected) {
                            viewModel.paymentProviderAppChanged(it.paymentProviderApp)
                            viewModel.checkBankAppInstallState(it.paymentProviderApp)
                        }
                    }
                }
                launch {
                    if (viewModel.getLastBackstackEntry() == DisplayedScreen.ShareSheet) {
                        viewModel.popBackStack()
                        viewModel.getPaymentProviderApp()?.let {
                            showOpenWithDialog(it)
                        }
                    }
                }
                launch {
                    viewModel.backButtonEvent.collect {
                        handleBackFlow()
                    }
                }
            }
        }

        if (viewModel.getLastBackstackEntry() == DisplayedScreen.Nothing) {
            setupStartingScreen()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackFlow()
            }
        })
    }

    override fun onDetach() {
        snackbar?.dismiss()
        super.onDetach()
    }

    /**
     * Starting screen logic:
     * 1. if it`s a new user, show [PaymentComponentBottomSheet]
     * 2. if the user is a returning one:
     *  a. if flow was started with a documentId, show [ReviewFragment]
     *  b. if flow was started without document and review is enabled, show [ReviewBottomSheet]
     *  c. else show [PaymentComponentBottomSheet]
     */
    private fun setupStartingScreen() {
        if (viewModel.giniInternalPaymentModule.getReturningUser()) {
            if (viewModel.documentId != null) {
                showReviewFragment()
            } else if (viewModel.paymentFlowConfiguration?.shouldShowReviewBottomDialog == true){
                showReviewBottomDialog()
            } else {
                showPaymentComponentBottomSheet()
            }
        } else {
            showPaymentComponentBottomSheet()
        }
    }

    private fun GhsFragmentHealthBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.giniInternalPaymentModule.eventsFlow.collect {
                        handleSDKEvent(it)
                    }
                }

                launch {
                    viewModel.paymentNextStep.collect {
                        handlePaymentNextStep(it)
                    }
                }
            }
        }
    }

    private fun GhsFragmentHealthBinding.handleError(text: String, onRetry: () -> Unit) {
        if (viewModel.paymentFlowConfiguration?.shouldHandleErrorsInternally == true) {
            showSnackbar(text, onRetry)
        }
    }

    private fun GhsFragmentHealthBinding.showSnackbar(text: String, onRetry: () -> Unit) {
        val context = requireContext().wrappedWithGiniPaymentThemeAndLocale(viewModel.paymentComponent.getGiniPaymentLanguage(requireContext()))
        snackbar = Snackbar.make(context, root, text, Snackbar.LENGTH_INDEFINITE).apply {
            setTextMaxLines(2)
            setAction(getString(R.string.ghs_snackbar_retry)) { onRetry() }
            show()
        }
    }

    private fun handleBackFlow() {
        viewModel.popBackStack()

        if (childFragmentManager.backStackEntryCount > 0) {
            if (viewModel.getLastBackstackEntry() == DisplayedScreen.Nothing) {
                viewModel.setFlowCancelled()
                return
            } else if (viewModel.getLastBackstackEntry() != DisplayedScreen.ReviewFragment) {
                childFragmentManager.popBackStackImmediate()
            }
        }

        if (childFragmentManager.backStackEntryCount == 0 || childFragmentManager.fragments.last() is ReviewFragment) {
            when (viewModel.getLastBackstackEntry()) {
                DisplayedScreen.ReviewBottomSheet -> {
                    createReviewBottomSheet().also { it.show(childFragmentManager, ReviewBottomSheet::class.java.name) }
                }
                DisplayedScreen.BankSelectionBottomSheet -> {
                    viewModel.paymentComponent.let {
                        BankSelectionBottomSheet.newInstance(it, viewModel).show(childFragmentManager, BankSelectionBottomSheet::class.java.name)
                    }
                }
                DisplayedScreen.PaymentComponentBottomSheet -> {
                    if (childFragmentManager.fragments.any { it is PaymentComponentBottomSheet }) {
                        return
                    }
                    PaymentComponentBottomSheet.newInstance(
                        viewModel.paymentComponent,
                        if (viewModel.documentId != null) true else viewModel.paymentFlowConfiguration?.shouldShowReviewBottomDialog ?: false,
                        viewModel
                    ).show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
                }
                DisplayedScreen.Nothing -> viewModel.setFlowCancelled()
                DisplayedScreen.ReviewFragment -> if (viewModel.giniInternalPaymentModule.getReturningUser()) viewModel.setFlowCancelled() else {
                    PaymentComponentBottomSheet.newInstance(
                        viewModel.paymentComponent,
                        false,
                        viewModel
                    ).show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
                }
                else -> {

                }
            }
        }
    }

    @VisibleForTesting
    internal fun handlePayFlow() {
        if (viewModel.documentId != null) {
            showReviewFragment()
            return
        }

        if (viewModel.paymentFlowConfiguration?.shouldShowReviewBottomDialog == true) {
            showReviewBottomDialog()
            return
        }

        viewModel.onPaymentButtonTapped()
    }

    @VisibleForTesting
    internal fun showReviewBottomDialog() {
        viewModel.addToBackStack(DisplayedScreen.ReviewBottomSheet)
        createReviewBottomSheet().also { it.show(childFragmentManager, ReviewBottomSheet::class.java.name) }
    }

    @VisibleForTesting
    internal fun showReviewFragment() {
        viewModel.addToBackStack(DisplayedScreen.ReviewFragment)
        val reviewFragment = ReviewFragment.newInstance(
            giniHealth = viewModel.giniHealth,
            paymentComponent = viewModel.giniInternalPaymentModule.paymentComponent,
            listener = reviewFragmentListener,
            documentId = viewModel.documentId ?: "",
            configuration = ReviewConfiguration(
                handleErrorsInternally = viewModel.paymentFlowConfiguration?.shouldHandleErrorsInternally == true,
                selectBankButtonVisible = true,
            ),
            shouldShowCloseButton = viewModel.paymentFlowConfiguration?.showCloseButtonOnReviewFragment ?: false
        )
        childFragmentManager.beginTransaction()
            .add(R.id.ghs_fragment_container_view, reviewFragment, reviewFragment::class.simpleName)
            .addToBackStack(reviewFragment::class.java.name)
            .commit()
    }

    @VisibleForTesting
    internal fun showPaymentComponentBottomSheet() {
        val paymentComponentBottomSheet = PaymentComponentBottomSheet.newInstance(
            viewModel.paymentComponent,
            reviewFragmentShown = if (viewModel.documentId != null) true else viewModel.paymentFlowConfiguration?.shouldShowReviewBottomDialog ?: false,
            backListener = viewModel
        )
        paymentComponentBottomSheet.show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
        viewModel.addToBackStack(DisplayedScreen.PaymentComponentBottomSheet)
    }

    private fun createReviewBottomSheet() = ReviewBottomSheet.newInstance(
            backListener = viewModel,
            configuration = ReviewConfiguration(
                handleErrorsInternally = viewModel.paymentFlowConfiguration?.shouldHandleErrorsInternally == true,
                editableFields = listOf(ReviewFields.AMOUNT),
                selectBankButtonVisible = true
            ),
            listener = reviewViewListener,
            giniInternalPaymentModule = viewModel.giniInternalPaymentModule,
        )

    private fun showInstallAppDialog() {
        childFragmentManager.showInstallAppBottomSheet(
            paymentComponent = viewModel.paymentComponent,
            backListener = viewModel
        ) {
            viewModel.onPayment()
        }
        viewModel.addToBackStack(DisplayedScreen.InstallAppBottomSheet)
    }

    private fun showOpenWithDialog(paymentProviderApp: PaymentProviderApp) {
        childFragmentManager.showOpenWithBottomSheet(
            paymentProviderApp = paymentProviderApp,
            paymentComponent = viewModel.paymentComponent,
            backListener = viewModel,
            paymentDetails = viewModel.paymentDetails?.toCommonPaymentDetails(),
            paymentRequestId = viewModel.paymentRequestFlow.value?.id ?: ""
        ) {
            val overriddenPdfName = getString(net.gini.android.internal.payment.R.string.gps_payment_request_pdf_name)
            val pdfName = if (overriddenPdfName.isValidPdfName()) overriddenPdfName else getString(net.gini.android.internal.payment.R.string.gps_payment_request_pdf_name_default)
            viewModel.onForwardToSharePdfTapped(pdfName)
        }
    }

    private fun GhsFragmentHealthBinding.handleSDKEvent(sdkEvent: GiniInternalPaymentModule.InternalPaymentEvents) {
        when (sdkEvent) {
            is GiniInternalPaymentModule.InternalPaymentEvents.OnFinishedWithPaymentRequestCreated -> {
                if (viewModel.getLastBackstackEntry() is DisplayedScreen.ShareSheet) {
                        return
                    }
                try {
                    val intent = viewModel.getPaymentProviderApp()?.getIntent(sdkEvent.paymentRequestId)
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        handleError(getString(R.string.ghs_generic_error_message)) { viewModel.onPaymentButtonTapped() }
                    }
                } catch (exception: ActivityNotFoundException) {
                    handleError(getString(R.string.ghs_generic_error_message)) { viewModel.onPaymentButtonTapped() }
                }
            }
            is GiniInternalPaymentModule.InternalPaymentEvents.OnErrorOccurred -> {
                binding.loading.isVisible = false
                handleError(getString(R.string.ghs_generic_error_message)) { viewModel.onPaymentButtonTapped() }
            }
            GiniInternalPaymentModule.InternalPaymentEvents.OnCancelled -> viewModel.giniHealth.setOpenBankState(GiniHealth.PaymentState.Cancel, viewModel.viewModelScope)
            else -> {
            }
        }
    }

    private fun handlePaymentNextStep(paymentNextStep: PaymentNextStep) {
        when (paymentNextStep) {
            is PaymentNextStep.SetLoadingVisibility -> {
                binding.loading.isVisible = paymentNextStep.isVisible
            }
            PaymentNextStep.RedirectToBank -> {
                viewModel.onPayment()
            }
            PaymentNextStep.ShowInstallApp -> showInstallAppDialog()
            is PaymentNextStep.ShowOpenWithSheet -> viewModel.getPaymentProviderApp()?.let {
                showOpenWithDialog(it)
                viewModel.addToBackStack(DisplayedScreen.OpenWithBottomSheet)
            }
            is PaymentNextStep.OpenSharePdf -> {
                binding.loading.isVisible = false
                startSharePdfIntent(paymentNextStep.file, requireContext().createShareWithPendingIntent())
                viewModel.addToBackStack(DisplayedScreen.ShareSheet)
            }
        }
    }

    override fun onStop() {
        requireActivity().unregisterReceiver(shareWithEventBroadcastReceiver)
        super.onStop()
    }

    companion object {
        /**
         * Creates an instance of [PaymentFragment] for the case when payment is initiated with payment details
         *
         * @param giniHealth The [GiniHealth] instance
         * @param paymentDetails the [PaymentDetails] with which the payment will be initiated
         * @param paymentFlowConfiguration The [PaymentFlowConfiguration]
         */
        fun newInstance(giniHealth: GiniHealth, paymentDetails: PaymentDetails, paymentFlowConfiguration: PaymentFlowConfiguration,
                        viewModelFactory : ViewModelProvider.Factory =
                            PaymentFlowViewModel.Factory(
                                paymentDetails,
                                null,
                                paymentFlowConfiguration,
                                giniHealth
                            )
        ) = PaymentFragment(viewModelFactory)

        /**
         * Creates an instance of [PaymentFragment] for the case when payment is initiated with a documentId
         *
         * @param giniHealth The [GiniHealth] instance
         * @param documentId the id of the document to be paid
         * @param paymentFlowConfiguration The [PaymentFlowConfiguration]
         */
        fun newInstance(giniHealth: GiniHealth, documentId: String, paymentFlowConfiguration: PaymentFlowConfiguration,
                        viewModelFactory : ViewModelProvider.Factory =
                            PaymentFlowViewModel.Factory(
                                null,
                                documentId,
                                paymentFlowConfiguration,
                                giniHealth
                            )
        ) = PaymentFragment(viewModelFactory)
    }
}

