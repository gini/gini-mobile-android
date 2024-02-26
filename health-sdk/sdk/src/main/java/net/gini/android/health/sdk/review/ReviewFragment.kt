package net.gini.android.health.sdk.review

import android.content.ActivityNotFoundException
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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
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
import com.google.android.material.textfield.TextInputLayout
import dev.chrisbanes.insetter.applyInsetter
import dev.chrisbanes.insetter.windowInsetTypesOf
import kotlinx.coroutines.launch
import net.gini.android.core.api.models.Document
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsFragmentReviewBinding
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.pager.DocumentPageAdapter
import net.gini.android.health.sdk.util.amountWatcher
import net.gini.android.health.sdk.util.autoCleared
import net.gini.android.health.sdk.util.clearErrorMessage
import net.gini.android.health.sdk.util.hideErrorMessage
import net.gini.android.health.sdk.util.hideKeyboard
import net.gini.android.health.sdk.util.setBackgroundTint
import net.gini.android.health.sdk.util.setErrorMessage
import net.gini.android.health.sdk.util.setTextIfDifferent
import net.gini.android.health.sdk.util.showErrorMessage
import org.slf4j.LoggerFactory


/**
 * Configuration for [ReviewFragment].
 */
data class ReviewConfiguration(
    /**
     * If true errors will be observed abd snackbars will be displayed.
     * If false errors will be ignored, in this case the flows exposed by [GiniHealth] should be observed for errors.
     */
    val handleErrorsInternally: Boolean = true,

    /**
     * Set to `true` to show a close button. Set a [ReviewFragmentListener] to be informed when the
     * button is pressed.
     */
    val showCloseButton: Boolean = false
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
     * Called when the next (pay) button was clicked.
     *
     * Collect the [GiniHealth.openBankState] flow to get details about the payment request creation and about the
     * selected bank app.
     *
     * @param paymentProviderName the name of the selected payment provider
     */
    fun onNextClicked(paymentProviderName: String)
}

/**
 * TODO: update documentation
 *
 * Fragment that displays document pages and extractions and it lets the user pay using a payment provider.
 *
 * To instantiate it you need to create a [FragmentFactory] and set it to fragment manager:
 *
 * ```
 *  class ReviewFragmentFactory(private val giniHealth: GiniHealth) : FragmentFactory() {
 *      override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
 *          return ReviewFragment(giniHealth)
 *      }
 *  }
 * ```
 */
