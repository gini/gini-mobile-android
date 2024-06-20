package net.gini.android.merchant.sdk.review

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
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
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.databinding.GmsFragmentReviewBinding
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.preferences.UserPreferences
import net.gini.android.merchant.sdk.review.installApp.InstallAppBottomSheet
import net.gini.android.merchant.sdk.review.installApp.InstallAppForwardListener
import net.gini.android.merchant.sdk.review.model.PaymentDetails
import net.gini.android.merchant.sdk.review.model.ResultWrapper
import net.gini.android.merchant.sdk.review.openWith.OpenWithBottomSheet
import net.gini.android.merchant.sdk.review.openWith.OpenWithForwardListener
import net.gini.android.merchant.sdk.review.openWith.OpenWithPreferences
import net.gini.android.merchant.sdk.review.pager.DocumentPageAdapter
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import net.gini.android.merchant.sdk.util.amountWatcher
import net.gini.android.merchant.sdk.util.autoCleared
import net.gini.android.merchant.sdk.util.clearErrorMessage
import net.gini.android.merchant.sdk.util.extensions.getFontScale
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme
import net.gini.android.merchant.sdk.util.hideErrorMessage
import net.gini.android.merchant.sdk.util.hideKeyboard
import net.gini.android.merchant.sdk.util.setBackgroundTint
import net.gini.android.merchant.sdk.util.setErrorMessage
import net.gini.android.merchant.sdk.util.setTextIfDifferent
import net.gini.android.merchant.sdk.util.showErrorMessage
import net.gini.android.merchant.sdk.util.wrappedWithGiniMerchantTheme
import java.io.File

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
        viewModel.startObservingOpenWithCount()
        viewModel.loadPaymentDetails()

        with(binding) {
            setStateListeners()
            setInputListeners()
            setKeyboardAnimation()
            setEditableFields()
            removePagerConstraintAndSetPreviousHeightIfNeeded(documentPagerHeight)
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
                    viewModel.giniMerchant.documentFlow.collect { handleDocumentResult(it) }
                }
                launch {
                    viewModel.giniMerchant.paymentFlow.collect { handlePaymentResult(it) }
                }
                launch {
                    viewModel.paymentDetails.collect { setPaymentDetails(it) }
                }
                launch {
                    viewModel.paymentValidation.collect { handleValidationResult(it) }
                }
                launch {
                    viewModel.giniMerchant.eventsFlow.collect { handlePaymentState(it) }
                }
                launch {
                    viewModel.isPaymentButtonEnabled.collect { isEnabled ->
                        payment.isEnabled = isEnabled
                        payment.alpha = if (isEnabled) 1f else 0.4f
                        payment.text = if (!isEnabled) "" else getString(R.string.gms_pay_button)
                        paymentProgress.isVisible = !isEnabled
                    }
                }
                launch {
                    viewModel.isInfoBarVisible.collect { visible ->
                        if (visible) showInfoBar() else hideInfoBarAnimated()
                    }
                }
                launch {
                    viewModel.paymentProviderApp.collect { paymentProviderApp ->
                        if (paymentProviderApp != null) {
                            showSelectedPaymentProviderApp(paymentProviderApp)
                            setActionListeners(paymentProviderApp)
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

    private fun GmsFragmentReviewBinding.showSelectedPaymentProviderApp(paymentProviderApp: PaymentProviderApp) {
        paymentProviderApp.icon?.let { appIcon ->
            val roundedDrawable =
                RoundedBitmapDrawableFactory.create(requireContext().resources, appIcon.bitmap).apply {
                    cornerRadius = resources.getDimension(R.dimen.gms_small_2)
                }

            payment.setCompoundDrawablesWithIntrinsicBounds(
                roundedDrawable,
                null,
                null,
                null
            )
        }
        payment.setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
        payment.setTextColor(paymentProviderApp.colors.textColor)
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

    private fun GmsFragmentReviewBinding.setPaymentDetails(paymentDetails: PaymentDetails) {
        recipient.setTextIfDifferent(paymentDetails.recipient)
        iban.setTextIfDifferent(paymentDetails.iban)
        amount.setTextIfDifferent(paymentDetails.amount)
        purpose.setTextIfDifferent(paymentDetails.purpose)
    }

    private fun GmsFragmentReviewBinding.setInputListeners() {
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

    private fun GmsFragmentReviewBinding.handleValidationResult(messages: List<ValidationMessage>) {
        val (fieldsWithError, fieldsWithoutError) = PaymentField.values()
            .map { field -> field to messages.firstOrNull { it.field == field } }
            .partition { (_, message) -> message != null }
        fieldsWithError.forEach { (field, validationMessage) ->
            validationMessage?.let { message ->
                getTextInputLayout(field).apply {
                    if (error.isNullOrEmpty() || getTag(R.id.text_input_layout_tag_is_error_enabled) == null) {
                        setErrorMessage(
                            when (message) {
                                is ValidationMessage.Empty -> when (field) {
                                    PaymentField.Recipient -> R.string.gms_error_input_recipient_empty
                                    PaymentField.Iban -> R.string.gms_error_input_iban_empty
                                    PaymentField.Amount -> R.string.gms_error_input_amount_empty
                                    PaymentField.Purpose -> R.string.gms_error_input_purpose_empty
                                }

                                ValidationMessage.InvalidIban -> R.string.gms_error_input_invalid_iban
                                ValidationMessage.AmountFormat -> R.string.gms_error_input_amount_format
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

    private fun GmsFragmentReviewBinding.getTextInputLayout(field: PaymentField) = when (field) {
        PaymentField.Recipient -> recipientLayout
        PaymentField.Iban -> ibanLayout
        PaymentField.Amount -> amountLayout
        PaymentField.Purpose -> purposeLayout
    }

    private fun GmsFragmentReviewBinding.handlePaymentState(event: GiniMerchant.MerchantSDKEvents) {
        (event is GiniMerchant.MerchantSDKEvents.OnLoading).let { isLoading ->
            recipientLayout.isEnabled = !isLoading
            ibanLayout.isEnabled = !isLoading
            amountLayout.isEnabled = !isLoading
            purposeLayout.isEnabled = !isLoading
        }
        when (event) {
            is GiniMerchant.MerchantSDKEvents.OnFinishedWithPaymentRequestCreated -> {
                if (viewModel.paymentProviderApp.value?.paymentProvider?.gpcSupported() == false) return
                try {
                    val intent =
                        viewModel.paymentProviderApp.value?.getIntent(event.paymentRequestId)
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

    private fun GmsFragmentReviewBinding.setActionListeners(paymentProviderApp: PaymentProviderApp) {
        paymentDetails.setOnClickListener { it.hideKeyboard() }
        payment.setOnClickListener {
            requireActivity().currentFocus?.clearFocus()
            it.hideKeyboard()
            viewModel.onPaymentButtonTapped(requireContext().externalCacheDir)
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

    private fun GmsFragmentReviewBinding.clearFocus() {
        recipient.clearFocus()
        iban.clearFocus()
        amount.clearFocus()
        purpose.clearFocus()
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

    private fun GmsFragmentReviewBinding.setEditableFields() {
        iban.focusable = View.NOT_FOCUSABLE
        recipient.focusable = View.NOT_FOCUSABLE
        purpose.focusable = View.NOT_FOCUSABLE
        amount.focusable = if (viewModel.configuration.isAmountFieldEditable) View.FOCUSABLE else View.NOT_FOCUSABLE
    }


    private fun showInstallAppDialog(paymentProviderApp: PaymentProviderApp) {
        val dialog = InstallAppBottomSheet.newInstance(viewModel.paymentComponent, object :
            InstallAppForwardListener {
            override fun onForwardToBankSelected() {
                redirectToBankApp(paymentProviderApp)
            }
        }, binding.paymentDetailsScrollview.height)
        dialog.show(requireActivity().supportFragmentManager, InstallAppBottomSheet::class.simpleName)
    }

    private fun redirectToBankApp(paymentProviderApp: PaymentProviderApp) {
        listener?.onToTheBankButtonClicked(paymentProviderApp.name)
        viewModel.onPayment()
    }

    private fun showOpenWithDialog(paymentProviderApp: PaymentProviderApp) {
        OpenWithBottomSheet.newInstance(paymentProviderApp, object: OpenWithForwardListener {
            override fun onForwardSelected() {
                viewModel.onForwardToSharePdfTapped(requireContext().externalCacheDir)
            }
        }).also {
            it.show(requireActivity().supportFragmentManager, it::class.java.name)
        }
        viewModel.incrementOpenWithCounter()
    }

    private fun startSharePdfIntent(paymentRequestFile: File) {
        val uriForFile = FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName+".merchant.sdk.fileprovider",
            paymentRequestFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_STREAM, uriForFile)
        }
        startActivity(Intent.createChooser(shareIntent, uriForFile.lastPathSegment))
    }

    private fun handlePaymentNextStep(paymentNextStep: ReviewViewModel.PaymentNextStep) {
        when (paymentNextStep) {
            is ReviewViewModel.PaymentNextStep.SetLoadingVisibility -> {
                binding.loading.isVisible = paymentNextStep.isVisible
            }
            ReviewViewModel.PaymentNextStep.RedirectToBank -> {
                viewModel.paymentProviderApp.value?.let {
                    redirectToBankApp(it)
                }
            }
            ReviewViewModel.PaymentNextStep.ShowOpenWithSheet -> viewModel.paymentProviderApp.value?.let { showOpenWithDialog(it) }
            ReviewViewModel.PaymentNextStep.ShowInstallApp -> viewModel.paymentProviderApp.value?.let { showInstallAppDialog(it) }
            is ReviewViewModel.PaymentNextStep.OpenSharePdf -> {
                binding.loading.isVisible = false
                startSharePdfIntent(paymentNextStep.file)
            }
        }
    }


    private fun handleLoading(isLoading: Boolean) {
        binding.loading.isVisible = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(PAGER_HEIGHT, binding.pager.layoutParams.height)
        super.onSaveInstanceState(outState)
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
