package net.gini.android.bank.sdk.exampleapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.databinding.CustomDigitalInvoiceNavigationBarBinding

class CustomDigitalInvoiceNavigationBarBottomAdapter: DigitalInvoiceNavigationBarBottomAdapter {

    private var binding: CustomDigitalInvoiceNavigationBarBinding? = null

    override fun setOnHelpClickListener(listener: View.OnClickListener) {
        binding?.gbsHelpBtn?.setOnClickListener(listener)
    }

    override fun setOnProceedClickListener(listener: View.OnClickListener) {
        binding?.gbsPay?.setOnClickListener(listener)
    }

    override fun setProceedButtonEnabled(enabled: Boolean) {
        binding?.gbsPay?.isEnabled = enabled
    }

    override fun setTotalPrice(priceWithCurrencySymbol: String) {
        binding?.grossPriceTotal?.text = priceWithCurrencySymbol
    }

    override fun onCreateView(container: ViewGroup): View {
        binding = CustomDigitalInvoiceNavigationBarBinding.inflate(LayoutInflater.from(container.context), container, false)
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