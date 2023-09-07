package net.gini.android.bank.screen.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.screen.databinding.CustomDigitalInvoiceHelpNavigationBarBottomBinding
import net.gini.android.bank.sdk.capture.digitalinvoice.help.view.DigitalInvoiceHelpNavigationBarBottomAdapter

class CustomDigitalInvoiceHelpNavigationBarBottomAdapter:
    DigitalInvoiceHelpNavigationBarBottomAdapter {
    var viewBinding: CustomDigitalInvoiceHelpNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(listener: View.OnClickListener?) {
        viewBinding?.gbsGoBack?.setOnClickListener(listener)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = CustomDigitalInvoiceHelpNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }
}
