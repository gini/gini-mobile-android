package net.gini.android.health.sdk.paymentcomponent

import android.content.Context
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.inSpans
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.databinding.GhsViewPaymentComponentBinding
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.getLayoutInflaterWithGiniHealthTheme
import net.gini.android.health.sdk.util.setBackgroundTint
import net.gini.android.health.sdk.util.wrappedWithGiniHealthTheme
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class PaymentComponentView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    var paymentComponent: PaymentComponent? = null

    var coroutineContext: CoroutineContext = Dispatchers.Main
    private var coroutineScope: CoroutineScope? = null

    var isPayable: Boolean = false
        set(isPayable) {
            field = isPayable
            if (isPayable) {
                show()
            } else {
                hide()
            }
        }

    private val binding = GhsViewPaymentComponentBinding.inflate(getLayoutInflaterWithGiniHealthTheme(), this)

    init {
        setupMoreInformationLabelAndIcon()
        addButtonInputHandlers()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LOG.debug("onAttachedToWindow")
        if (coroutineScope == null) {
            LOG.debug("Creating coroutine scope")
            coroutineScope = CoroutineScope(coroutineContext)
        }
        coroutineScope?.launch {
            if (paymentComponent == null) {
                LOG.warn("Cannot show payment provider apps: PaymentComponent must be set before showing the PaymentComponentView")
                return@launch
            }
            paymentComponent?.let { pc ->
                LOG.debug("Collecting payment provider apps state and selected payment provider app from PaymentComponent")

                pc.selectedPaymentProviderAppFlow.combine(pc.paymentProviderAppsFlow) { selectedPaymentProviderAppState, paymentProviderAppsState ->
                    selectedPaymentProviderAppState to paymentProviderAppsState
                }.collect { (selectedPaymentProviderAppState, paymentProviderAppsState) ->
                    LOG.debug("Received selected payment provider app state: {}", selectedPaymentProviderAppState)
                    LOG.debug("Received payment provider apps state: {}", paymentProviderAppsState)

                    if (paymentProviderAppsState is PaymentProviderAppsState.Success) {
                        when (selectedPaymentProviderAppState) {
                            is SelectedPaymentProviderAppState.AppSelected -> {
                                enableBankPicker()
                                customizeBankPicker(selectedPaymentProviderAppState.paymentProviderApp)
                                customizePayInvoiceButton(selectedPaymentProviderAppState.paymentProviderApp)
                                enablePayInvoiceButton()
                            }

                            SelectedPaymentProviderAppState.NothingSelected -> {
                                enableBankPicker()
                                restoreBankPickerDefaultState()
                                restorePayInvoiceButtonDefaultState()
                                disablePayInvoiceButton()
                            }
                        }
                    } else {
                        disableBankPicker()
                        disablePayInvoiceButton()
                    }
                }
            }
        }
    }

    private fun restoreBankPickerDefaultState() {
        LOG.debug("Restoring bank picker default state")
        context?.wrappedWithGiniHealthTheme()?.let { context ->
            binding.ghsSelectBankPicker.text = context.getString(R.string.ghs_select_bank)
            binding.ghsSelectBankPicker.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(context, R.drawable.ghs_chevron_down_icon),
                null
            )
        }
    }

    private fun customizeBankPicker(paymentProviderApp: PaymentProviderApp) {
        LOG.debug("Customizing bank picker for payment provider app: {}", paymentProviderApp.name)
        context?.wrappedWithGiniHealthTheme()?.let { context ->
            binding.ghsSelectBankPicker.text = paymentProviderApp.name
            binding.ghsSelectBankPicker.setCompoundDrawablesWithIntrinsicBounds(
                paymentProviderApp.icon,
                null,
                ContextCompat.getDrawable(context, R.drawable.ghs_chevron_down_icon),
                null
            )
        }
    }

    private fun customizePayInvoiceButton(paymentProviderApp: PaymentProviderApp) {
        LOG.debug("Customizing pay invoice button for payment provider app: {}", paymentProviderApp.name)
        binding.ghsPayInvoiceButton.setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
        binding.ghsPayInvoiceButton.setTextColor(paymentProviderApp.colors.textColor)
    }

    private fun enableBankPicker() {
        LOG.debug("Enabling bank picker")
        binding.ghsSelectBankPicker.isEnabled = true
    }

    private fun disableBankPicker() {
        LOG.debug("Disabling bank picker")
        binding.ghsSelectBankPicker.isEnabled = false
    }

    private fun enablePayInvoiceButton() {
        LOG.debug("Enabling pay invoice button")
        binding.ghsPayInvoiceButton.isEnabled = true
        binding.ghsPayInvoiceButton.alpha = 1f
    }

    private fun disablePayInvoiceButton() {
        LOG.debug("Disabling pay invoice button")
        binding.ghsPayInvoiceButton.isEnabled = false
        binding.ghsPayInvoiceButton.alpha = 0.4f
    }

    private fun restorePayInvoiceButtonDefaultState() {
        LOG.debug("Restoring pay invoice button default state")
        context?.wrappedWithGiniHealthTheme()?.let { context ->
            binding.ghsPayInvoiceButton.setBackgroundTint(
                ContextCompat.getColor(
                    context,
                    R.color.ghs_unelevated_button_background
                )
            )
            binding.ghsPayInvoiceButton.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.ghs_unelevated_button_text
                )
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LOG.debug("onDetachedFromWindow")
        coroutineScope?.cancel()
        coroutineScope = null
    }

    private fun setupMoreInformationLabelAndIcon() {
        binding.ghsMoreInformationLabel.movementMethod = LinkMovementMethod.getInstance()
        addMoreInformationUnderlinedClickableText {
            onMoreInformationClicked()
        }
        binding.ghsInfoCircleIcon.setOnClickListener {
            onMoreInformationClicked()
        }
    }

    fun prepareForReuse() {
        isPayable = false
        disablePayInvoiceButton()
        restorePayInvoiceButtonDefaultState()
        restoreBankPickerDefaultState()
        disableBankPicker()
    }

    private fun show() {
        LOG.debug("Showing payment component")
        binding.ghsPayInvoiceButton.visibility = VISIBLE
        binding.ghsMoreInformationLabel.visibility = VISIBLE
        binding.ghsSelectBankLabel.visibility = VISIBLE
        binding.ghsSelectBankPicker.visibility = VISIBLE
        binding.ghsInfoCircleIcon.visibility = VISIBLE
        binding.ghsPoweredByGiniLabel.visibility = VISIBLE
        binding.ghsGiniLogo.visibility = VISIBLE
    }

    private fun hide() {
        LOG.debug("Hiding payment component")
        binding.ghsPayInvoiceButton.visibility = GONE
        binding.ghsMoreInformationLabel.visibility = GONE
        binding.ghsSelectBankLabel.visibility = GONE
        binding.ghsSelectBankPicker.visibility = GONE
        binding.ghsInfoCircleIcon.visibility = GONE
        binding.ghsPoweredByGiniLabel.visibility = GONE
        binding.ghsGiniLogo.visibility = GONE
    }

    private fun addMoreInformationUnderlinedClickableText(clickListener: () -> Unit) {
        binding.ghsMoreInformationLabel.text = buildSpannedString {
            append(ContextCompat.getString(context, R.string.ghs_more_information_label))
            append(" ")
            append(buildSpannedString {
                color(ContextCompat.getColor(context, R.color.ghs_payment_component_caption)) {
                    bold {
                        inSpans(object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                clickListener()
                            }
                        }) {
                            val underlinedPart =
                                ContextCompat.getString(context, R.string.ghs_more_information_underlined_part)
                            append(underlinedPart.replace(" ".toRegex(), "\u00A0"))
                        }
                    }
                }

            })
        }
    }

    private fun onMoreInformationClicked() {
        if (paymentComponent == null) {
            LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
        }
        paymentComponent?.listener?.onMoreInformationClicked()
    }

    private fun addButtonInputHandlers() {
        binding.ghsSelectBankPicker.setOnClickListener {
            if (paymentComponent == null) {
                LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
            }
            paymentComponent?.listener?.onBankPickerClicked()
        }
        binding.ghsPayInvoiceButton.setOnClickListener {
            if (paymentComponent == null) {
                LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
            }
            paymentComponent?.listener?.onPayInvoiceClicked()
        }
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponentView::class.java)
    }
}