package net.gini.android.bank.sdk.exampleapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.DigitalInvoiceSkontoNavigationBarBottomAdapter
import net.gini.android.bank.sdk.capture.skonto.SkontoNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.databinding.CustomDigitalInvoiceSkontoNavigationBarBinding
import net.gini.android.bank.sdk.exampleapp.databinding.CustomSkontoNavigationBarBinding

class CustomDigitalInvoiceSkontoNavigationBarBottomAdapter : DigitalInvoiceSkontoNavigationBarBottomAdapter {

    private var binding: CustomDigitalInvoiceSkontoNavigationBarBinding? = null

    override fun setOnBackClickListener(onClick: () -> Unit) {
        binding?.gbsBackBtn?.setOnClickListener { onClick() }
    }


    override fun setOnHelpClickListener(onClick: () -> Unit) {
        // TODO help here
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