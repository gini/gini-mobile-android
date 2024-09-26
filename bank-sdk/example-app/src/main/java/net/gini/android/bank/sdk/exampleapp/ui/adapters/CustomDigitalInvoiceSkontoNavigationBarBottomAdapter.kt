package net.gini.android.bank.sdk.exampleapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.DigitalInvoiceSkontoNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.databinding.CustomDigitalInvoiceSkontoNavigationBarBinding

class CustomDigitalInvoiceSkontoNavigationBarBottomAdapter : DigitalInvoiceSkontoNavigationBarBottomAdapter {

    private var binding: CustomDigitalInvoiceSkontoNavigationBarBinding? = null

    override fun setOnBackClickListener(onClick: () -> Unit) {
        binding?.gbsBackBtn?.setOnClickListener { onClick() }
    }


    override fun setOnHelpClickListener(onClick: () -> Unit) {
        binding?.gbsHelpBtn?.setOnClickListener { onClick() }
    }

    override fun onCreateView(container: ViewGroup): View {
        binding = CustomDigitalInvoiceSkontoNavigationBarBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
        return binding!!.root
    }

    override fun onDestroy() {
        binding = null
    }
}
