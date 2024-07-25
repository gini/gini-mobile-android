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

    override fun setProceedButtonEnabled(enabled: Boolean) {
        binding?.gbsPay?.isEnabled = enabled
    }

    override fun setTotalPriceText(text: String) {
        binding?.priceTotal?.text = text
    }

    override fun setDiscountLabelText(text: String) {
        binding?.discountInfo?.text = text
    }

    override fun onCreateView(container: ViewGroup): View {
        binding = CustomSkontoNavigationBarBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
        return binding!!.root
    }

    override fun setDiscountLabelVisible(visible: Boolean) {
        binding?.discountInfo?.isVisible = visible
    }

    override fun onDestroy() {
        binding = null
    }
}