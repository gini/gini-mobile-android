package net.gini.android.bank.sdk.capture.digitalinvoice.view

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import net.gini.android.bank.sdk.databinding.GbsDigitalInvoiceNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Adapter for injecting a custom bottom navigation bar on the DigitalInvoiceFragment screen.
 */
interface DigitalInvoiceNavigationBarBottomAdapter : InjectedViewAdapter {

    /**
     * Set the click listener for the help button.
     *
     * @param listener the click listener for the button
     */
    fun setOnHelpClickListener(listener: OnClickListener)

    /**
     * Set the click listener for the pay button.
     *
     * @param listener the click listener for the button
     */
    fun setOnPayClickListener(listener: OnClickListener)

    /**
     * Enable or disable pay button.
     *
     * @param enabled for enabling or disabling the button
     */
    fun setPayButtonEnabled(enabled: Boolean)

    /**
     * Set the gross price total.
     *
     * @param integralPartWithCurrencySymbol integral with currency symbol
     * @param fractionalPart fractional part
     */
    fun setGrossPriceTotal(integralPartWithCurrencySymbol: String, fractionalPart: String)

}

/**
 * Internal use only.
 *
 * @suppress
 */
class DefaultDigitalInvoiceNavigationBarBottomAdapter: DigitalInvoiceNavigationBarBottomAdapter {

    private var binding: GbsDigitalInvoiceNavigationBarBottomBinding? = null

    override fun setOnHelpClickListener(listener: OnClickListener) {
        binding?.gbsHelpBtn?.setOnClickListener(listener)
    }

    override fun setOnPayClickListener(listener: OnClickListener) {
        binding?.gbsPay?.setOnClickListener(listener)
    }

    override fun setPayButtonEnabled(enabled: Boolean) {
        binding?.gbsPay?.isEnabled = enabled
    }

    override fun setGrossPriceTotal(integralPartWithCurrencySymbol: String, fractionalPart: String) {
        binding?.grossPriceTotalIntegralPart?.text = integralPartWithCurrencySymbol
        binding?.grossPriceTotalFractionalPart?.text = fractionalPart
    }

    override fun onCreateView(container: ViewGroup): View {
        binding = GbsDigitalInvoiceNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)
        return binding!!.root
    }

    override fun onDestroy() {
        binding = null
    }

}