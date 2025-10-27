package net.gini.android.health.sdk.review

import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.widget.EditText
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
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.chrisbanes.insetter.applyInsetter
import dev.chrisbanes.insetter.windowInsetTypesOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R as HealthR
import net.gini.android.internal.payment.R as InternalPaymentR
import net.gini.android.health.sdk.databinding.GhsFragmentReviewBinding
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.hideKeyboard
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.review.ReviewConfiguration
import net.gini.android.internal.payment.review.ReviewViewStateLandscape
import net.gini.android.internal.payment.review.reviewComponent.ReviewViewListener
import net.gini.android.internal.payment.utils.autoCleared
import net.gini.android.internal.payment.utils.extensions.getFontScale
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.extensions.getLocaleStringResource
import net.gini.android.internal.payment.utils.extensions.isLandscapeOrientation
import net.gini.android.internal.payment.utils.extensions.isViewModelInitialized
import net.gini.android.internal.payment.utils.extensions.onKeyboardAction
import net.gini.android.internal.payment.utils.extensions.wrappedWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.utils.showKeyboard
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
 * Delay duration (in milliseconds) used to allow the view to settle down before requesting focus.
 *
 * A value of 200ms was chosen based on observed behaviour on Android Q (API 29) and below, where
 * immediately requesting keyboard focus after view creation can result in the keyboard not
 * appearing.
 * This delay helps ensure that the keyboard is reliably shown when the field requests focus.
 */
