package net.gini.android.merchant.sdk.integratedFlow

import android.content.ActivityNotFoundException
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
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.merchant.sdk.databinding.GmsFragmentMerchantBinding
import net.gini.android.merchant.sdk.moreinformation.MoreInformationFragment
import net.gini.android.merchant.sdk.paymentComponentBottomSheet.PaymentComponentBottomSheet
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.review.ReviewConfiguration
import net.gini.android.merchant.sdk.review.ReviewFragment
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.util.BackListener
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.PaymentNextStep
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.add
import net.gini.android.merchant.sdk.util.extensions.showInstallAppBottomSheet
import net.gini.android.merchant.sdk.util.extensions.showOpenWithBottomSheet
import net.gini.android.merchant.sdk.util.extensions.startSharePdfIntent
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme
import net.gini.android.merchant.sdk.util.wrappedWithGiniMerchantTheme
import org.jetbrains.annotations.VisibleForTesting

/**
 * Configuration for the integrated payment flow
 */
@Parcelize
data class PaymentFlowConfiguration(
    /**
     * If set to `true`, the [ReviewFragment] will be shown before initiating payment.
     * If set to `false`, the payment request process will be executed under the hood before redirecting to the selected payment provider
     *
     * Default value is `false`
     */
    val shouldShowReviewFragment: Boolean = false,

    /**
     * If set to `true`, the errors will be handled internally and snackbars will be shown for errors.
     * If set to `false`, errors will be ignored by the [PaymentFlowFragment] and the [ReviewFragment]. In this case the flows exposed by [GiniMerchant] should be observed for errors.
     *
     * Default value is `true`.
     */
    val shouldHandleErrorsInternally: Boolean = true,

    /**
     * If set to `true`, the [Amount] field of shown as part of the [ReviewFragment] will be editable.
     * If set to `false`, the [Amount] field will be read-only.
     *
     * Default value is `false`
     */
    val isAmountFieldEditable: Boolean = true
): Parcelable

/**
 * The [PaymentFlowFragment] provides a container for all screens that should be displayed for the user
 * during the payment process (eg. [PaymentComponentBottomSheet], [BankSelectionBottomSheet], [ReviewFragment]).
 *
 * It handles the display logic for all screens. A new instance can be created using the [PaymentComponent.getContainerFragment] method.
 */
