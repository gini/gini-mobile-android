package net.gini.android.bank.sdk.capture.digitalinvoice.view

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.view.isVisible
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
     * Set the click listener for the proceed button.
     *
     * @param listener the click listener for the button
     */
    fun setOnProceedClickListener(listener: OnClickListener)

    /**
     * Enable or disable the proceed button.
     *
     * @param enabled for enabling or disabling the button
     */
    fun setProceedButtonEnabled(enabled: Boolean)

    /**
     * Set the total price.
     *
     * @param priceWithCurrencySymbol price string with currency symbol
     */
    fun setTotalPrice(priceWithCurrencySymbol: String)

    /**
     * Called when Skonto percentage badge text updated.
     *
     * @param text formatted text.
     * Example value: "3% Skonto discount"
     */
    fun onSkontoPercentageBadgeUpdated(text: String) {

    }

    /**
     * Called when visibility of Skonto badge should be changed.
     *
     * @param isVisible visibility flag
     */
    fun onSkontoPercentageBadgeVisibilityUpdate(isVisible: Boolean) {

    }

    /**
     * Called when Skonto savings amount text updated.
     *
     * @param text formatted text.
     * Example value: `"Save 100.00 EUR"`
     */
    fun onSkontoSavingsAmountUpdated(text: String) {

    }

    /**
     * Called when Skonto savings amount visibility updated.
     *
     * @param isVisible visibility flag
     */
    fun onSkontoSavingsAmountVisibilityUpdated(isVisible: Boolean) {

    }

}

/**
 * Internal use only.
 *
 * @suppress
 */
class DefaultDigitalInvoiceNavigationBarBottomAdapter : DigitalInvoiceNavigationBarBottomAdapter {

    private var binding: GbsDigitalInvoiceNavigationBarBottomBinding? = null

    override fun setOnHelpClickListener(listener: OnClickListener) {
        binding?.gbsHelpBtn?.setOnClickListener(listener)
    }

    override fun setOnProceedClickListener(listener: OnClickListener) {
        binding?.gbsPay?.setOnClickListener(listener)
    }

    override fun setProceedButtonEnabled(enabled: Boolean) {
        binding?.gbsPay?.isEnabled = enabled
    }

    override fun setTotalPrice(priceWithCurrencySymbol: String) {
        binding?.grossPriceTotal?.text = priceWithCurrencySymbol
    }

    override fun onCreateView(container: ViewGroup): View {
        binding = GbsDigitalInvoiceNavigationBarBottomBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
        return binding!!.root
    }

    override fun onDestroy() {
        binding = null
    }

    override fun onSkontoPercentageBadgeUpdated(text: String) {
        super.onSkontoPercentageBadgeUpdated(text)
        binding?.skontoDiscountLabel?.text = text
    }

    override fun onSkontoPercentageBadgeVisibilityUpdate(isVisible: Boolean) {
        binding?.skontoDiscountLabel?.isVisible = isVisible
    }

    override fun onSkontoSavingsAmountUpdated(text: String) {
        binding?.skontoSavedAmount?.text = text
    }

    override fun onSkontoSavingsAmountVisibilityUpdated(isVisible: Boolean) {
        binding?.skontoSavedAmount?.isVisible = isVisible
    }
}