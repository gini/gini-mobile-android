package net.gini.android.bank.sdk.capture.digitalinvoice.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.databinding.GbsDigitalInvoiceNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

interface DigitalInvoiceOnboardingBarBottomAdapter: InjectedViewAdapter {

    fun setOnNextButtonClickListener(click: View.OnClickListener)
}

class DefaultDigitalInvoiceOnboardingBarBottomAdapter: DigitalInvoiceOnboardingBarBottomAdapter {

    var viewBinding: GbsDigitalInvoiceNavigationBarBottomBinding? = null


    override fun setOnNextButtonClickListener(click: View.OnClickListener) {
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