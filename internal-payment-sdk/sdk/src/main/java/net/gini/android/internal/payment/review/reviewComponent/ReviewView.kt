package net.gini.android.internal.payment.review.reviewComponent

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.databinding.GpsReviewBinding
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.PaymentField
import net.gini.android.internal.payment.review.ValidationMessage
import net.gini.android.internal.payment.utils.amountWatcher
import net.gini.android.internal.payment.utils.extensions.clearErrorMessage
import net.gini.android.internal.payment.utils.extensions.getLayoutInflaterWithGiniPaymentTheme
import net.gini.android.internal.payment.utils.extensions.hideErrorMessage
import net.gini.android.internal.payment.utils.extensions.hideKeyboard
import net.gini.android.internal.payment.utils.extensions.sanitizeAmount
import net.gini.android.internal.payment.utils.extensions.setErrorMessage
import net.gini.android.internal.payment.utils.extensions.setIntervalClickListener
import net.gini.android.internal.payment.utils.extensions.showErrorMessage
import net.gini.android.internal.payment.utils.setBackgroundTint
import net.gini.android.internal.payment.utils.setTextIfDifferent
import org.slf4j.LoggerFactory

interface ReviewViewListener {
    fun onPaymentButtonTapped(paymentDetails: PaymentDetails)

    fun onSelectBankButtonTapped()
}
class ReviewView(private val context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val binding = GpsReviewBinding.inflate(getLayoutInflaterWithGiniPaymentTheme(), this)
    private val coroutineContext = Dispatchers.Main
    private var coroutineScope: CoroutineScope? = null

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
        coroutineScope = CoroutineScope(coroutineContext)
        coroutineScope?.launch {
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
                    paymentDetails = it.copy(
                        amount = it.amount.sanitizeAmount()
                    )
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
        }
    }

    private fun setButtonHandlers() {
        binding.gpsPaymentDetails.applyInsetter {
            type(navigationBars = true, ime = true) {
                padding(bottom = true)
            }
        }

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
            iban.addTextChangedListener(onTextChanged = { text, _, _, _ -> reviewComponent?.setIban(text.toString()) })
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

    companion object {
        private val LOG = LoggerFactory.getLogger(ReviewView::class.java)
    }

}
enum class ReviewFields { RECIPIENT, IBAN, AMOUNT, PURPOSE }