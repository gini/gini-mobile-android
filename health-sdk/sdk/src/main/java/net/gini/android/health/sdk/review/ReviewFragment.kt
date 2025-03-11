package net.gini.android.health.sdk.review

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
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsFragmentReviewBinding
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.hideKeyboard
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.getFontScale
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.getLocaleStringResource
import net.gini.android.internal.payment.utils.extensions.wrappedWithGiniPaymentThemeAndLocale
import org.jetbrains.annotations.VisibleForTesting

/**
 * Listener for [ReviewFragment] events.
 */
internal interface ReviewFragmentListener {
    /**
     * Called when the close button was pressed.
     */
    fun onCloseReview()

    /**
     * Called when the "to the bank" button was clicked.
     *
     * Collect the [GiniHealth.openBankState] flow to get details about the payment request creation and about the
     * selected bank app.
     *
     * @param paymentProviderName the name of the selected payment provider
     */
    fun onToTheBankButtonClicked(paymentProviderName: String, paymentDetails: PaymentDetails)
}

/**
 * The [ReviewFragment] displays an invoice’s pages and payment information extractions. It also lets users pay the
 * invoice with the bank they selected in the [BankSelectionBottomSheet].
 *
 * Instances can be created using the [PaymentComponent.getPaymentReviewFragment] method.
 */
