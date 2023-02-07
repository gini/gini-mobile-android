package net.gini.android.bank.sdk.capture.digitalinvoice.view

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import net.gini.android.bank.sdk.databinding.GbsDigitalInvoiceNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

interface DigitalInvoiceNavigationBarBottomAdapter : InjectedViewAdapter {

    fun setOnBackClickListener(listener: OnClickListener)

    fun setOnHelpClickListener(listener: OnClickListener)

    fun setOnPayClickListener(listener: OnClickListener)

    fun setHelpIconResource(@DrawableRes drawable: Int)

    fun setPayButtonEnabled(enabled: Boolean)

    fun setGrossPriceTotal(integral: String, fractional: String)

}

class DefaultDigitalInvoiceNavigationBarBottomAdapter: DigitalInvoiceNavigationBarBottomAdapter {

    private var binding: GbsDigitalInvoiceNavigationBarBottomBinding? = null

    override fun setOnBackClickListener(listener: OnClickListener) {
        binding?.gbsBackBtn?.setOnClickListener(listener)
    }

    override fun setOnHelpClickListener(listener: OnClickListener) {
        binding?.gbsHelpBtn?.setOnClickListener(listener)
    }

    override fun setOnPayClickListener(listener: OnClickListener) {
        binding?.gbsPay?.setOnClickListener(listener)
    }

    override fun setHelpIconResource(drawable: Int) {
        binding?.gbsHelpBtn?.setImageResource(drawable)
    }

    override fun setPayButtonEnabled(enabled: Boolean) {
        binding?.gbsPay?.isEnabled = enabled
    }

    override fun setGrossPriceTotal(integral: String, fractional: String) {
        binding?.grossPriceTotalIntegralPart?.text = integral
        binding?.grossPriceTotalFractionalPart?.text = fractional
    }

    override fun onCreateView(container: ViewGroup): View {
        binding = GbsDigitalInvoiceNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)
        return binding!!.root
    }

    override fun onDestroy() {
        binding = null
    }

}