class PaymentFlowFragment private constructor(
    private val viewModelFactory: ViewModelProvider.Factory? = null) : Fragment(),
    BackListener {

    constructor(): this(null)
    private var binding: GmsFragmentMerchantBinding by autoCleared()
    private val viewModel by viewModels<PaymentFlowViewModel> {
        viewModelFactory ?: object : ViewModelProvider.Factory {}
    }
    private var snackbar: Snackbar? = null

    private val paymentComponentListener = object: PaymentComponent.Listener {
        override fun onMoreInformationClicked() {
            viewModel.addToBackStack(DisplayedScreen.MoreInformationFragment)
            childFragmentManager.add(
                containerId = binding.gmsFragmentContainerView.id,
                fragment = MoreInformationFragment.newInstance(viewModel.paymentComponent, this@PaymentFlowFragment),
                addToBackStack = true
            )
        }

        override fun onBankPickerClicked() {
            viewModel.paymentComponent?.let {
                viewModel.addToBackStack(DisplayedScreen.BankSelectionBottomSheet)
                BankSelectionBottomSheet.newInstance(it, backListener = this@PaymentFlowFragment).show(childFragmentManager, BankSelectionBottomSheet::class.java.name)
            }
        }

        override fun onPayInvoiceClicked(documentId: String) {
            handlePayFlow(documentId)
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniMerchantTheme(inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = GmsFragmentMerchantBinding.inflate(inflater, container, false)

        // If ReviewFragment will be shown, it will hande the opening of the bank app. Otherwise, listen for the openBankState event and handle it here
        if (viewModel.paymentFlowConfiguration?.shouldShowReviewFragment == false) {
            binding.setStateListeners()
        }

        viewModel.paymentComponent.listener = paymentComponentListener
        viewModel.openWithPreferences = OpenWithPreferences(requireContext())
        viewModel.loadPaymentDetails()
        viewModel.loadPaymentProviderApps()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
                }
                launch {
                    viewModel.paymentComponent.selectedPaymentProviderAppFlow.collect {
                        if (it is SelectedPaymentProviderAppState.AppSelected) {
                            if (viewModel.paymentProviderAppChanged(it.paymentProviderApp) && viewModel.getLastBackstackEntry() is DisplayedScreen.BankSelectionBottomSheet) {
                                handleBackFlow()
                            } else {
                                viewModel.checkBankAppInstallState(it.paymentProviderApp)
                            }
                        }
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

    private fun handleBackFlow() {
        childFragmentManager.popBackStackImmediate()
        viewModel.popBackStack()

        if (childFragmentManager.backStackEntryCount == 0) {
            when (viewModel.getLastBackstackEntry()) {
                DisplayedScreen.BankSelectionBottomSheet -> {
                    viewModel.paymentComponent.let {
                        BankSelectionBottomSheet.newInstance(it, this).show(childFragmentManager, BankSelectionBottomSheet::class.java.name)
                    }
                }
                DisplayedScreen.PaymentComponentBottomSheet -> PaymentComponentBottomSheet.newInstance(viewModel.paymentComponent, documentId = viewModel.documentId,this).show(childFragmentManager, PaymentComponentBottomSheet::class.java.name)
                else -> {

                }
            }
        }
    }

    @VisibleForTesting
    internal fun handlePayFlow(documentId: String) {
        if (viewModel.paymentFlowConfiguration?.shouldShowReviewFragment == true) {
            showReviewFragment(documentId)
            return
        }

        viewModel.onPaymentButtonTapped(requireContext().externalCacheDir)
    }

    @VisibleForTesting
    internal fun showReviewFragment(documentId: String) {
        viewModel.addToBackStack(DisplayedScreen.ReviewFragment)
        val reviewFragment = viewModel.giniMerchant.getPaymentReviewFragment(documentId, ReviewConfiguration(
            handleErrorsInternally = viewModel.paymentFlowConfiguration?.shouldHandleErrorsInternally == true,
            showCloseButton = true,
            isAmountFieldEditable = viewModel.paymentFlowConfiguration?.isAmountFieldEditable == true
        ))
        reviewFragment.let {
            childFragmentManager.add(
                containerId = binding.gmsFragmentContainerView.id,
                fragment = it,
                addToBackStack = true
            )
        }
    }

    @VisibleForTesting
    internal fun showPaymentComponentBottomSheet() {
        val paymentComponentBottomSheet = PaymentComponentBottomSheet.newInstance(
            viewModel.paymentComponent,
            documentId = viewModel.documentId,
            backListener = this@PaymentFlowFragment
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
                        viewModel.onBankOpened()
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
                startSharePdfIntent(paymentNextStep.file)
                viewModel.addToBackStack(DisplayedScreen.ShareSheet)
                viewModel.emitFinishEvent(paymentNextStep.paymentRequest)
            }
        }
    }

    private fun showInstallAppDialog() {
        childFragmentManager.showInstallAppBottomSheet(
            paymentComponent = viewModel.paymentComponent,
            backListener = this
        ) {
            viewModel.onPayment()
        }
        viewModel.addToBackStack(DisplayedScreen.InstallAppBottomSheet)
    }

    private fun showOpenWithDialog(paymentProviderApp: PaymentProviderApp) {
        requireActivity().supportFragmentManager.showOpenWithBottomSheet(
            paymentProviderApp = paymentProviderApp,
            backListener = this
        ) {
            viewModel.onForwardToSharePdfTapped(requireContext().externalCacheDir)
        }
        viewModel.incrementOpenWithCounter(viewModel.viewModelScope, paymentProviderApp.paymentProvider.id)
        viewModel.addToBackStack(DisplayedScreen.OpenWithBottomSheet)
    }

    override fun backCalled() {
        handleBackFlow()
    }

    companion object {
        fun newInstance(giniMerchant: GiniMerchant, documentId: String, paymentFlowConfiguration: PaymentFlowConfiguration,
                        viewModelFactory : ViewModelProvider.Factory = PaymentFlowViewModel.Factory(giniMerchant.paymentComponent, documentId, paymentFlowConfiguration, giniMerchant)) = PaymentFlowFragment(viewModelFactory)
    }
}
