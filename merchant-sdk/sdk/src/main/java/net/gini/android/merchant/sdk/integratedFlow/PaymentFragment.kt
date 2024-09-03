package net.gini.android.merchant.sdk.integratedFlow

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
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.internal.payment.utils.PaymentNextStep
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.createShareWithPendingIntent
import net.gini.android.internal.payment.utils.extensions.showInstallAppBottomSheet
import net.gini.android.internal.payment.utils.extensions.showOpenWithBottomSheet
import net.gini.android.internal.payment.utils.extensions.startSharePdfIntent
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.merchant.sdk.databinding.GmsFragmentMerchantBinding
import net.gini.android.merchant.sdk.moreinformation.MoreInformationFragment
import net.gini.android.merchant.sdk.paymentComponentBottomSheet.PaymentComponentBottomSheet
import net.gini.android.merchant.sdk.review.reviewBottomSheet.ReviewBottomSheet
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.extensions.add
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme
import net.gini.android.merchant.sdk.util.wrappedWithGiniMerchantTheme
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
     */
    val shouldShowReviewFragment: Boolean = false,

    /**
     * If set to `true`, the errors will be handled internally and snackbars will be shown for errors.
     * If set to `false`, errors will be ignored by the [PaymentFragment] and the [ReviewBottomSheet]. In this case the flows exposed by [GiniMerchant] should be observed for errors.
     *
     * Default value is `true`.
     */
    val shouldHandleErrorsInternally: Boolean = true,

    /**
     * If set to `true`, the [Amount] field of shown as part of the [ReviewBottomSheet] will be editable.
     * If set to `false`, the [Amount] field will be read-only.
     *
     * Default value is `false`
     */
    internal val isAmountFieldEditable: Boolean = true,

    /**
     * If set to `true` and the user is a returning one, the `Select your bank to pay` and `More Information` labels will be hidden
     * If set to `false`, the labels will always be displayed
     *
     * Default value is `false`
     */
    internal val checkForReturningUser: Boolean = false,

    /**
     * If set to `true`, the [PaymentComponent] will be displayed on 2 rows
     * If set to `false`, the [PaymentComponent] will be displayed in a single row
     *
     * Default value is `true`
     */
    internal val paymentComponentOnTwoRows: Boolean = true

): Parcelable

/**
 * The [PaymentFragment] provides a container for all screens that should be displayed for the user
 * during the payment process (eg. [PaymentComponentBottomSheet], [BankSelectionBottomSheet], [ReviewBottomSheet]).
 *
 * It handles the display logic for all screens. A new instance can be created using the [GiniMerchant.createFragment] method.
 */
