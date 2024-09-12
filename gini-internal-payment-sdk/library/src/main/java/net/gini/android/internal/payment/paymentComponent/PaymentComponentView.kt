package net.gini.android.internal.payment.paymentComponent

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.databinding.GpsPaymentProviderIconHolderBinding
import net.gini.android.internal.payment.databinding.GpsViewPaymentComponentBinding
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.util.extensions.getLayoutInflaterWithGiniPaymentThemeAndLocale
import net.gini.android.internal.payment.util.extensions.setBackgroundTint
import net.gini.android.internal.payment.util.extensions.wrappedWithGiniPaymentTheme
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
        set(value) {
            field = value
            initViews()
        }

    internal var coroutineContext: CoroutineContext = Dispatchers.Main

    @VisibleForTesting
    internal var coroutineScope: CoroutineScope? = null

    /**
     * The document id of the invoice item. This will be returned in the [PaymentComponent.Listener.onPayInvoiceClicked] method.
     */
    var documentId: String? = null

    var reviewFragmentWillBeShown: Boolean = false

    private val binding = GpsViewPaymentComponentBinding.inflate(getLayoutInflaterWithGiniPaymentThemeAndLocale(GiniInternalPaymentModule.getSDKLanguage(context)?.languageLocale()), this)
    private lateinit var selectBankButton: Button
    private lateinit var payInvoiceButton: Button
    private lateinit var paymentProviderAppIconHolder: GpsPaymentProviderIconHolderBinding

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LOG.debug("onAttachedToWindow")
        if (coroutineScope == null) {
            LOG.debug("Creating coroutine scope")
            coroutineScope = CoroutineScope(coroutineContext)
        }
        if (paymentComponent?.bankPickerRows == BankPickerRows.TWO) {
            binding.gpsTwoRowsBankSelection.isVisible = true
            binding.gpsSingleRowBankSelection.root.visibility = View.GONE
        } else {
            binding.gpsSingleRowBankSelection.root.visibility = View.VISIBLE
            binding.gpsTwoRowsBankSelection.visibility = View.GONE
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
                        binding.gpsMoreInformation.visibility = if (isReturning) View.GONE else View.VISIBLE
                        binding.gpsSelectBankLabel.visibility = if (isReturning) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun checkPaymentComponentHeight() {
        if (resources.getDimension(R.dimen.gps_payment_component_height) >= resources.getDimension(R.dimen.gps_accessibility_min_height)) {
            binding.gpsSelectBankPickerLayout.layoutParams.height = resources.getDimension(R.dimen.gps_payment_component_height).toInt()
        }
    }

    private fun restoreBankPickerDefaultState() {
        LOG.debug("Restoring bank picker default state")
        context?.wrappedWithGiniPaymentTheme()?.let { context ->
            payInvoiceButton.visibility = View.GONE
            paymentProviderAppIconHolder.root.visibility = View.GONE
            selectBankButton.text = context.getString(R.string.gps_select_bank)
            selectBankButton.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(context, R.drawable.gps_chevron_down_icon),
                null
            )
        }
    }

    private fun customizeBankPicker(paymentProviderApp: PaymentProviderApp) {
        LOG.debug("Customizing bank picker for payment provider app: {}", paymentProviderApp.name)
        context?.wrappedWithGiniPaymentTheme()?.let { context ->
            selectBankButton.apply {
                text = if (paymentComponent?.bankPickerRows == BankPickerRows.SINGLE) "" else paymentProviderApp.name
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(context, R.drawable.gps_chevron_down_icon),
                    null
                )
            }

            paymentProviderAppIconHolder.apply {
                gpsPaymentProviderIcon.setImageDrawable(paymentProviderApp.icon)
                root.visibility = View.VISIBLE
                root.contentDescription = paymentProviderApp.name
            }
        }
    }

    private fun customizePayInvoiceButton(paymentProviderApp: PaymentProviderApp) {
        LOG.debug("Customizing pay invoice button for payment provider app: {}", paymentProviderApp.name)
        payInvoiceButton.setBackgroundTint(paymentProviderApp.colors.backgroundColor, 255)
        payInvoiceButton.setTextColor(paymentProviderApp.colors.textColor)
        payInvoiceButton.visibility = View.VISIBLE
    }

    private fun enableBankPicker() {
        LOG.debug("Enabling bank picker")
        selectBankButton.isEnabled = true
    }

    private fun disableBankPicker() {
        LOG.debug("Disabling bank picker")
        selectBankButton.isEnabled = false
    }

    private fun enablePayInvoiceButton() {
        LOG.debug("Enabling pay invoice button")
        payInvoiceButton.isEnabled = true
        payInvoiceButton.alpha = 1f
    }

    private fun disablePayInvoiceButton() {
        LOG.debug("Disabling pay invoice button")
        payInvoiceButton.isEnabled = false
        payInvoiceButton.alpha = 0.4f
    }

    private fun restorePayInvoiceButtonDefaultState() {
        LOG.debug("Restoring pay invoice button default state")
        context?.wrappedWithGiniPaymentTheme()?.let { context ->
            payInvoiceButton.apply {
                setBackgroundTint(
                    ContextCompat.getColor(
                        context,
                        R.color.gps_unelevated_button_background
                    )
                )
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.gps_unelevated_button_text
                    )
                )
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LOG.debug("onDetachedFromWindow")
        coroutineScope?.cancel()
        coroutineScope = null
    }

    private fun initViews() {
        selectBankButton = if (paymentComponent?.bankPickerRows == BankPickerRows.TWO) binding.gpsSelectBankPicker.gpsSelectBankButton else binding.gpsSingleRowBankSelection.gpsSelectBankButton
        payInvoiceButton = if (paymentComponent?.bankPickerRows == BankPickerRows.TWO) binding.gpsPayInvoiceButtonTwoRows else binding.gpsSingleRowBankSelection.gpsPayInvoiceButton
        paymentProviderAppIconHolder = if (paymentComponent?.bankPickerRows == BankPickerRows.TWO) binding.gpsSelectBankPicker.gpsPaymentProviderAppIconHolder else binding.gpsSingleRowBankSelection.gpsPaymentProviderAppIconHolder

        payInvoiceButton.text = if (reviewFragmentWillBeShown) resources.getString(R.string.gps_continue_to_overview) else resources.getString(R.string.gps_pay_button)
    }

    fun getMoreInformationLabel() = binding.gpsMoreInformation

    fun getPayInvoiceButton() = payInvoiceButton

    fun getBankPickerButton() = selectBankButton

    private companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponentView::class.java)
    }
}