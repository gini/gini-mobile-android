package net.gini.android.merchant.sdk.review

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.math.MathUtils.lerp
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.chrisbanes.insetter.applyInsetter
import dev.chrisbanes.insetter.windowInsetTypesOf
import kotlinx.coroutines.launch
import net.gini.android.core.api.models.Document
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.databinding.GmsFragmentReviewBinding
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.preferences.UserPreferences
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.review.pager.DocumentPageAdapter
import net.gini.android.merchant.sdk.review.reviewComponent.ReviewViewListener
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import net.gini.android.merchant.sdk.util.PaymentNextStep
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.extensions.createShareWithPendingIntent
import net.gini.android.merchant.sdk.util.extensions.getFontScale
import net.gini.android.merchant.sdk.util.extensions.showInstallAppBottomSheet
import net.gini.android.merchant.sdk.util.extensions.showOpenWithBottomSheet
import net.gini.android.merchant.sdk.util.extensions.startSharePdfIntent
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme
import net.gini.android.merchant.sdk.util.hideKeyboard
import net.gini.android.merchant.sdk.util.wrappedWithGiniMerchantTheme

/**
 * Configuration for the [ReviewFragment].
 */
data class ReviewConfiguration(
    /**
     * If set to `true`, the [ReviewFragment] will handle errors internally and show snackbars for errors.
     * If set to `false`, errors will be ignored by the [ReviewFragment]. In this case the flows exposed by [GiniMerchant] should be observed for errors.
     *
     * Default value is `true`.
     */
    val handleErrorsInternally: Boolean = true,

    /**
     * Set to `true` to show a close button. Set a [ReviewFragmentListener] to be informed when the
     * button is pressed.
     *
     * Default value is `false`.
     */
    val showCloseButton: Boolean = false,

    /**
     * If set to `true`, the [Amount] field will be editable.
     * If set to `false` the [Amount] field will be read-only.
     *
     * Default value is `true`
     */
    val isAmountFieldEditable: Boolean = true
)

/**
 * Listener for [ReviewFragment] events.
 */
interface ReviewFragmentListener {
    /**
     * Called when the close button was pressed.
     */
    fun onCloseReview()

    /**
     * Called when the "to the bank" button was clicked.
     *
     * Collect the [GiniMerchant.openBankState] flow to get details about the payment request creation and about the
     * selected bank app.
     *
     * @param paymentProviderName the name of the selected payment provider
     */
    fun onToTheBankButtonClicked(paymentProviderName: String)
}

/**
 * The [ReviewFragment] displays an invoice’s pages and payment information extractions. It also lets users pay the
 * invoice with the bank they selected in the [BankSelectionBottomSheet].
 *
 * Instances can be created using the [PaymentComponent.getPaymentReviewFragment] method.
 */
