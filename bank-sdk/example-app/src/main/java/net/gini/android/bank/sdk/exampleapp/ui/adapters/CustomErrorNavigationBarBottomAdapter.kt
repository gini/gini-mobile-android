package net.gini.android.bank.sdk.exampleapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.exampleapp.databinding.CustomErrorNavigationBarBottomBinding
import net.gini.android.capture.error.view.ErrorNavigationBarBottomAdapter

class CustomErrorNavigationBarBottomAdapter : ErrorNavigationBarBottomAdapter {

    var binding: CustomErrorNavigationBarBottomBinding? = null

    override fun setOnBackClickListener(listener: View.OnClickListener?) {
        binding?.imageButtonGoBack?.setOnClickListener(listener)
    }

    override fun onCreateView(container: ViewGroup): View {

        val viewBinding = CustomErrorNavigationBarBottomBinding.inflate(
            LayoutInflater.from(container.context),
            container,
            false
        )
        binding = viewBinding

        return viewBinding.root
    }

    override fun onDestroy() {
        binding = null
    }
}
