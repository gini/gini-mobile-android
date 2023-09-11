package net.gini.android.bank.exampleapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.capture.digitalinvoice.view.DigitalInvoiceOnboardingNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.databinding.CustomDigitalInvoiceOnboardingBottomNavigationBarBinding

class CustomDigitalInvoiceOnboardingNavigationBarBottomAdapter:
    DigitalInvoiceOnboardingNavigationBarBottomAdapter {

    var viewBinding: CustomDigitalInvoiceOnboardingBottomNavigationBarBinding? = null


    override fun setGetStartedButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gbsBarBottomNextButton?.setOnClickListener(listener)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = CustomDigitalInvoiceOnboardingBottomNavigationBarBinding.inflate(LayoutInflater.from(container.context), container, false)

        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}