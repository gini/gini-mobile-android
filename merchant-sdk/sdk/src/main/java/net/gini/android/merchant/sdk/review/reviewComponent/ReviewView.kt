package net.gini.android.merchant.sdk.review.reviewComponent

import android.content.Context
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
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.databinding.GmsReviewBinding
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.review.PaymentField
import net.gini.android.merchant.sdk.review.ReviewViewModel
import net.gini.android.merchant.sdk.review.ValidationMessage
import net.gini.android.merchant.sdk.util.amountWatcher
import net.gini.android.merchant.sdk.util.clearErrorMessage
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme
import net.gini.android.merchant.sdk.util.hideErrorMessage
import net.gini.android.merchant.sdk.util.hideKeyboard
import net.gini.android.merchant.sdk.util.setBackgroundTint
import net.gini.android.merchant.sdk.util.setErrorMessage
import net.gini.android.merchant.sdk.util.setTextIfDifferent
import net.gini.android.merchant.sdk.util.showErrorMessage
import org.slf4j.LoggerFactory

internal interface ReviewViewListener {
    fun onPaymentButtonTapped()
}
internal class ReviewView(private val context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val binding = GmsReviewBinding.inflate(getLayoutInflaterWithGiniMerchantTheme(), this)
    private val coroutineContext = Dispatchers.Main
    private var coroutineScope: CoroutineScope? = null

    var onPayButtonTapped: ReviewViewListener? = null
    var reviewComponent: ReviewComponent? = null
        set(value) {
            field = value
            setEditableFields()
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
        setButtonHandlers()
        setInputListeners()
        coroutineScope = CoroutineScope(coroutineContext)
        coroutineScope?.launch {
            launch {
                reviewComponent?.isPaymentButtonEnabled?.collect { isEnabled ->
                    binding.payment.isEnabled = isEnabled
                    binding.payment.alpha = if (isEnabled) 1f else 0.4f
                    binding.payment.text =
                        if (!isEnabled) "" else context.getString(R.string.gms_pay_button)
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
                    binding.amountLayout.isEnabled = !isLoading && reviewComponent?.reviewConfig?.isAmountFieldEditable ?: false
                }
            }
        }
    }

    private fun setButtonHandlers() {
        binding.gmsPaymentDetails.applyInsetter {
            type(navigationBars = true, ime = true) {
                padding(bottom = true)
            }
        }

        binding.gmsPaymentDetails.setOnClickListener { it.hideKeyboard() }
        binding.payment.setOnClickListener {
            it.hideKeyboard()
            onPayButtonTapped?.onPaymentButtonTapped()
        }
    }

    private fun setSelectedPaymentProviderApp(paymentProviderApp: PaymentProviderApp) {
        paymentProviderApp.icon?.let { appIcon ->
            val roundedDrawable =
                RoundedBitmapDrawableFactory.create(context.resources, appIcon.bitmap).apply {
                    cornerRadius = resources.getDimension(R.dimen.gms_small_2)
                }

            binding.payment.setCompoundDrawablesWithIntrinsicBounds(
                roundedDrawable,
                null,
                null,
                null
            )
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

    private fun getTextInputLayout(field: PaymentField) = when (field) {
        PaymentField.Recipient -> binding.recipientLayout
        PaymentField.Iban -> binding.ibanLayout
        PaymentField.Amount -> binding.amountLayout
        PaymentField.Purpose -> binding.purposeLayout
    }

    fun clearEditTextFocus() {
        binding.recipient.clearFocus()
        binding.iban.clearFocus()
        binding.amount.clearFocus()
        binding.purpose.clearFocus()
    }

    private fun setEditableFields() {
        binding.iban.focusable = View.NOT_FOCUSABLE
        binding.recipient.focusable = View.NOT_FOCUSABLE
        binding.purpose.focusable = View.NOT_FOCUSABLE
        binding.amount.focusable = if (reviewComponent?.reviewConfig?.isAmountFieldEditable == true) View.FOCUSABLE else View.NOT_FOCUSABLE
    }

    private fun handleInputFocusChange(hasFocus: Boolean, textInputLayout: TextInputLayout) {
        if (hasFocus) textInputLayout.hideErrorMessage() else textInputLayout.showErrorMessage()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ReviewViewModel::class.java)
    }

}