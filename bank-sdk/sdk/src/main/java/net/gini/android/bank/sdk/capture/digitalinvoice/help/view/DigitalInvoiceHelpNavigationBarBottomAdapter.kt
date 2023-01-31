package net.gini.android.bank.sdk.capture.digitalinvoice.help.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.databinding.GbsHelpNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

interface DigitalInvoiceHelpNavigationBarBottomAdapter: InjectedViewAdapter {

    fun setOnBackButtonClickListener(click: View.OnClickListener)

}

class DefaultDigitalInvoiceHelpNavigationBarBottomAdapter: DigitalInvoiceHelpNavigationBarBottomAdapter {
    var viewBinding: GbsHelpNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(click: View.OnClickListener) {
        viewBinding?.gbsGoBack?.setOnClickListener(click)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = GbsHelpNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }
}