class ReviewFragment private constructor(
    private val viewModelFactory: ViewModelProvider.Factory? = null,
) : Fragment() {

    constructor() : this(null)

    private val viewModel: ReviewViewModel by viewModels { viewModelFactory ?: object : ViewModelProvider.Factory {} }
    private var binding: GhsFragmentReviewBinding by autoCleared()
    private var documentPageAdapter: DocumentPageAdapter by autoCleared()
    private var isKeyboardShown = false
    private var errorSnackbar: Snackbar? = null

    @VisibleForTesting
    internal val reviewViewListener = object: ReviewViewListener {
        override fun onPaymentButtonTapped(paymentDetails: net.gini.android.internal.payment.api.model.PaymentDetails) {
            requireActivity().currentFocus?.clearFocus()
            binding.ghsPaymentDetails.hideKeyboard()
            viewModel.reviewFragmentListener.onToTheBankButtonClicked(viewModel.paymentProviderApp.value?.name ?: "", viewModel.paymentDetails.value)
        }

        override fun onSelectBankButtonTapped() {
            viewModel.paymentComponent.listener?.onBankPickerClicked()
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniPaymentThemeAndLocale(
            inflater,
            viewModel.paymentComponent.getGiniPaymentLanguage(requireContext())
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        documentPageAdapter = DocumentPageAdapter(viewModel.giniHealth)
        binding = GhsFragmentReviewBinding.inflate(inflater).apply {
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

        with(binding) {
            ghsPaymentDetails.reviewComponent = viewModel.reviewComponent
            setStateListeners()
            setKeyboardAnimation()
            removePagerConstraintAndSetPreviousHeightIfNeeded(documentPagerHeight)
        }

        // Set info bar bottom margin programmatically to reuse radius dimension with negative sign
        binding.paymentDetailsInfoBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = -resources.getDimensionPixelSize(net.gini.android.internal.payment.R.dimen.gps_medium_12)
        }
    }

    private fun GhsFragmentReviewBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.giniHealth.documentFlow.collect { handleDocumentResult(it) }
                }
                launch {
                    viewModel.giniHealth.paymentFlow.collect { handlePaymentResult(it) }
                }
                launch {
                    viewModel.isInfoBarVisible.collect { visible ->
                        if (visible) showInfoBar() else hideInfoBarAnimated()
                    }
                }
                launch {
                    viewModel.paymentProviderApp.collect { paymentProviderApp ->
                        if (paymentProviderApp != null) {
                            setActionListeners()
                        }
                    }
                }
            }
        }
    }

    private fun GhsFragmentReviewBinding.handleDocumentResult(documentResult: ResultWrapper<Document>) {
        when (documentResult) {
            is ResultWrapper.Success -> {
                documentPageAdapter.submitList(viewModel.getPages(documentResult.value).also { pages ->
                    indicator.isVisible = pages.size > 1
                    pager.isUserInputEnabled = pages.size > 1
                })
            }

            is ResultWrapper.Error -> handleError(getLocaleStringResource(net.gini.android.internal.payment.R.string.gps_generic_error_message)) { viewModel.retryDocumentReview() }
            else -> { // Loading state handled by payment details
            }
        }
    }

    private fun GhsFragmentReviewBinding.handlePaymentResult(paymentResult: ResultWrapper<PaymentDetails>) {
        binding.loading.isVisible = paymentResult is ResultWrapper.Loading
        if (paymentResult is ResultWrapper.Error) {
            handleError(getLocaleStringResource(net.gini.android.internal.payment.R.string.gps_generic_error_message)) { viewModel.retryDocumentReview() }
        }
    }

    private fun GhsFragmentReviewBinding.configureViews() {
        close.isGone = !viewModel.shouldShowCloseButton
    }

    private fun GhsFragmentReviewBinding.configureOrientation() {
        pager.isVisible = true
        pager.adapter = documentPageAdapter
        TabLayoutMediator(indicator, pager) { tab, _ -> tab.view.isClickable = false }.attach()
    }

    private fun GhsFragmentReviewBinding.handleError(text: String, onRetry: () -> Unit) {
        if (viewModel.configuration.handleErrorsInternally) {
            showSnackbar(text, onRetry)
        }
    }

    private fun GhsFragmentReviewBinding.showSnackbar(text: String, onRetry: () -> Unit) {
        val context = requireContext().wrappedWithGiniPaymentThemeAndLocale(viewModel.paymentComponent.getGiniPaymentLanguage(requireContext()))
        errorSnackbar?.dismiss()
        errorSnackbar = Snackbar.make(context, root, text, Snackbar.LENGTH_INDEFINITE).apply {
            if (context.getFontScale() < 1.5) {
                anchorView = paymentDetailsScrollview
            }
            setTextMaxLines(2)
            setAction(getLocaleStringResource(net.gini.android.internal.payment.R.string.gps_snackbar_retry)) {
                onRetry()
            }
            show()
        }
    }

    private fun GhsFragmentReviewBinding.setActionListeners() {
        ghsPaymentDetails.listener = reviewViewListener
        close.setOnClickListener { view ->
            if (isKeyboardShown) {
                view.hideKeyboard()
            } else {
                viewModel.reviewFragmentListener.onCloseReview()
            }
        }
    }

    private fun GhsFragmentReviewBinding.applyInsets() {
        close.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }
    }

    private fun GhsFragmentReviewBinding.removePagerConstraintAndSetPreviousHeightIfNeeded(savedHeight: Int) {
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

    private fun GhsFragmentReviewBinding.setKeyboardAnimation() {
        ViewCompat.setWindowInsetsAnimationCallback(
            ghsPaymentDetails,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                var startBottom = 0
                var endBottom = 0

                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    startBottom = ghsPaymentDetails.paddingBottom
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    if (Build.VERSION.SDK_INT >= 30) {
                        endBottom = ghsPaymentDetails.paddingBottom
                        ghsPaymentDetails.translationY = (endBottom - startBottom).toFloat()
                        paymentDetailsInfoBar.translationY = ghsPaymentDetails.translationY
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
                            ghsPaymentDetails.translationY =
                                lerp((endBottom - startBottom).toFloat(), 0f, animation.interpolatedFraction)
                            paymentDetailsInfoBar.translationY = ghsPaymentDetails.translationY
                        }
                    }
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    if (Build.VERSION.SDK_INT >= 30) {
                        ghsPaymentDetails.translationY = 0f
                        paymentDetailsInfoBar.translationY = ghsPaymentDetails.translationY
                    }
                    // Was it a closing animation?
                    if (startBottom > endBottom) {
                        if (pager.isUserInputEnabled) {
                            indicator.isVisible = true
                        }
                        binding.ghsPaymentDetails.clearFocus()
                        isKeyboardShown = false
                    } else {
                        isKeyboardShown = true
                    }
                }
            })
    }

    private fun GhsFragmentReviewBinding.showInfoBar() {
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

    private fun GhsFragmentReviewBinding.hideInfoBarAnimated() {
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

    private fun getLocaleStringResource(resourceId: Int): String {
        return getLocaleStringResource(resourceId, viewModel.giniInternalPaymentModule)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(PAGER_HEIGHT, binding.pager.layoutParams.height)
        super.onSaveInstanceState(outState)
    }

    internal companion object {
        private const val PAGER_HEIGHT = "pager_height"

        fun newInstance(
            giniHealth: GiniHealth,
            configuration: ReviewConfiguration = ReviewConfiguration(),
            listener: ReviewFragmentListener,
            paymentComponent: PaymentComponent,
            documentId: String,
            shouldShowCloseButton: Boolean,
            popupDurationPaymentReview: Int,
            viewModelFactory: ViewModelProvider.Factory = ReviewViewModel.Factory(giniHealth, configuration, paymentComponent, documentId, shouldShowCloseButton, popupDurationPaymentReview, listener),
        ): ReviewFragment = ReviewFragment(viewModelFactory)
    }
}