class ReviewFragment private constructor(
    private val giniHealth: GiniHealth? = null,
    private val configuration: ReviewConfiguration? = null,
    var listener: ReviewFragmentListener? = null,
    private val paymentProviderApp: PaymentProviderApp? = null,
    private val viewModelFactory: ViewModelProvider.Factory? = null,
) : Fragment() {

    constructor() : this(null)

    private val viewModel: ReviewViewModel by viewModels { viewModelFactory ?: object : ViewModelProvider.Factory {} }
    private var binding: GhsFragmentReviewBinding by autoCleared()
    private var documentPageAdapter: DocumentPageAdapter by autoCleared()
    private var isKeyboardShown = false

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

        viewModel.userPreferences = UserPreferences(requireContext())

        with(binding) {
            setStateListeners()
            setInputListeners()
            setActionListeners()
            setKeyboardAnimation()
            removePagerConstraint()
            showSelectedPaymentProviderApp()
        }

        // Set info bar bottom margin programmatically to reuse radius dimension with negative sign
        binding.paymentDetailsInfoBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = -resources.getDimensionPixelSize(R.dimen.ghs_payment_details_radius)
        }
    }

    private fun GhsFragmentReviewBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.giniHealth.documentFlow.collect { handleDocumentResult(it) }
                }
                launch {
                    viewModel.giniHealth.paymentFlow.collect { handlePaymentResult(it) }
                }
                launch {
                    viewModel.paymentDetails.collect { setPaymentDetails(it) }
                }
                launch {
                    viewModel.paymentValidation.collect { handleValidationResult(it) }
                }
                launch {
                    viewModel.giniHealth.openBankState.collect { handlePaymentState(it) }
                }
                launch {
                    viewModel.isPaymentButtonEnabled.collect { isEnabled ->
                        payment.isEnabled = isEnabled
                        payment.alpha = if (isEnabled) 1f else 0.4f
                    }
                }
                launch {
                    viewModel.isInfoBarVisible.collect { visible ->
                        if (visible) showInfoBar() else hideInfoBarAnimated()
                    }
                }
            }
        }
    }

    private fun GhsFragmentReviewBinding.showSelectedPaymentProviderApp() {
        payment.setCompoundDrawablesWithIntrinsicBounds(
            viewModel.paymentProviderApp.icon,
            null,
            null,
            null
        )
        payment.setBackgroundTint(viewModel.paymentProviderApp.colors.backgroundColor, 255)
        payment.setTextColor(viewModel.paymentProviderApp.colors.textColor)
    }

    private fun GhsFragmentReviewBinding.handleDocumentResult(documentResult: ResultWrapper<Document>) {
        when (documentResult) {
            is ResultWrapper.Success -> {
                documentPageAdapter.submitList(viewModel.getPages(documentResult.value).also { pages ->
                    indicator.isVisible = pages.size > 1
                    pager.isUserInputEnabled = pages.size > 1
                })
                removePagerConstraint()
            }

            is ResultWrapper.Error -> handleError(getString(R.string.ghs_error_document)) { viewModel.retryDocumentReview() }
            else -> { // Loading state handled by payment details
            }
        }
    }

    private fun GhsFragmentReviewBinding.handlePaymentResult(paymentResult: ResultWrapper<PaymentDetails>) {
        binding.loading.isVisible = paymentResult is ResultWrapper.Loading
        if (paymentResult is ResultWrapper.Error) {
            handleError(getString(R.string.ghs_error_payment_details)) { viewModel.retryDocumentReview() }
        }
    }

    private fun GhsFragmentReviewBinding.configureViews() {
        close.isGone = !viewModel.configuration.showCloseButton
    }

    private fun GhsFragmentReviewBinding.configureOrientation() {
        pager.isVisible = true
        pager.adapter = documentPageAdapter
        TabLayoutMediator(indicator, pager) { tab, _ -> tab.view.isClickable = false }.attach()
    }

    private fun GhsFragmentReviewBinding.setPaymentDetails(paymentDetails: PaymentDetails) {
        recipient.setTextIfDifferent(paymentDetails.recipient)
        iban.setTextIfDifferent(paymentDetails.iban)
        amount.setTextIfDifferent(paymentDetails.amount)
        purpose.setTextIfDifferent(paymentDetails.purpose)
    }

    private fun GhsFragmentReviewBinding.setInputListeners() {
        recipient.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setRecipient(text.toString()) })
        iban.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setIban(text.toString()) })
        amount.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setAmount(text.toString()) })
        amount.addTextChangedListener(amountWatcher)
        purpose.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setPurpose(text.toString()) })
        recipient.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, recipientLayout) }
        iban.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, ibanLayout) }
        amount.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, amountLayout) }
        purpose.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, purposeLayout) }
    }

    private fun handleInputFocusChange(hasFocus: Boolean, textInputLayout: TextInputLayout) {
        if (hasFocus) textInputLayout.hideErrorMessage() else textInputLayout.showErrorMessage()
    }

    private fun GhsFragmentReviewBinding.handleValidationResult(messages: List<ValidationMessage>) {
        val (fieldsWithError, fieldsWithoutError) = PaymentField.values()
            .map { field -> field to messages.firstOrNull { it.field == field } }
            .partition { (_, message) -> message != null }

        fieldsWithError.forEach { (field, validationMessage) ->
            validationMessage?.let { message ->
                getTextInputLayout(field).apply {
                    if (error.isNullOrEmpty()) {
                        setErrorMessage(
                            when (message) {
                                is ValidationMessage.Empty -> when (field) {
                                    PaymentField.Recipient -> R.string.ghs_error_input_recipient_empty
                                    PaymentField.Iban -> R.string.ghs_error_input_iban_empty
                                    PaymentField.Amount -> R.string.ghs_error_input_amount_empty
                                    PaymentField.Purpose -> R.string.ghs_error_input_purpose_empty
                                }

                                ValidationMessage.InvalidIban -> R.string.ghs_error_input_invalid_iban
                                ValidationMessage.AmountFormat -> R.string.ghs_error_input_amount_format
                            }
                        )
                        if (editText?.isFocused == true) {
                            hideErrorMessage()
                        }
                    }
                }
            }
        }

        fieldsWithoutError.forEach { (field, _) ->
            getTextInputLayout(field).apply {
                clearErrorMessage()
            }
        }
    }

    private fun GhsFragmentReviewBinding.getTextInputLayout(field: PaymentField) = when (field) {
        PaymentField.Recipient -> recipientLayout
        PaymentField.Iban -> ibanLayout
        PaymentField.Amount -> amountLayout
        PaymentField.Purpose -> purposeLayout
    }

    private fun GhsFragmentReviewBinding.handlePaymentState(paymentState: GiniHealth.PaymentState) {
        (paymentState is GiniHealth.PaymentState.Loading).let { isLoading ->
            paymentProgress.isVisible = isLoading
            recipientLayout.isEnabled = !isLoading
            ibanLayout.isEnabled = !isLoading
            amountLayout.isEnabled = !isLoading
            purposeLayout.isEnabled = !isLoading
            payment.text = if (isLoading) "" else getString(R.string.ghs_pay_button)
        }
        when (paymentState) {
            is GiniHealth.PaymentState.Success -> {
                try {
                    val intent =
                        paymentState.paymentRequest.bankApp.getIntent(paymentState.paymentRequest.id)
                    if (intent != null) {
                        startActivity(intent)
                        viewModel.onBankOpened()
                    } else {
                        // TODO: use more informative error messages (include selected bank app name)
                        handleError(getString(R.string.ghs_error_bank_not_found)) { viewModel.onPayment() }
                    }
                } catch (exception: ActivityNotFoundException) {
                    // TODO: use more informative error messages (include selected bank app name)
                    handleError(getString(R.string.ghs_error_bank_not_found)) { viewModel.onPayment() }
                }
            }
            // TODO: use more informative error messages (include error details)
            is GiniHealth.PaymentState.Error -> handleError(getString(R.string.ghs_error_open_bank)) { viewModel.onPayment() }
            else -> { // Loading is already handled
            }
        }
    }

    private fun GhsFragmentReviewBinding.handleError(text: String, onRetry: () -> Unit) {
        if (viewModel.configuration.handleErrorsInternally) {
            showSnackbar(text, onRetry)
        }
    }

    private fun GhsFragmentReviewBinding.showSnackbar(text: String, onRetry: () -> Unit) {
        Snackbar.make(root, text, Snackbar.LENGTH_INDEFINITE)
            .setAnchorView(paymentDetails)
            .setAction(getString(R.string.ghs_snackbar_retry)) { onRetry() }
            .show()
    }

    private fun GhsFragmentReviewBinding.setActionListeners() {
        paymentDetails.setOnClickListener { it.hideKeyboard() }
        payment.setOnClickListener {
            it.hideKeyboard()
            listener?.onNextClicked(viewModel.paymentProviderApp.name)
            viewModel.onPayment()
        }
        close.setOnClickListener { view ->
            if (isKeyboardShown) {
                view.hideKeyboard()
            } else {
                listener?.onCloseReview()
            }
        }
    }

    private fun GhsFragmentReviewBinding.applyInsets() {
        paymentDetails.applyInsetter {
            type(navigationBars = true, ime = true) {
                padding(bottom = true)
            }
        }
        close.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }
    }

    private fun GhsFragmentReviewBinding.removePagerConstraint() {
        root.post {
            ConstraintSet().apply {
                clone(constraintRoot)
                constrainHeight(R.id.pager, pager.height)
                clear(R.id.pager, ConstraintSet.BOTTOM)
                applyTo(constraintRoot)
            }
        }
    }

    private fun GhsFragmentReviewBinding.setKeyboardAnimation() {
        ViewCompat.setWindowInsetsAnimationCallback(
            paymentDetails,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                var startBottom = 0
                var endBottom = 0

                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    startBottom = paymentDetails.paddingBottom
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    if (Build.VERSION.SDK_INT >= 30) {
                        endBottom = paymentDetails.paddingBottom
                        paymentDetails.translationY = (endBottom - startBottom).toFloat()
                        paymentDetailsInfoBar.translationY = paymentDetails.translationY
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
                            paymentDetails.translationY =
                                lerp((endBottom - startBottom).toFloat(), 0f, animation.interpolatedFraction)
                            paymentDetailsInfoBar.translationY = paymentDetails.translationY
                        }
                    }
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    if (Build.VERSION.SDK_INT >= 30) {
                        paymentDetails.translationY = 0f
                        paymentDetailsInfoBar.translationY = paymentDetails.translationY
                    }
                    // Was it a closing animation?
                    if (startBottom > endBottom) {
                        if (pager.isUserInputEnabled) {
                            indicator.isVisible = true
                        }
                        binding.clearFocus()
                        isKeyboardShown = false
                    } else {
                        isKeyboardShown = true
                    }
                }
            })
    }

    private fun GhsFragmentReviewBinding.clearFocus() {
        recipient.clearFocus()
        iban.clearFocus()
        amount.clearFocus()
        purpose.clearFocus()
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
                    bottomToTop = paymentDetails.id
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
                    topToTop = paymentDetails.id
                    bottomToTop = ConstraintLayout.LayoutParams.UNSET
                }
            }
        }
    }

    companion object {
        fun newInstance(
            giniHealth: GiniHealth,
            configuration: ReviewConfiguration = ReviewConfiguration(),
            listener: ReviewFragmentListener? = null,
            paymentProviderApp: PaymentProviderApp,
            viewModelFactory: ViewModelProvider.Factory = ReviewViewModel.Factory(giniHealth, paymentProviderApp, configuration)
        ): ReviewFragment = ReviewFragment(giniHealth, configuration, listener, paymentProviderApp, viewModelFactory)
    }
}
