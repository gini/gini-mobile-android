package net.gini.android.internal.payment.review.reviewComponent

import android.content.Context
import android.text.Editable
import android.os.Build
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputLayout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.gini.android.health.api.response.IngredientBrandType
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.databinding.GpsReviewBinding
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.PaymentField
import net.gini.android.internal.payment.review.ReviewViewStateLandscape
import net.gini.android.internal.payment.review.ValidationMessage
import net.gini.android.internal.payment.utils.amountWatcher
import net.gini.android.internal.payment.utils.extensions.bottomSystemBarOverlap
import net.gini.android.internal.payment.utils.extensions.clearErrorMessage
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentTheme
import net.gini.android.internal.payment.utils.extensions.hideErrorMessage
import net.gini.android.internal.payment.utils.extensions.hideKeyboard
import net.gini.android.internal.payment.utils.extensions.hideKeyboardFully
import net.gini.android.internal.payment.utils.extensions.isLandscapeOrientation
import net.gini.android.internal.payment.utils.extensions.setErrorMessage
import net.gini.android.internal.payment.utils.extensions.setIntervalClickListener
import net.gini.android.internal.payment.utils.extensions.showErrorMessage
import net.gini.android.internal.payment.utils.extensions.systemBottomInset
import net.gini.android.internal.payment.utils.setBackgroundTint
import net.gini.android.internal.payment.utils.setTextIfDifferent
import org.slf4j.LoggerFactory
import java.util.Locale

interface ReviewViewListener {
    fun onPaymentButtonTapped(paymentDetails: PaymentDetails)

    fun onSelectBankButtonTapped()
}

/**
 * Represents the view with all the fields which hold the payment details.
 */
