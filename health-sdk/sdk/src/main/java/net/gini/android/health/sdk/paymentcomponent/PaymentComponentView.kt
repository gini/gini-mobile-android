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
import net.gini.android.health.sdk.util.setIntervalClickListener
import net.gini.android.health.sdk.util.wrappedWithGiniHealthTheme
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

/**
 * The [PaymentComponentView] is a custom view widget and the main entry point for users. It allows them to pick a bank
 * and initiate the payment process. In addition, it also allows users to view more information about the payment feature.
 *
 * It is hidden by default and should be added to the layout of each invoice item.
 *
 * When creating the view holder for the invoice item, pass the [PaymentComponent] instance to the view holder:
 *
 * ```
 * val paymentComponentView = view.findViewById(R.id.payment_component)
 * paymentComponentView.paymentComponent = paymentComponent
 * ```
 *
 * When binding the view holder of the invoice item, prepare the [PaymentComponentView] for reuse, set the payable state
 * and the document id:
 *
 * ```
 * viewHolder.paymentComponentView.prepareForReuse()
 * viewHolder.paymentComponentView.isPayable = invoiceItem.isPayable
 * viewHolder.paymentComponentView.documentId = invoiceItem.documentId
 * ```
 *
 * _Note_: The [PaymentComponentView] will only be visible if its [PaymentComponentView.isPayable] property is `true`.
 */
class PaymentComponentView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    /**
     * The [PaymentComponent] instance which provides the data and state for the [PaymentComponentView].
     */
    var paymentComponent: PaymentComponent? = null

    internal var coroutineContext: CoroutineContext = Dispatchers.Main
    private var coroutineScope: CoroutineScope? = null

    /**
     * Sets the payable state of the [PaymentComponentView]. If `true`, the view will be shown, otherwise it will be hidden.
     */
    var isPayable: Boolean = false
        set(isPayable) {
            field = isPayable
            if (isPayable) {
                show()
            } else {
                hide()
            }
        }

    /**
     * The document id of the invoice item. This will be returned in the [PaymentComponent.Listener.onPayInvoiceClicked] method.
     */
    var documentId: String? = null

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
            binding.ghsSelectBankPicker.ghsPaymentProviderAppIconHolder.root.visibility = View.GONE
            binding.ghsSelectBankPicker.ghsSelectBankButton.text = context.getString(R.string.ghs_select_bank)
            binding.ghsSelectBankPicker.ghsSelectBankButton.setCompoundDrawablesWithIntrinsicBounds(
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
            binding.ghsSelectBankPicker.ghsSelectBankButton.text = paymentProviderApp.name
            binding.ghsSelectBankPicker.ghsSelectBankButton.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(context, R.drawable.ghs_chevron_down_icon),
                null
            )
            binding.ghsSelectBankPicker.ghsPaymentProviderAppIconHolder.ghsPaymentProviderIcon.setImageDrawable(paymentProviderApp.icon)
            binding.ghsSelectBankPicker.ghsPaymentProviderAppIconHolder.root.visibility = View.VISIBLE
        }
    }

    private fun customizePayInvoiceButton(paymentProviderApp: PaymentProviderApp) {
        LOG.debug("Customizing pay invoice button for payment provider app: {}", paymentProviderApp.name)
        binding.ghsPayInvoiceButton.setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
        binding.ghsPayInvoiceButton.setTextColor(paymentProviderApp.colors.textColor)
    }

    private fun enableBankPicker() {
        LOG.debug("Enabling bank picker")
        binding.ghsSelectBankPicker.ghsSelectBankButton.isEnabled = true
    }

    private fun disableBankPicker() {
        LOG.debug("Disabling bank picker")
        binding.ghsSelectBankPicker.ghsSelectBankButton.isEnabled = false
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
        binding.ghsInfoCircleIcon.setIntervalClickListener {
            onMoreInformationClicked()
        }
    }

    /**
     * Resets the internal state of the [PaymentComponentView] to its default state. This should be called before the view is reused.
     */
    fun prepareForReuse() {
        isPayable = false
        documentId = null
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
        binding.ghsSelectBankPicker.root.visibility = VISIBLE
        binding.ghsInfoCircleIcon.visibility = VISIBLE
        binding.ghsPoweredByGini.visibility = VISIBLE
    }

    private fun hide() {
        LOG.debug("Hiding payment component")
        binding.ghsPayInvoiceButton.visibility = GONE
        binding.ghsMoreInformationLabel.visibility = GONE
        binding.ghsSelectBankLabel.visibility = GONE
        binding.ghsSelectBankPicker.root.visibility = GONE
        binding.ghsInfoCircleIcon.visibility = GONE
        binding.ghsPoweredByGini.visibility = GONE
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
        binding.ghsSelectBankPicker.ghsSelectBankButton.setIntervalClickListener {
            if (paymentComponent == null) {
                LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
            }
            paymentComponent?.listener?.onBankPickerClicked()
        }
        binding.ghsPayInvoiceButton.setIntervalClickListener {
            if (paymentComponent == null) {
                LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
            }
            documentId?.let { docId ->
                paymentComponent?.listener?.onPayInvoiceClicked(docId)
            } ?: run {
                LOG.warn("Cannot call PaymentComponent's listener: documentId must be set before showing the PaymentComponentView")
            }
        }
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponentView::class.java)
    }
}