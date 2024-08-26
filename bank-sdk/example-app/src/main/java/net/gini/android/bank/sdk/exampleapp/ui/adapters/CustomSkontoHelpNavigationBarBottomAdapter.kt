package net.gini.android.bank.sdk.exampleapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.capture.skonto.help.SkontoHelpNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.databinding.CustomSkontoHelpNavigationBarBottomBinding

class CustomSkontoHelpNavigationBarBottomAdapter:
    SkontoHelpNavigationBarBottomAdapter {
    var viewBinding: CustomSkontoHelpNavigationBarBottomBinding? = null

    override fun setOnBackClickListener(onClick: () -> Unit) {
        viewBinding?.gbsGoBack?.setOnClickListener { onClick() }
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = CustomSkontoHelpNavigationBarBottomBinding
            .inflate(LayoutInflater.from(container.context), container, false)
        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }
}
