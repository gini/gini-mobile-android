package net.gini.android.bank.sdk.exampleapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import net.gini.android.bank.sdk.capture.skonto.SkontoNavigationBarBottomAdapter
import net.gini.android.bank.sdk.exampleapp.databinding.CustomSkontoNavigationBarBinding

class CustomSkontoNavigationBarBottomAdapter : SkontoNavigationBarBottomAdapter {

    private var binding: CustomSkontoNavigationBarBinding? = null

    override fun setOnBackClickListener(onClick: () -> Unit) {
        binding?.gbsBackBtn?.setOnClickListener { onClick() }
    }

    override fun setOnProceedClickListener(onClick: () -> Unit) {
        binding?.gbsPay?.setOnClickListener { onClick() }
    }


    override fun onTotalAmountUpdated(amount: String) {
        binding?.priceTotal?.text = amount
    }

    override fun setOnHelpClickListener(onClick: () -> Unit) {
        // Unused now
    }

    override fun onSkontoPercentageBadgeUpdated(text: String) {
        binding?.discountInfo?.text = text
    }

    override fun onSkontoPercentageBadgeVisibilityUpdate(isVisible: Boolean) {
        binding?.discountInfo?.isVisible = isVisible
    }

    override fun onSkontoSavingsAmountUpdated(text: String) {
        binding?.skontoSavingsAmount?.text = text
    }

    override fun onSkontoSavingsAmountVisibilityUpdated(isVisible: Boolean) {
        binding?.skontoSavingsAmount?.isVisible = isVisible
    }

    override fun onCreateView(container: ViewGroup): View {
        binding = CustomSkontoNavigationBarBinding.inflate(
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