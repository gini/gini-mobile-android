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
                LOG.warn("Cannot show payment provider apps: PaymentComponentsController must be set before showing the PaymentComponentView")
                return@launch
            }
            LOG.debug("Collecting payment provider apps state from PaymentComponentsController")
            paymentComponent?.paymentProviderAppsFlow?.collect { paymentProviderAppsState ->
                LOG.debug("Received payment provider apps state: {}", paymentProviderAppsState)
                when (paymentProviderAppsState) {
                    is PaymentProviderAppsState.Error,
                    is PaymentProviderAppsState.Loading -> {
                        disablePayInvoiceButton()
                        disableBankPicker()
                    }
                    is PaymentProviderAppsState.Success -> {
                        if (paymentProviderAppsState.paymentProviderApps.isNotEmpty()) {
                            LOG.debug("Received {} payment provider apps", paymentProviderAppsState.paymentProviderApps.size)
                            val firstInstalledPaymentProviderApp = paymentProviderAppsState.paymentProviderApps.find { it.installedPaymentProviderApp != null }
                            if (firstInstalledPaymentProviderApp != null) {
                                LOG.debug("First payment provider app is installed: {}", firstInstalledPaymentProviderApp.name)
                                customizeBankPicker(firstInstalledPaymentProviderApp)
                                enablePayInvoiceButton()
                                customizePayInvoiceButton(firstInstalledPaymentProviderApp)
                            } else {
                                LOG.debug("No installed payment provider app found")
                                context?.wrappedWithGiniHealthTheme()?.let { context ->
                                    disablePayInvoiceButton()
                                }
                            }
                        } else {
                            LOG.debug("No payment provider apps received")
                            disablePayInvoiceButton()
                        }
                        enableBankPicker()
                    }
                }
            }
        }
    }

    private fun customizeBankPicker(firstPaymentProviderApp: PaymentProviderApp) {
        binding.ghsSelectBankPicker.text = firstPaymentProviderApp.name
        binding.ghsSelectBankPicker.setCompoundDrawablesWithIntrinsicBounds(
            firstPaymentProviderApp.icon,
            null,
            ContextCompat.getDrawable(context, R.drawable.ghs_chevron_down_icon),
            null
        )
    }

    private fun customizePayInvoiceButton(firstPaymentProviderApp: PaymentProviderApp) {
        binding.ghsPayInvoiceButton.setBackgroundTint(firstPaymentProviderApp.colors.backgroundColor)
        binding.ghsPayInvoiceButton.setTextColor(firstPaymentProviderApp.colors.textColor)
    }

    private fun enableBankPicker() {
        binding.ghsSelectBankPicker.isEnabled = true
    }

    private fun disableBankPicker() {
        binding.ghsSelectBankPicker.isEnabled = false
    }

    private fun enablePayInvoiceButton() {
        binding.ghsPayInvoiceButton.isEnabled = true
        binding.ghsPayInvoiceButton.alpha = 1f
    }

    private fun disablePayInvoiceButton() {
        binding.ghsPayInvoiceButton.setBackgroundTint(ContextCompat.getColor(context, R.color.ghs_unelevated_button_background))
        binding.ghsPayInvoiceButton.setTextColor(ContextCompat.getColor(context, R.color.ghs_unelevated_button_text))
        binding.ghsPayInvoiceButton.isEnabled = false
        binding.ghsPayInvoiceButton.alpha = 0.4f
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
        LOG.debug("More information clicked")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PaymentComponentView::class.java)
    }
}