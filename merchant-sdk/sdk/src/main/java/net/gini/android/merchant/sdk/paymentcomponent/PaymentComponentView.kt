package net.gini.android.merchant.sdk.paymentcomponent

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.databinding.GmsViewPaymentComponentBinding
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.util.getLayoutInflaterWithGiniMerchantTheme
import net.gini.android.merchant.sdk.util.setBackgroundTint
import net.gini.android.merchant.sdk.util.setIntervalClickListener
import net.gini.android.merchant.sdk.util.wrappedWithGiniMerchantTheme
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
internal class PaymentComponentView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    /**
     * The [PaymentComponent] instance which provides the data and state for the [PaymentComponentView].
     */
    var paymentComponent: PaymentComponent? = null

    internal var coroutineContext: CoroutineContext = Dispatchers.Main

    @VisibleForTesting
    internal var coroutineScope: CoroutineScope? = null

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

    private val binding = GmsViewPaymentComponentBinding.inflate(getLayoutInflaterWithGiniMerchantTheme(), this)

    init {
        addButtonInputHandlers()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LOG.debug("onAttachedToWindow")
        if (coroutineScope == null) {
            LOG.debug("Creating coroutine scope")
            coroutineScope = CoroutineScope(coroutineContext)
        }
        checkPaymentComponentHeight()
        coroutineScope?.launch {
            if (paymentComponent == null) {
                LOG.warn("Cannot show payment provider apps: PaymentComponent must be set before showing the PaymentComponentView")
                return@launch
            }
            paymentComponent?.checkReturningUser()
            paymentComponent?.let { pc ->
                LOG.debug("Collecting payment provider apps state and selected payment provider app from PaymentComponent")
                launch {
                    pc.selectedPaymentProviderAppFlow.combine(pc.paymentProviderAppsFlow) { selectedPaymentProviderAppState, paymentProviderAppsState ->
                        selectedPaymentProviderAppState to paymentProviderAppsState
                    }.collect { (selectedPaymentProviderAppState, paymentProviderAppsState) ->
                        LOG.debug(
                            "Received selected payment provider app state: {}",
                            selectedPaymentProviderAppState
                        )
                        LOG.debug(
                            "Received payment provider apps state: {}",
                            paymentProviderAppsState
                        )
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
                launch {
                    pc.returningUserFlow.collect { isReturning ->
                        binding.gmsMoreInformation.visibility = if (isReturning) View.GONE else View.VISIBLE
                        binding.gmsSelectBankLabel.visibility = if (isReturning) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun checkPaymentComponentHeight() {
        if (resources.getDimension(R.dimen.gms_payment_component_height) >= resources.getDimension(R.dimen.gms_accessibility_min_height)) {
            binding.gmsSelectBankPicker.layoutParams.height = resources.getDimension(R.dimen.gms_payment_component_height).toInt()
        }
    }

    private fun restoreBankPickerDefaultState() {
        LOG.debug("Restoring bank picker default state")
        context?.wrappedWithGiniMerchantTheme()?.let { context ->
            binding.gmsPayInvoiceButton.visibility = View.GONE
            binding.gmsPaymentProviderAppIconHolder.root.visibility = View.GONE
            binding.gmsSelectBankButton.text = context.getString(R.string.gms_select_bank)
            binding.gmsSelectBankButton.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(context, R.drawable.gms_chevron_down_icon),
                null
            )
        }
    }

    private fun customizeBankPicker(paymentProviderApp: PaymentProviderApp) {
        LOG.debug("Customizing bank picker for payment provider app: {}", paymentProviderApp.name)
        context?.wrappedWithGiniMerchantTheme()?.let { context ->
            binding.gmsSelectBankButton.text = ""
            binding.gmsSelectBankButton.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(context, R.drawable.gms_chevron_down_icon),
                null
            )
            binding.gmsPaymentProviderAppIconHolder.gmsPaymentProviderIcon.setImageDrawable(paymentProviderApp.icon)
            binding.gmsPaymentProviderAppIconHolder.root.visibility = View.VISIBLE
            binding.gmsPaymentProviderAppIconHolder.root.contentDescription = paymentProviderApp.name
        }
    }

    private fun customizePayInvoiceButton(paymentProviderApp: PaymentProviderApp) {
        LOG.debug("Customizing pay invoice button for payment provider app: {}", paymentProviderApp.name)
        binding.gmsPayInvoiceButton.setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
        binding.gmsPayInvoiceButton.setTextColor(paymentProviderApp.colors.textColor)
        binding.gmsPayInvoiceButton.visibility = View.VISIBLE
    }

    private fun enableBankPicker() {
        LOG.debug("Enabling bank picker")
        binding.gmsSelectBankButton.isEnabled = true
    }

    private fun disableBankPicker() {
        LOG.debug("Disabling bank picker")
        binding.gmsSelectBankButton.isEnabled = false
    }

    private fun enablePayInvoiceButton() {
        LOG.debug("Enabling pay invoice button")
        binding.gmsPayInvoiceButton.isEnabled = true
        binding.gmsPayInvoiceButton.alpha = 1f
    }

    private fun disablePayInvoiceButton() {
        LOG.debug("Disabling pay invoice button")
        binding.gmsPayInvoiceButton.isEnabled = false
        binding.gmsPayInvoiceButton.alpha = 0.4f
    }

    private fun restorePayInvoiceButtonDefaultState() {
        LOG.debug("Restoring pay invoice button default state")
        context?.wrappedWithGiniMerchantTheme()?.let { context ->
            binding.gmsPayInvoiceButton.setBackgroundTint(
                ContextCompat.getColor(
                    context,
                    R.color.gms_unelevated_button_background
                )
            )
            binding.gmsPayInvoiceButton.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.gms_unelevated_button_text
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
        binding.gmsSelectBankLabel.visibility = VISIBLE
        binding.gmsSelectBankPicker.visibility = VISIBLE
        binding.gmsPoweredByGini.visibility = VISIBLE
    }

    private fun hide() {
        LOG.debug("Hiding payment component")
        binding.gmsSelectBankLabel.visibility = GONE
        binding.gmsSelectBankPicker.visibility = GONE
        binding.gmsPoweredByGini.visibility = GONE
    }

    private fun addButtonInputHandlers() {
        binding.gmsSelectBankButton.setIntervalClickListener {
            if (paymentComponent == null) {
                LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
            }
            paymentComponent?.listener?.onBankPickerClicked()
        }
        binding.gmsPayInvoiceButton.setIntervalClickListener {
            if (paymentComponent == null) {
                LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
            }
            documentId?.let { docId ->
                coroutineScope?.launch {
                    paymentComponent?.onPayInvoiceClicked(docId)
                }
            } ?: run {
                LOG.warn("Cannot call PaymentComponent's listener: documentId must be set before showing the PaymentComponentView")
            }
        }
        binding.gmsMoreInformation.setIntervalClickListener {
            if (paymentComponent == null) {
                LOG.warn("Cannot call PaymentComponent's listener: PaymentComponent must be set before showing the PaymentComponentView")
            }
            paymentComponent?.listener?.onMoreInformationClicked()
        }
    }

    private companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponentView::class.java)
    }
}