class ReviewView(private val context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val binding = GpsReviewBinding.inflate(getLayoutInflaterWithGiniPaymentTheme(), this)
    private val coroutineContext = Dispatchers.Main
    private var coroutineScope: CoroutineScope? = null
    private val internalPaymentModule
        get() = reviewComponent?.giniInternalPaymentModule
    private var amountAccessibilityDelegate: AccessibilityDelegateCompat? = null

    var listener: ReviewViewListener? = null
    var reviewComponent: ReviewComponent? = null
        set(value) {
            field = value
            setEditableFields()
            binding.gpsSelectBankLayout.isVisible = reviewComponent?.shouldShowBankSelectionButton() == true
        }
    var paymentDetails: PaymentDetails? = null
        set(value) {
            field = value
            field?.let {
                fillPaymentDetails(it)
            }
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setDisabledIcons()
        setButtonHandlers()
        setInputListeners()
        disableSuffixAccessibility(binding.amountLayout)
        setupAmountAccessibilityDelegate()
        coroutineScope = CoroutineScope(coroutineContext)
        coroutineScope?.launch {
            launch {
                setIngredientBrandVisibility()
            }
            launch {
                reviewComponent?.isPaymentButtonEnabled?.collect { isEnabled ->
                    binding.payment.isEnabled = isEnabled
                    binding.payment.alpha = if (isEnabled) 1f else 0.4f
                }
            }
            launch {
                reviewComponent?.paymentComponent?.selectedPaymentProviderAppFlow?.collect {
                    if (it is SelectedPaymentProviderAppState.AppSelected) {
                        setSelectedPaymentProviderApp(it.paymentProviderApp)
                    } else {
                        LOG.error("No selected payment provider app")
                    }
                }
            }
            launch {
                reviewComponent?.paymentDetails?.collect {
                    paymentDetails = it
                }
            }
            launch {
                reviewComponent?.paymentValidation?.collect {
                    handleValidationResult(it)
                }
            }
            launch {
                reviewComponent?.loadingFlow?.collect { isLoading ->
                    binding.paymentProgress.isVisible = isLoading
                    binding.amountLayout.isEnabled = !isLoading &&
                            (reviewComponent?.reviewConfig?.editableFields?.contains(ReviewFields.AMOUNT) ?: false)
                }
            }
            launch {
                reviewComponent?.reviewViewStateInLandscapeMode?.collect { reviewViewState ->
                    binding.gpsFieldsLayout?.isVisible = reviewViewState == ReviewViewStateLandscape.EXPANDED
                }
            }
        }
    }
    private fun disableSuffixAccessibility(textInputLayout: TextInputLayout) {
        textInputLayout.post {
            val suffixView = textInputLayout.findViewById<View>(
                com.google.android.material.R.id.textinput_suffix_text
            )
            suffixView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }
    private fun setupAmountAccessibilityDelegate() {
        if (amountAccessibilityDelegate == null) {
            amountAccessibilityDelegate = object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    val text = binding.amount.text?.toString()?.trim().orEmpty()
                    val suffix = binding.amountLayout.suffixText
                    info.text = if (text.isNotEmpty()) {
                        "$text $suffix"
                    } else {
                        context.getString(R.string.gps_amount_hint)
                    }
                }
            }
            ViewCompat.setAccessibilityDelegate(binding.amount, amountAccessibilityDelegate)
        }
    }

    /**
     * [handleViewInsets] -> Reserves space at the bottom of the payment details view for the
     * system navigation bar and for the keyboard, so the payment button is never covered by them.
     *
     * - The bottom sheet case needs no handling, system insets are applied automatically there.
     * - Two sources are used on every API level, because neither one is reliable on its own:
     *   - A [ViewCompat.setOnApplyWindowInsetsListener], which is the normal mechanism and also
     *     delivers keyboard (ime) changes while they animate.
     *   - The window insets read directly with [ViewCompat.getRootWindowInsets]. This is required
     *     because a host app whose theme has an ActionBar renders our fragments inside AppCompat's
     *     `ActionBarOverlayLayout`, which consumes the window insets. Insets are then never
     *     dispatched to any view below it, so the listener above is never called and the padding
     *     would stay at zero. Reading the window directly is unaffected by that consumption.
     *
     * Both sources set the same absolute padding, so applying them together is safe.
     */
    private fun handleViewInsets() {
        if (isReviewViewInBottomSheet()) return

        ViewCompat.setOnApplyWindowInsetsListener(binding.gpsPaymentDetails) { _, insets ->
            applyBottomSystemInsets(insets)
            insets
        }
        requestApplyInsetsWhenAttached(binding.gpsPaymentDetails)

        removeOnLayoutChangeListener(bottomInsetLayoutListener)
        addOnLayoutChangeListener(bottomInsetLayoutListener)
        applyBottomSystemInsetsFromWindow()
    }

    /**
     * Re-reads the window insets on every layout pass. This keeps the bottom padding correct when
     * the listener in [handleViewInsets] is never called because a host view consumed the insets,
     * for example after a rotation or when the keyboard resizes the window.
     */
    private val bottomInsetLayoutListener =
        OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyBottomSystemInsetsFromWindow() }

    private fun applyBottomSystemInsetsFromWindow() {
        ViewCompat.getRootWindowInsets(this)?.let { applyBottomSystemInsets(it) }
    }

    /**
     * Applies as bottom padding only the part of the navigation bar or keyboard that actually
     * overlaps the payment details view.
     *
     * See [bottomSystemBarOverlap] for why the raw inset value cannot be used as the padding.
     */
    private fun applyBottomSystemInsets(insets: WindowInsetsCompat) {
        val imeInsetBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val target = binding.gpsPaymentDetails
        val overlap = target.bottomSystemBarOverlap(insets.systemBottomInset())
        if (target.paddingBottom != overlap) {
            target.updatePadding(bottom = overlap)
        }
        if (isAndroid15OrAbove() && insets.isVisible(WindowInsetsCompat.Type.ime())) {
            scrollFocusedViewAboveKeyboard(imeInsetBottom)
        }
    }

    private fun scrollFocusedViewAboveKeyboard(keyboardBottom: Int) {
        if (!resources.isLandscapeOrientation() || !rootView.isAttachedToWindow) return

        findParentScrollView(this)?.let { scrollView ->
            val focusedView = rootView.findFocus() as? EditText ?: return

            val location = IntArray(2)
            focusedView.getLocationOnScreen(location)
            val viewBottom = location[1] + focusedView.height

            val keyboardTop = rootView.height - keyboardBottom

            if (viewBottom > keyboardTop) {
                val scrollAmount = viewBottom - keyboardTop
                scrollView.post {
                    scrollView.smoothScrollBy(0, scrollAmount)
                }
            }
        }
    }

    private fun findParentScrollView(view: View): ScrollView? {
        var current = view.parent
        while (current is ViewGroup) {
            if (current is ScrollView) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun isReviewViewInBottomSheet(): Boolean {
        var current: View? = this
        while (current != null) {
            val parent = current.parent
            if (parent is CoordinatorLayout) {
                try {
                    BottomSheetBehavior.from(current)
                    return true
                } catch (_: IllegalArgumentException) {
                    // Expected: view is not controlled by BottomSheetBehavior
                }
            }
            current = parent as? View
        }
        return false
    }

    private fun isAndroid15OrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }


    /**
     * Triggers an insets pass for [target] as soon as this view is attached to the window.
     *
     * A freshly installed [ViewCompat.setOnApplyWindowInsetsListener] only runs on the next insets
     * dispatch. When the listener is installed after the initial dispatch it may never run until an
     * unrelated insets event occurs, so we explicitly request an insets pass once we are attached.
     */
    private fun requestApplyInsetsWhenAttached(target: View) {
        if (isAttachedToWindow) {
            ViewCompat.requestApplyInsets(target)
        } else {
            addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    ViewCompat.requestApplyInsets(target)
                    v.removeOnAttachStateChangeListener(this)
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })
        }
    }

    private fun setButtonHandlers() {
        handleViewInsets()
        binding.gpsPaymentDetails.setOnClickListener { it.hideKeyboard() }
        binding.payment.setIntervalClickListener {
            it.hideKeyboard()
            binding.root.clearFocus()
            reviewComponent?.paymentDetails?.value?.let { paymentDetails ->
                val areFieldsValid = reviewComponent?.validatePaymentDetails(paymentDetails)
                if (areFieldsValid == true) {
                    listener?.onPaymentButtonTapped(paymentDetails)
                }
            }
        }

        binding.payment.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                clearInputFieldsFocus()
                view.hideKeyboardFully()
            }
        }
    }

    private fun setSelectedPaymentProviderApp(paymentProviderApp: PaymentProviderApp) {
        paymentProviderApp.icon?.let { appIcon ->
            val roundedDrawable =
                RoundedBitmapDrawableFactory.create(context.resources, appIcon.bitmap).apply {
                    cornerRadius = resources.getDimension(R.dimen.gps_small_2)
                }

            if (reviewComponent?.shouldShowBankSelectionButton() == true) {
                binding.gpsPaymentProviderAppIconHolder.gpsPaymentProviderIcon.setImageDrawable(roundedDrawable)
                binding.gpsSelectBankButton.setOnClickListener { listener?.onSelectBankButtonTapped() }
                binding.gpsSelectBankButton.setOnFocusChangeListener{
                    view, hasFocus ->
                    if (hasFocus) {
                        clearInputFieldsFocus()
                        view.hideKeyboardFully()
                    }
                }
            } else {
                binding.payment.setCompoundDrawablesWithIntrinsicBounds(
                    roundedDrawable,
                    null,
                    null,
                    null
                )

                // Adding negative icon padding in order to center the text on the button.
                binding.payment.iconPadding = -roundedDrawable.intrinsicWidth
            }
        }
        binding.payment.setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
        binding.payment.setTextColor(paymentProviderApp.colors.textColor)
    }

    private fun fillPaymentDetails(paymentDetails: PaymentDetails) {
        binding.recipient.setTextIfDifferent(paymentDetails.recipient)
        binding.iban.setTextIfDifferent(paymentDetails.iban)
        binding.amount.setTextIfDifferent(paymentDetails.amount)
        binding.purpose.setTextIfDifferent(paymentDetails.purpose)
    }

    private fun setInputListeners() {
        with(binding) {
            recipient.addTextChangedListener(onTextChanged = { text, _, _, _ -> reviewComponent?.setRecipient(text.toString()) })
            iban.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // No action needed before the text changes
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val upperCaseText = s.toString().uppercase(Locale.getDefault())
                    if (s.toString() != upperCaseText) {
                        val cursorPosition = iban.selectionStart
                        iban.removeTextChangedListener(this)
                        iban.setText(upperCaseText)
                        iban.setSelection(cursorPosition)
                        iban.addTextChangedListener(this)
                    }
                    reviewComponent?.setIban(upperCaseText)
                }

                override fun afterTextChanged(s: Editable?) {
                    // No action needed after the text changes
                }
            })
            amount.addTextChangedListener(onTextChanged = { text, _, _, _ -> reviewComponent?.setAmount(text.toString()) })
            amount.addTextChangedListener(amountWatcher)
            purpose.addTextChangedListener(onTextChanged = { text, _, _, _ -> reviewComponent?.setPurpose(text.toString()) })
            recipient.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, recipientLayout) }
            iban.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, ibanLayout) }
            amount.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, amountLayout) }
            purpose.setOnFocusChangeListener { _, hasFocus -> handleInputFocusChange(hasFocus, purposeLayout) }
        }
    }

    private fun handleValidationResult(messages: List<ValidationMessage>) {
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
                                    PaymentField.Recipient -> R.string.gps_error_input_recipient_empty
                                    PaymentField.Iban -> R.string.gps_error_input_iban_empty
                                    PaymentField.Amount -> R.string.gps_error_input_amount_empty
                                    PaymentField.Purpose -> R.string.gps_error_input_reference_number_empty
                                }

                                ValidationMessage.InvalidIban -> R.string.gps_error_input_invalid_iban
                                ValidationMessage.AmountFormat -> R.string.gps_error_input_amount_format
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

    private fun getTextInputLayout(field: PaymentField) = when (field) {
        PaymentField.Recipient -> binding.recipientLayout
        PaymentField.Iban -> binding.ibanLayout
        PaymentField.Amount -> binding.amountLayout
        PaymentField.Purpose -> binding.purposeLayout
    }

    private fun clearInputFieldsFocus() {
        binding.recipient.clearFocus()
        binding.iban.clearFocus()
        binding.amount.clearFocus()
        binding.purpose.clearFocus()
    }

    private fun setEditableFields() {
        val editableFields = reviewComponent?.reviewConfig?.editableFields
        binding.iban.focusable = if (editableFields?.contains(ReviewFields.IBAN) == true) {
            View.FOCUSABLE
        } else {
            View.NOT_FOCUSABLE
        }
        binding.recipient.focusable = if (editableFields?.contains(ReviewFields.RECIPIENT) == true) {
            View.FOCUSABLE
        } else {
            View.NOT_FOCUSABLE
        }
        binding.purpose.focusable = if (editableFields?.contains(ReviewFields.PURPOSE) == true) {
            View.FOCUSABLE
        } else {
            View.NOT_FOCUSABLE
        }
        binding.amount.focusable = if (editableFields?.contains(ReviewFields.AMOUNT) == true) {
            View.FOCUSABLE
        } else {
            View.NOT_FOCUSABLE
        }
    }

    private fun setDisabledIcon(text: String, textView:TextInputLayout) {
        // Padding the end of the text is needed to add space between the text and the image, and because setting the imageSpan replaces the character in the string, it does not add to the beginning or the end of it.
        val spannableString = SpannableStringBuilder(text.padEnd(text.length + 3, ' '))
        val imageSpan = ImageSpan(context, R.drawable.gps_lock)
        spannableString.setSpan(imageSpan, spannableString.length - 1, spannableString.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        textView.hint = spannableString;
    }

    private fun setDisabledIcons() {
        if (reviewComponent?.reviewConfig?.editableFields?.contains(ReviewFields.IBAN) == false) {
            setDisabledIcon(context.getString(R.string.gps_iban_hint), binding.ibanLayout)
            binding.iban.isEnabled = false
        }
        if (reviewComponent?.reviewConfig?.editableFields?.contains(ReviewFields.RECIPIENT) == false) {
            setDisabledIcon(context.getString(R.string.gps_recipient_hint), binding.recipientLayout)
            binding.recipient.isEnabled = false
        }
        if (reviewComponent?.reviewConfig?.editableFields?.contains(ReviewFields.PURPOSE) == false) {
            setDisabledIcon(context.getString(R.string.gps_reference_number_hint), binding.purposeLayout)
            binding.purpose.isEnabled = false
        }
    }

    private fun handleInputFocusChange(hasFocus: Boolean, textInputLayout: TextInputLayout) {
        if (hasFocus) textInputLayout.hideErrorMessage() else textInputLayout.showErrorMessage()
    }

    private fun setIngredientBrandVisibility() {
        binding.gpsPoweredByGiniLayout.root.visibility =
            if (internalPaymentModule?.getIngredientBrandVisibility() == IngredientBrandType.FULL_VISIBLE)
                View.VISIBLE else View.INVISIBLE
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ReviewView::class.java)
    }

}
@kotlinx.parcelize.Parcelize
enum class ReviewFields : Parcelable {
    IBAN, AMOUNT, RECIPIENT, PURPOSE
}