package net.gini.pay.ginipaybusiness.review

import android.content.ActivityNotFoundException
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.math.MathUtils.lerp
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.chrisbanes.insetter.applyInsetter
import dev.chrisbanes.insetter.windowInsetTypesOf
import kotlinx.coroutines.flow.collect
import net.gini.android.models.Document
import net.gini.pay.ginipaybusiness.GiniBusiness
import net.gini.pay.ginipaybusiness.R
import net.gini.pay.ginipaybusiness.databinding.GpbFragmentReviewBinding
import net.gini.pay.ginipaybusiness.review.bank.getBanks
import net.gini.pay.ginipaybusiness.review.model.PaymentDetails
import net.gini.pay.ginipaybusiness.review.model.ResultWrapper
import net.gini.pay.ginipaybusiness.review.pager.DocumentPageAdapter
import net.gini.pay.ginipaybusiness.util.amountWatcher
import net.gini.pay.ginipaybusiness.util.autoCleared
import net.gini.pay.ginipaybusiness.util.setTextIfDifferent


/**
 * Configuration for [ReviewFragment].
 */
data class ReviewConfiguration(
    /**
     * If true errors will be observed abd snackbars will be displayed.
     * If false errors will be ignored, in this case the flows exposed by [GiniBusiness] should be observed for errors.
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

    companion object {
        internal fun noOpInstance() = object : ReviewFragmentListener {
            override fun onCloseReview() {}
        }
    }
}

/**
 * Fragment that displays document pages and extractions and it lets the user pay using a payment provider.
 *
 * To instantiate it you need to create a [FragmentFactory] and set it to fragment manager:
 *
 * ```
 *  class ReviewFragmentFactory(private val giniBusiness: GiniBusiness) : FragmentFactory() {
 *      override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
 *          return ReviewFragment(giniBusiness)
 *      }
 *  }
 * ```
 */
class ReviewFragment(
    private val giniBusiness: GiniBusiness,
    private val configuration: ReviewConfiguration = ReviewConfiguration(),
    private val listener: ReviewFragmentListener? = null
) : Fragment() {

    private val viewModel: ReviewViewModel by viewModels { getReviewViewModelFactory(giniBusiness) }
    private var binding: GpbFragmentReviewBinding by autoCleared()
    private var documentPageAdapter: DocumentPageAdapter by autoCleared()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        documentPageAdapter = DocumentPageAdapter(giniBusiness)
        binding = GpbFragmentReviewBinding.inflate(inflater).apply {
            configureViews()
            configureOrientation()
            applyInsets()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            setBank()
            setStateListeners()
            setInputListeners()
            setActionListeners()
            setKeyboardAnimation()
            removePagerConstraint()
        }
    }

    private fun GpbFragmentReviewBinding.setBank() {
        requireActivity().packageManager.getBanks().firstOrNull()?.let { bankInfo ->
            viewModel.selectedBank = bankInfo
            val icon = bankInfo.getIconDrawable(requireActivity().packageManager).apply {
                resources.getDimension(R.dimen.gpb_bank_icon_size).toInt().let { this.setBounds(0, 0, it, it) }
            }
            bank.setCompoundDrawables(icon, null, null, null)
            bank.text = bankInfo.name
        }
    }

    private fun GpbFragmentReviewBinding.setStateListeners() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.giniBusiness.documentFlow.collect { handleDocumentResult(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.giniBusiness.paymentFlow.collect { handlePaymentResult(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.paymentDetails.collect { setPaymentDetails(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.paymentValidation.collect { handleValidationResult(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.giniBusiness.openBankState.collect { handlePaymentState(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isPaymentButtonEnabled.collect { payment.isEnabled = it }
        }
    }

    private fun GpbFragmentReviewBinding.handleDocumentResult(documentResult: ResultWrapper<Document>) {
        when (documentResult) {
            is ResultWrapper.Success -> {
                documentPageAdapter.submitList(viewModel.getPages(documentResult.value).also { pages ->
                    indicator.isVisible = pages.size > 1
                    pager.isUserInputEnabled = pages.size > 1
                })
                removePagerConstraint()
            }
            is ResultWrapper.Error -> handleError(getString(R.string.gpb_error_document)) { viewModel.retryDocumentReview() }
            else -> { // Loading state handled by payment details
            }
        }
    }

    private fun GpbFragmentReviewBinding.handlePaymentResult(paymentResult: ResultWrapper<PaymentDetails>) {
        binding.loading.isVisible = paymentResult is ResultWrapper.Loading
        if (paymentResult is ResultWrapper.Error) {
            handleError(getString(R.string.gpb_error_payment_details)) { viewModel.retryDocumentReview() }
        }
    }

    private fun GpbFragmentReviewBinding.configureViews() {
        close.isGone = !configuration.showCloseButton
    }

    private fun GpbFragmentReviewBinding.configureOrientation() {
        pager.isVisible = true
        pager.adapter = documentPageAdapter
        TabLayoutMediator(indicator, pager) { tab, _ -> tab.view.isClickable = false }.attach()
    }

    private fun GpbFragmentReviewBinding.setPaymentDetails(paymentDetails: PaymentDetails) {
        recipient.setTextIfDifferent(paymentDetails.recipient)
        iban.setTextIfDifferent(paymentDetails.iban)
        amount.setTextIfDifferent(paymentDetails.amount)
        purpose.setTextIfDifferent(paymentDetails.purpose)
    }

    private fun GpbFragmentReviewBinding.setInputListeners() {
        recipient.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setRecipient(text.toString()) })
        iban.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setIban(text.toString()) })
        amount.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setAmount(text.toString()) })
        amount.addTextChangedListener(amountWatcher)
        purpose.addTextChangedListener(onTextChanged = { text, _, _, _ -> viewModel.setPurpose(text.toString()) })
        recipient.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) recipientLayout.isErrorEnabled = false }
        iban.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) ibanLayout.isErrorEnabled = false }
        amount.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) amountLayout.isErrorEnabled = false }
        purpose.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) purposeLayout.isErrorEnabled = false }
        close.setOnClickListener { listener?.onCloseReview() }
    }

    private fun GpbFragmentReviewBinding.handleValidationResult(messages: List<ValidationMessage>) {
        recipientLayout.error = ""
        ibanLayout.error = ""
        amountLayout.error = ""
        purposeLayout.error = ""

        TransitionManager.beginDelayedTransition(root)
        messages.forEach { message ->
            with(getField(message.field)) {
                if (error.isNullOrEmpty()) {
                    isErrorEnabled = true
                    error = getString(
                        when (message) {
                            is ValidationMessage.Empty -> R.string.gpb_error_input_empty
                            ValidationMessage.InvalidIban -> R.string.gpb_error_input_invalid_iban
                            ValidationMessage.InvalidCurrency -> R.string.gpb_error_input_invalid_Currency
                            ValidationMessage.NoCurrency -> R.string.gpb_error_input_no_currency
                            ValidationMessage.AmountFormat -> R.string.gpb_error_input_amount_format
                        }
                    )
                }
            }
        }
        if (recipientLayout.error.isNullOrEmpty()) recipientLayout.isErrorEnabled = false
        if (ibanLayout.error.isNullOrEmpty()) ibanLayout.isErrorEnabled = false
        if (amountLayout.error.isNullOrEmpty()) amountLayout.isErrorEnabled = false
        if (purposeLayout.error.isNullOrEmpty()) purposeLayout.isErrorEnabled = false
    }

    private fun GpbFragmentReviewBinding.getField(field: PaymentField) = when (field) {
        PaymentField.Recipient -> recipientLayout
        PaymentField.Iban -> ibanLayout
        PaymentField.Amount -> amountLayout
        PaymentField.Purpose -> purposeLayout
    }

    private fun GpbFragmentReviewBinding.handlePaymentState(paymentState: GiniBusiness.PaymentState) {
        (paymentState is GiniBusiness.PaymentState.Loading).let { isLoading ->
            paymentProgress.isVisible = isLoading
            recipientLayout.isEnabled = !isLoading
            ibanLayout.isEnabled = !isLoading
            amountLayout.isEnabled = !isLoading
            purposeLayout.isEnabled = !isLoading
            bank.isEnabled = !isLoading
            payment.text = if (isLoading) "" else getString(R.string.gpb_pay_button)
        }
        when (paymentState) {
            is GiniBusiness.PaymentState.Success -> {
                try {
                    startActivity(paymentState.paymentRequest.bankApp.getIntent(paymentState.paymentRequest.id))
                    viewModel.onBankOpened()
                } catch (exception: ActivityNotFoundException) {
                    handleError(getString(R.string.gpb_error_bank_not_found)) { viewModel.onPayment() }
                }
            }
            is GiniBusiness.PaymentState.Error -> handleError(getString(R.string.gpb_error_open_bank)) { viewModel.onPayment() }
            else -> { // Loading is already handled
            }
        }
    }

    private fun GpbFragmentReviewBinding.handleError(text: String, onRetry: () -> Unit) {
        if (configuration.handleErrorsInternally) {
            showSnackbar(text, onRetry)
        }
    }

    private fun GpbFragmentReviewBinding.showSnackbar(text: String, onRetry: () -> Unit) {
        Snackbar.make(root, text, Snackbar.LENGTH_INDEFINITE)
            .setAnchorView(paymentDetails)
            .setAction(getString(R.string.gpb_snackbar_retry)) { onRetry() }
            .show()
    }

    private fun GpbFragmentReviewBinding.setActionListeners() {
        payment.setOnClickListener {
            viewModel.onPayment()
        }
    }

    private fun GpbFragmentReviewBinding.applyInsets() {
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

    private fun GpbFragmentReviewBinding.removePagerConstraint() {
        root.post {
            ConstraintSet().apply {
                clone(constraintRoot)
                constrainHeight(R.id.pager, pager.height)
                clear(R.id.pager, ConstraintSet.BOTTOM)
                applyTo(constraintRoot)
            }
        }
    }

    private fun GpbFragmentReviewBinding.setKeyboardAnimation() {
        ViewCompat.setWindowInsetsAnimationCallback(paymentDetails, object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
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
                }
                if (startBottom < endBottom) {
                    indicator.isVisible = false
                }
                return bounds
            }

            override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
                if (Build.VERSION.SDK_INT >= 30) {
                    runningAnimations.find { it.typeMask == windowInsetTypesOf(ime = true) }?.let { animation ->
                        paymentDetails.translationY = lerp((endBottom - startBottom).toFloat(), 0f, animation.interpolatedFraction)
                    }
                }
                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                super.onEnd(animation)
                if (Build.VERSION.SDK_INT >= 30) {
                    paymentDetails.translationY = 0f
                }
                if (startBottom > endBottom && pager.isUserInputEnabled) {
                    indicator.isVisible = true
                }
            }
        })
    }
}