class PaymentFragment private constructor(
    private val viewModelFactory: ViewModelProvider.Factory? = null) : Fragment() {

    constructor(): this(null)
    private var binding: GmsFragmentMerchantBinding by autoCleared()
    private val viewModel by viewModels<PaymentFlowViewModel> {
        viewModelFactory ?: object : ViewModelProvider.Factory {}
    }
    private var snackbar: Snackbar? = null
    private var shareWithEventBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.emitShareWithStartedEvent()
        }
    }

    private val paymentComponentListener = object: PaymentComponent.Listener {
        override fun onMoreInformationClicked() {
            viewModel.addToBackStack(DisplayedScreen.MoreInformationFragment)
            childFragmentManager.add(
                containerId = binding.gmsFragmentContainerView.id,
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

        override fun onPayInvoiceClicked(documentId: String) {
            handlePayFlow()
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniMerchantTheme(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModelFactory == null) {
            // When the viewModelFactory is not available we are in an unrecoverable state
            // (most likely after an activity restart) and we should remove this fragment which is the container (root)
            // for all our other fragments. This returns the user to the client app's previous screen.
            parentFragmentManager.commit {
                remove(this@PaymentFragment)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GmsFragmentMerchantBinding.inflate(inflater, container, false)

        // If ReviewFragment will be shown, it will hande the opening of the bank app. Otherwise, listen for the openBankState event and handle it here
        binding.setStateListeners()

        viewModel.paymentComponent.listener = paymentComponentListener

        viewModel.loadPaymentProviderApps()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    requireActivity().registerReceiver(shareWithEventBroadcastReceiver, IntentFilter().also { it.addAction(GiniMerchant.SHARE_WITH_INTENT_FILTER) },
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
                    }
                }
                launch {
                    viewModel.backButtonEvent.collect {
                        handleBackFlow()
                    }
                }
            }
        }
        if (viewModel.getLastBackstackEntry() is DisplayedScreen.Nothing) {
            showPaymentComponentBottomSheet()
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

    private fun GmsFragmentMerchantBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.giniMerchant.eventsFlow.collect {
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

    private fun GmsFragmentMerchantBinding.handleError(text: String, onRetry: () -> Unit) {
        if (viewModel.paymentFlowConfiguration?.shouldHandleErrorsInternally == true) {
            showSnackbar(text, onRetry)
        }
    }

    private fun GmsFragmentMerchantBinding.showSnackbar(text: String, onRetry: () -> Unit) {
        val context = requireContext().wrappedWithGiniMerchantTheme()
        snackbar = Snackbar.make(context, root, text, Snackbar.LENGTH_INDEFINITE).apply {
            setTextMaxLines(2)
            setAction(getString(R.string.gms_snackbar_retry)) { onRetry() }
            show()
        }
    }

    internal fun handleBackFlow() {
        childFragmentManager.popBackStackImmediate()
        viewModel.popBackStack()

        if (childFragmentManager.backStackEntryCount == 0) {
            when (viewModel.getLastBackstackEntry()) {
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
                        viewModel.paymentFlowConfiguration?.shouldShowReviewFragment ?: false,
                        viewModel
                    ).show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
                }
                DisplayedScreen.Nothing -> viewModel.giniMerchant.setFlowCancelled()
                else -> {

                }
            }
        }
    }

    @VisibleForTesting
    internal fun handlePayFlow() {
        if (viewModel.paymentFlowConfiguration?.shouldShowReviewFragment == true) {
            showReviewBottomDialog()
            return
        }

        viewModel.onPaymentButtonTapped(requireContext().externalCacheDir)
    }

    internal fun showReviewBottomDialog() {
        viewModel.addToBackStack(DisplayedScreen.ReviewBottomSheet)
        val reviewBottomSheet = ReviewBottomSheet.newInstance(
            giniMerchant = viewModel.giniMerchant,
            backListener = viewModel,
            configuration = ReviewConfiguration(
                handleErrorsInternally = viewModel.paymentFlowConfiguration?.shouldHandleErrorsInternally == true,
                showCloseButton = true,
                isAmountFieldEditable = viewModel.paymentFlowConfiguration?.isAmountFieldEditable == true
            ),
            listener = object: ReviewViewListener {
                override fun onPaymentButtonTapped(paymentDetails: PaymentDetails) {
                    viewModel.updatePaymentDetails(paymentDetails)
                    viewModel.onPaymentButtonTapped(requireContext().externalCacheDir)
                }
            },
            paymentComponent = viewModel.paymentComponent
        )
        reviewBottomSheet.show(childFragmentManager, ReviewBottomSheet::class.java.name)
    }

    @VisibleForTesting
    internal fun showPaymentComponentBottomSheet() {
        val paymentComponentBottomSheet = PaymentComponentBottomSheet.newInstance(
            viewModel.paymentComponent,
            reviewFragmentShown = viewModel.paymentFlowConfiguration?.shouldShowReviewFragment ?: false,
            backListener = viewModel
        )
        paymentComponentBottomSheet.show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
        viewModel.addToBackStack(DisplayedScreen.PaymentComponentBottomSheet)
    }

    private fun GmsFragmentMerchantBinding.handleSDKEvent(sdkEvent: GiniMerchant.MerchantSDKEvents) {
        when (sdkEvent) {
            is GiniMerchant.MerchantSDKEvents.OnFinishedWithPaymentRequestCreated -> {
                if (viewModel.getLastBackstackEntry() is DisplayedScreen.ShareSheet) return
                try {
                    val intent = viewModel.getPaymentProviderApp()?.getIntent(sdkEvent.paymentRequestId)
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        handleError(getString(R.string.gms_generic_error_message)) { viewModel.onPaymentButtonTapped(requireContext().externalCacheDir) }
                    }
                } catch (exception: ActivityNotFoundException) {
                    handleError(getString(R.string.gms_generic_error_message)) { viewModel.onPaymentButtonTapped(requireContext().externalCacheDir) }
                }
            }
            is GiniMerchant.MerchantSDKEvents.OnErrorOccurred -> {
                binding.loading.isVisible = false
                handleError(getString(R.string.gms_generic_error_message)) { viewModel.onPaymentButtonTapped(requireContext().externalCacheDir) }
            }
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
            PaymentNextStep.ShowOpenWithSheet -> viewModel.getPaymentProviderApp()?.let { showOpenWithDialog(it) }
            PaymentNextStep.ShowInstallApp -> showInstallAppDialog()
            is PaymentNextStep.OpenSharePdf -> {
                binding.loading.isVisible = false
                startSharePdfIntent(paymentNextStep.file, requireContext().createShareWithPendingIntent())
                startSharePdfIntent(paymentNextStep.file)
                viewModel.addToBackStack(DisplayedScreen.ShareSheet)
            }
        }
    }

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
            backListener = viewModel
        ) {
            viewModel.onForwardToSharePdfTapped(requireContext().externalCacheDir)
        }
        viewModel.incrementOpenWithCounter(viewModel.viewModelScope, paymentProviderApp.paymentProvider.id)
        viewModel.addToBackStack(DisplayedScreen.OpenWithBottomSheet)
    }

    override fun onStop() {
        requireActivity().unregisterReceiver(shareWithEventBroadcastReceiver)
        super.onStop()
    }

    companion object {
        fun newInstance(giniMerchant: GiniMerchant, paymentDetails: PaymentDetails, paymentFlowConfiguration: PaymentFlowConfiguration,
                        viewModelFactory : ViewModelProvider.Factory = PaymentFlowViewModel.Factory(paymentDetails, paymentFlowConfiguration, giniMerchant)) = PaymentFragment(viewModelFactory)

    }
}