class ReviewFragment private constructor(
    var listener: ReviewFragmentListener? = null,
    private val viewModelFactory: ViewModelProvider.Factory? = null,
) : Fragment() {

    constructor() : this(null)

    private val viewModel: ReviewViewModel by viewModels { viewModelFactory ?: object : ViewModelProvider.Factory {} }
    private var binding: GmsFragmentReviewBinding by autoCleared()
    private var documentPageAdapter: DocumentPageAdapter by autoCleared()
    private var isKeyboardShown = false

    private var shareWithEventBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.emitShareWithStartedEvent()
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniMerchantTheme(inflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        documentPageAdapter = DocumentPageAdapter(viewModel.giniMerchant)
        binding = GmsFragmentReviewBinding.inflate(inflater).apply {
            configureViews()
            configureOrientation()
            applyInsets()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val documentPagerHeight = savedInstanceState?.getInt(PAGER_HEIGHT, -1) ?: -1

        viewModel.userPreferences = UserPreferences(requireContext())
        viewModel.openWithPreferences = OpenWithPreferences(requireContext())
        viewModel.reviewComponent.paymentProviderApp.value?.paymentProvider?.id?.let { paymentProviderAppId ->
            viewModel.startObservingOpenWithCount(viewModel.viewModelScope, paymentProviderAppId)
        }
        viewModel.loadPaymentDetails()

        with(binding) {
            setStateListeners()
            setKeyboardAnimation()
            removePagerConstraintAndSetPreviousHeightIfNeeded(documentPagerHeight)
            gmsReviewView.reviewComponent = viewModel.reviewComponent
        }

        // Set info bar bottom margin programmatically to reuse radius dimension with negative sign
        binding.paymentDetailsInfoBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = -resources.getDimensionPixelSize(R.dimen.gms_medium_12)
        }
    }

    private fun GmsFragmentReviewBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    requireActivity().registerReceiver(shareWithEventBroadcastReceiver, IntentFilter().also { it.addAction(GiniMerchant.SHARE_WITH_INTENT_FILTER) }, Context.RECEIVER_NOT_EXPORTED)
                }
                launch {
                    viewModel.giniMerchant.documentFlow.collect { handleDocumentResult(it) }
                }
                launch {
                    viewModel.giniMerchant.paymentFlow.collect { handlePaymentResult(it) }
                }
                launch {
                    viewModel.giniMerchant.eventsFlow.collect { handlePaymentState(it) }
                }
                launch {
                    viewModel.isInfoBarVisible.collect { visible ->
                        if (visible) showInfoBar() else hideInfoBarAnimated()
                    }
                }
                launch {
                    viewModel.reviewComponent.paymentProviderApp.collect { paymentProviderApp ->
                        if (paymentProviderApp != null) {
                            setActionListeners()
                        }
                    }
                }
                launch {
                    viewModel.paymentNextStep.collect { paymentNextStep ->
                        handlePaymentNextStep(paymentNextStep)
                    }
                }
            }
        }
    }

    private fun GmsFragmentReviewBinding.handleDocumentResult(documentResult: ResultWrapper<Document>) {
        when (documentResult) {
            is ResultWrapper.Success -> {
                documentPageAdapter.submitList(viewModel.getPages(documentResult.value).also { pages ->
                    indicator.isVisible = pages.size > 1
                    pager.isUserInputEnabled = pages.size > 1
                })
            }

            is ResultWrapper.Error -> handleError(getString(R.string.gms_generic_error_message)) { viewModel.retryDocumentReview() }
            else -> { // Loading state handled by payment details
            }
        }
    }

    private fun GmsFragmentReviewBinding.handlePaymentResult(paymentResult: ResultWrapper<PaymentDetails>) {
        binding.loading.isVisible = paymentResult is ResultWrapper.Loading
        if (paymentResult is ResultWrapper.Error) {
            handleError(getString(R.string.gms_generic_error_message)) { viewModel.retryDocumentReview() }
        }
    }

    private fun GmsFragmentReviewBinding.configureViews() {
        close.isGone = !viewModel.configuration.showCloseButton
    }

    private fun GmsFragmentReviewBinding.configureOrientation() {
        pager.isVisible = true
        pager.adapter = documentPageAdapter
        TabLayoutMediator(indicator, pager) { tab, _ -> tab.view.isClickable = false }.attach()
    }

    private fun GmsFragmentReviewBinding.handlePaymentState(event: GiniMerchant.MerchantSDKEvents) {
        when (event) {
            is GiniMerchant.MerchantSDKEvents.OnFinishedWithPaymentRequestCreated -> {
                if (viewModel.reviewComponent.paymentProviderApp.value?.paymentProvider?.gpcSupported() == false) return
                try {
                    val intent =
                        viewModel.reviewComponent.paymentProviderApp.value?.getIntent(event.paymentRequestId)
                    if (intent != null) {
                        startActivity(intent)
                        viewModel.onBankOpened()
                    } else {
                        handleError(getString(R.string.gms_generic_error_message)) { viewModel.onPayment() }
                    }
                } catch (exception: ActivityNotFoundException) {
                    handleError(getString(R.string.gms_generic_error_message)) { viewModel.onPayment() }
                }
            }
            is GiniMerchant.MerchantSDKEvents.OnErrorOccurred -> {
                handleError(getString(R.string.gms_generic_error_message)) { viewModel.onPaymentButtonTapped(requireContext().externalCacheDir) }
            }
            else -> { // Loading is already handled
            }
        }
    }

    private fun GmsFragmentReviewBinding.handleError(text: String, onRetry: () -> Unit) {
        if (viewModel.configuration.handleErrorsInternally) {
            showSnackbar(text, onRetry)
        }
    }

    private fun GmsFragmentReviewBinding.showSnackbar(text: String, onRetry: () -> Unit) {
        val context = requireContext().wrappedWithGiniMerchantTheme()
        Snackbar.make(context, root, text, Snackbar.LENGTH_INDEFINITE).apply {
            if (context.getFontScale() < 1.5) {
                anchorView = paymentDetailsScrollview
            }
            setTextMaxLines(2)
            setAction(getString(R.string.gms_snackbar_retry)) { onRetry() }
            show()
        }
    }

    private fun GmsFragmentReviewBinding.setActionListeners() {
        gmsReviewView.onPayButtonTapped = object: ReviewViewListener {
            override fun onPaymentButtonTapped() {
                requireActivity().currentFocus?.clearFocus()
                viewModel.onPaymentButtonTapped(requireContext().externalCacheDir)
            }
        }
        close.setOnClickListener { view ->
            if (isKeyboardShown) {
                view.hideKeyboard()
            } else {
                listener?.onCloseReview()
            }
        }
    }

    private fun GmsFragmentReviewBinding.applyInsets() {
        close.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }
    }

    private fun GmsFragmentReviewBinding.removePagerConstraintAndSetPreviousHeightIfNeeded(savedHeight: Int) {
        root.post {
            ConstraintSet().apply {
                clone(constraintRoot)
                constrainHeight(R.id.pager, pager.height)
                clear(R.id.pager, ConstraintSet.BOTTOM)
                applyTo(constraintRoot)
            }
            if (savedHeight != -1) {
                val pagerLayoutParams = binding.pager.layoutParams
                pagerLayoutParams.height = savedHeight
                binding.pager.layoutParams = pagerLayoutParams
            }
        }
    }

    private fun GmsFragmentReviewBinding.setKeyboardAnimation() {
        ViewCompat.setWindowInsetsAnimationCallback(
            gmsReviewView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                var startBottom = 0
                var endBottom = 0

                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    startBottom = gmsReviewView.paddingBottom
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    if (Build.VERSION.SDK_INT >= 30) {
                        endBottom = gmsReviewView.paddingBottom
                        gmsReviewView.translationY = (endBottom - startBottom).toFloat()
                        paymentDetailsInfoBar.translationY = gmsReviewView.translationY
                    }
                    if (startBottom < endBottom) {
                        indicator.isVisible = false
                    }
                    return bounds
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    if (Build.VERSION.SDK_INT >= 30) {
                        runningAnimations.find { it.typeMask == windowInsetTypesOf(ime = true) }?.let { animation ->
                            gmsReviewView.translationY =
                                lerp((endBottom - startBottom).toFloat(), 0f, animation.interpolatedFraction)
                            paymentDetailsInfoBar.translationY = gmsReviewView.translationY
                        }
                    }
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    if (Build.VERSION.SDK_INT >= 30) {
                        gmsReviewView.translationY = 0f
                        paymentDetailsInfoBar.translationY = gmsReviewView.translationY
                    }
                    // Was it a closing animation?
                    if (startBottom > endBottom) {
                        if (pager.isUserInputEnabled) {
                            indicator.isVisible = true
                        }
                        binding.gmsReviewView.clearEditTextFocus()
                        isKeyboardShown = false
                    } else {
                        isKeyboardShown = true
                    }
                }
            })
    }

    private fun GmsFragmentReviewBinding.showInfoBar() {
        root.doOnLayout {
            // paymentDetailsInfoBar visibility should never be GONE, otherwise the animation won't work
            if (paymentDetailsInfoBar.isInvisible) {
                TransitionManager.beginDelayedTransition(root, TransitionSet().apply {
                    addTransition(ChangeBounds())
                    addListener(object : TransitionListenerAdapter() {
                        override fun onTransitionStart(transition: Transition) {
                            super.onTransitionStart(transition)
                            paymentDetailsInfoBar.isVisible = true
                        }
                    })
                })
                paymentDetailsInfoBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomToTop = paymentDetailsScrollview.id
                    topToTop = ConstraintLayout.LayoutParams.UNSET
                }
            }
        }
    }

    private fun GmsFragmentReviewBinding.hideInfoBarAnimated() {
        root.doOnLayout {
            if (paymentDetailsInfoBar.isVisible) {
                TransitionManager.beginDelayedTransition(root, TransitionSet().apply {
                    addTransition(ChangeBounds())
                    addListener(object : TransitionListenerAdapter() {
                        override fun onTransitionEnd(transition: Transition) {
                            super.onTransitionEnd(transition)
                            paymentDetailsInfoBar.isInvisible = true
                        }
                    })
                })
                paymentDetailsInfoBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topToTop = paymentDetailsScrollview.id
                    bottomToTop = ConstraintLayout.LayoutParams.UNSET
                }
            }
        }
    }

    private fun showInstallAppDialog(paymentProviderApp: PaymentProviderApp) {
        requireActivity().supportFragmentManager.showInstallAppBottomSheet(
            paymentComponent = viewModel.paymentComponent,
            minHeight = binding.paymentDetailsScrollview.height
        ) {
            redirectToBankApp(paymentProviderApp)
        }
    }

    private fun redirectToBankApp(paymentProviderApp: PaymentProviderApp) {
        listener?.onToTheBankButtonClicked(paymentProviderApp.name)
        viewModel.onPayment()
    }

    private fun showOpenWithDialog(paymentProviderApp: PaymentProviderApp) {
        requireActivity().supportFragmentManager.showOpenWithBottomSheet(
            paymentProviderApp = paymentProviderApp
        ) {
            viewModel.onForwardToSharePdfTapped(requireContext().externalCacheDir)
        }
        viewModel.incrementOpenWithCounter(viewModel.viewModelScope, paymentProviderApp.paymentProvider.id)
    }

    private fun handlePaymentNextStep(paymentNextStep: PaymentNextStep) {
        when (paymentNextStep) {
            is PaymentNextStep.SetLoadingVisibility -> {
                binding.loading.isVisible = paymentNextStep.isVisible
            }
            PaymentNextStep.RedirectToBank -> {
                viewModel.reviewComponent.paymentProviderApp.value?.let {
                    redirectToBankApp(it)
                }
            }
            PaymentNextStep.ShowOpenWithSheet -> viewModel.reviewComponent.paymentProviderApp.value?.let { showOpenWithDialog(it) }
            PaymentNextStep.ShowInstallApp -> viewModel.reviewComponent.paymentProviderApp.value?.let { showInstallAppDialog(it) }
            is PaymentNextStep.OpenSharePdf -> {
                binding.loading.isVisible = false
                startSharePdfIntent(paymentNextStep.file, requireContext().createShareWithPendingIntent())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(PAGER_HEIGHT, binding.pager.layoutParams.height)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        requireActivity().unregisterReceiver(shareWithEventBroadcastReceiver)
        super.onStop()
    }

    internal companion object {
        private const val PAGER_HEIGHT = "pager_height"
        fun newInstance(
            giniMerchant: GiniMerchant,
            configuration: ReviewConfiguration = ReviewConfiguration(),
            listener: ReviewFragmentListener? = null,
            paymentComponent: PaymentComponent,
            documentId: String,
            giniPaymentManager: GiniPaymentManager = GiniPaymentManager(giniMerchant),
            viewModelFactory: ViewModelProvider.Factory = ReviewViewModel.Factory(giniMerchant, configuration, paymentComponent, documentId, giniPaymentManager),
        ): ReviewFragment = ReviewFragment(listener, viewModelFactory)
    }
}