private const val VIEW_SETTLE_DELAY_MS = 200L
private const val KEY_IME_WAS_VISIBLE = "ime_was_visible"
private const val KEY_FOCUSED_ID = "focused_view_id"
private const val KEYBOARD_VISIBILITY_RATIO = 0.25f
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

    private val viewModel: ReviewViewModel by viewModels{
        viewModelFactory ?: object : ViewModelProvider.Factory {}
    }
    private var imeVisibleNow: Boolean = false
    private var preRKeyboardTracker: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var binding: GhsFragmentReviewBinding by autoCleared()
    private var documentPageAdapter: DocumentPageAdapter by autoCleared()
    private var isKeyboardShown = false
    private var errorSnackbar: Snackbar? = null


    @VisibleForTesting
    internal val reviewViewListener = object : ReviewViewListener {
        override fun onPaymentButtonTapped(paymentDetails: net.gini.android.internal.payment.api.model.PaymentDetails) {
            requireActivity().currentFocus?.clearFocus()
            binding.ghsPaymentDetails.hideKeyboard()
            viewModel.reviewFragmentListener.onToTheBankButtonClicked(
                viewModel.paymentProviderApp.value?.name ?: "", viewModel.paymentDetails.value
            )
        }

        override fun onSelectBankButtonTapped() {
            viewModel.paymentComponent.listener?.onBankPickerClicked()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isViewModelMissing = !isViewModelInitialized(ReviewViewModel::class)

        if (viewModelFactory == null && isViewModelMissing) {
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val inflater = super.onGetLayoutInflater(savedInstanceState)
        return this.getLayoutInflaterWithGiniPaymentThemeAndLocale(
            inflater,
            viewModel.paymentComponent.getGiniPaymentLanguage(requireContext())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        documentPageAdapter = DocumentPageAdapter { pageNumber ->
            viewModel.reloadImage(pageNumber)
        }
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
            bottomMargin =
                -resources.getDimensionPixelSize(net.gini.android.internal.payment.R.dimen.gps_medium_12)
        }

        if (resources.isLandscapeOrientation()) {
            setupLandscapeBehavior()
        }

        // handling keyboard in Version <= Q (Pie and below) after orientation change
        if (preQ()) {
            startPreRKeyboardTracker(view)
            restoreImeIfNeeded(view, savedInstanceState)
        }
    }

    private fun restoreImeIfNeeded(root: View, savedInstanceState: Bundle?) {
        val focusedId = savedInstanceState?.getInt(KEY_FOCUSED_ID) ?: View.NO_ID
        val imeWasVisible = savedInstanceState?.getBoolean(KEY_IME_WAS_VISIBLE) ?: false
        if (focusedId == View.NO_ID || !imeWasVisible) return

        root.post {
            val et = root.findViewById<EditText>(focusedId)
            if (et?.isShown == true && et.isEnabled && et.isFocusable) {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(VIEW_SETTLE_DELAY_MS)
                    et.showKeyboard() // Helper already requests focus
                }
            }
        }
    }


    private fun GhsFragmentReviewBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.paymentComponent.recheckWhichPaymentProviderAppsAreInstalled()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.documentPages.collect { handleDocumentPagesResult(it) }
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
                launch {
                    viewModel.showLoading.collect { isLoading ->
                        binding.loading.isVisible = isLoading
                    }
                }
            }
        }
    }

    private fun GhsFragmentReviewBinding.handleDocumentPagesResult(documentPagesResult: DocumentPagesResult) {
        when (documentPagesResult) {
            is DocumentPagesResult.Success -> {
                documentPageAdapter.submitList(documentPagesResult.pagesList.also { pages ->
                    indicator.isVisible = true
                    indicator.importantForAccessibility =
                        if (pages.size > 1) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    indicator.focusable = if (pages.size > 1) View.FOCUSABLE else View.NOT_FOCUSABLE
                    pager.isUserInputEnabled = pages.size > 1
                    indicator.isEnabled = pages.size > 1
                    indicator.alpha = if (pages.size > 1) 1f else 0f
                })
            }

            is DocumentPagesResult.Error -> {
                handleError(getLocaleStringResource(net.gini.android.internal.payment.R.string.gps_generic_error_message)) { viewModel.retryDocumentReview() }
            }

            else -> {
                //do nothing, loading is handled separately
            }
        }
    }

    private fun GhsFragmentReviewBinding.handlePaymentResult(paymentResult: ResultWrapper<PaymentDetails>) {
        if (paymentResult is ResultWrapper.Error) {
            handleError(getLocaleStringResource(net.gini.android.internal.payment.R.string.gps_generic_error_message)) { viewModel.retryDocumentReview() }
        }
    }

    private fun GhsFragmentReviewBinding.configureViews() {
        close.isGone = !viewModel.paymentFlowConfiguration.showCloseButtonOnReviewFragment
    }

    private fun GhsFragmentReviewBinding.configureOrientation() {
        pager.isVisible = true
        pager.adapter = documentPageAdapter
      val mediator = TabLayoutMediator(indicator, pager) { tab, _ ->
          tab.view.isFocusable = documentPageAdapter.itemCount > 1
          tab.view.isClickable = true
      }
        mediator.attach()
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
            setTextMaxLines(3)
            setAction(getLocaleStringResource(net.gini.android.internal.payment.R.string.gps_snackbar_retry)) {
                onRetry()
            }
            show()
        }
    }

    private fun GhsFragmentReviewBinding.setActionListeners() {
        ghsPaymentDetails.listener = reviewViewListener
        close.setOnClickListener { view ->
            binding.root.findFocus()?.clearFocus()
            binding.ghsPaymentDetails.clearFocus()
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
        if (resources.isLandscapeOrientation()) return
        root.post {
            if (savedHeight == 0) return@post
            ConstraintSet().apply {
                clone(constraintRoot)
                constrainHeight(HealthR.id.pager, pager.height)
                clear(HealthR.id.pager, ConstraintSet.BOTTOM)
                applyTo(constraintRoot)
            }
            if (savedHeight > 0) {
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
                        runningAnimations.find { it.typeMask == windowInsetTypesOf(ime = true) }
                            ?.let { animation ->
                                ghsPaymentDetails.translationY =
                                    com.google.android.material.math.MathUtils.lerp(
                                        (endBottom - startBottom).toFloat(),
                                        0f,
                                        animation.interpolatedFraction
                                    )
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

    private fun startPreRKeyboardTracker(root: View) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = android.graphics.Rect()
            root.getWindowVisibleDisplayFrame(r)
            val visible = r.height()
            val heightDiff = root.rootView.height - visible
            imeVisibleNow = heightDiff > root.rootView.height * KEYBOARD_VISIBILITY_RATIO // keyboard threshold
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        preRKeyboardTracker = listener
    }

    private fun GhsFragmentReviewBinding.showInfoBar() {
        root.doOnLayout {
            if (resources.isLandscapeOrientation()) {
                paymentDetailsInfoBar.isVisible = true
            }
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
            if (!resources.isLandscapeOrientation()) {
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
            } else {
                paymentInfoLabel?.isVisible = false
            }
        }
    }

    private fun setupLandscapeBehavior() {
        val dragHandle = binding.dragHandleContainer
        val fieldsLayout =
            binding.ghsPaymentDetails.findViewById<View>(net.gini.android.internal.payment.R.id.gps_fields_layout)
        val bottomLayout =
            binding.ghsPaymentDetails.findViewById<View>(net.gini.android.internal.payment.R.id.gps_bottom_layout)
        dragHandle?.onKeyboardAction {
            fieldsLayout.alpha = if (isVisible) 0f else 1f
            val currentState =
                binding.ghsPaymentDetails.reviewComponent?.getReviewViewStateInLandscapeMode()
            binding.ghsPaymentDetails.reviewComponent?.setReviewViewModeInLandscapeMode(
                if (currentState == ReviewViewStateLandscape.EXPANDED) ReviewViewStateLandscape.COLLAPSED else ReviewViewStateLandscape.EXPANDED
            )
        }
        binding.root.post {
            setupConstraintsForTabLayout((dragHandle?.height ?: 0) + bottomLayout.height)
        }
        dragHandle?.setOnClickListener {
            fieldsLayout.alpha = if (it.isVisible) 0f else 1f
            val currentState =
                binding.ghsPaymentDetails.reviewComponent?.getReviewViewStateInLandscapeMode()
            val nextState = if (currentState == ReviewViewStateLandscape.EXPANDED)
                ReviewViewStateLandscape.COLLAPSED
            else
                ReviewViewStateLandscape.EXPANDED

            binding.ghsPaymentDetails.reviewComponent?.setReviewViewModeInLandscapeMode(nextState)

            val announcement = when (nextState) {
                ReviewViewStateLandscape.EXPANDED -> getString(InternalPaymentR.string.gps_drag_handle_expanded)
                ReviewViewStateLandscape.COLLAPSED -> getString(InternalPaymentR.string.gps_drag_handle_collapsed)
                else -> null
            }
            announcement?.let {
                dragHandle.announceForAccessibility(it)
            }
        }

    }

    private fun setupConstraintsForTabLayout(collapsedBottomSheetHeight: Int) {
        val layoutParams = binding.indicator.layoutParams as MarginLayoutParams
        layoutParams.bottomMargin = collapsedBottomSheetHeight + 20
        binding.indicator.layoutParams = layoutParams
    }

    private fun getLocaleStringResource(resourceId: Int): String {
        return getLocaleStringResource(resourceId, viewModel.giniInternalPaymentModule)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val height = view?.findViewById<ViewPager2>(HealthR.id.pager)?.layoutParams?.height ?: -1
        outState.putInt(PAGER_HEIGHT, height)
        if (preQ()) {
            val focusedId = view?.findFocus()?.id ?: View.NO_ID
            outState.putInt(KEY_FOCUSED_ID, focusedId)
            outState.putBoolean(KEY_IME_WAS_VISIBLE, imeVisibleNow)
        }
        super.onSaveInstanceState(outState)
    }
    private fun preQ() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

    override fun onDestroyView() {
        preRKeyboardTracker?.let {
            view?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
        }
        preRKeyboardTracker = null
        super.onDestroyView()
    }
    internal companion object {
        private const val PAGER_HEIGHT = "pager_height"

        fun newInstance(
            giniHealth: GiniHealth,
            configuration: ReviewConfiguration = ReviewConfiguration(),
            listener: ReviewFragmentListener,
            paymentComponent: PaymentComponent,
            documentId: String,
            paymentFlowConfiguration: PaymentFlowConfiguration
        ): ReviewFragment {
            // Store non-Parcelable dependencies in holder
            val viewModelFactory: ViewModelProvider.Factory = ReviewViewModel.Factory(giniHealth, configuration, paymentComponent, documentId, paymentFlowConfiguration, listener)
            return ReviewFragment(viewModelFactory)
        }

    }
}
