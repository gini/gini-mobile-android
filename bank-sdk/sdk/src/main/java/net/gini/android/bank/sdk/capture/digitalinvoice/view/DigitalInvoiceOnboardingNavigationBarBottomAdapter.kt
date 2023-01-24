package net.gini.android.bank.sdk.capture.digitalinvoice.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.databinding.GbsDigitalInvoiceNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

interface DigitalInvoiceOnboardingNavigationBarBottomAdapter: InjectedViewAdapter {

    fun setGetStartedButtonClickListener(click: View.OnClickListener)
}

class DefaultDigitalInvoiceOnboardingBarBottomAdapter: DigitalInvoiceOnboardingNavigationBarBottomAdapter {

    var viewBinding: GbsDigitalInvoiceNavigationBarBottomBinding? = null


    override fun setGetStartedButtonClickListener(click: View.OnClickListener) {
        viewBinding?.gbsBarBottomNextButton?.setOnClickListener(click)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = GbsDigitalInvoiceNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)

        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}