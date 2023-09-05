package net.gini.android.bank.screen.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.screen.databinding.CustomHelpNavigationBarBottomBinding
import net.gini.android.capture.help.view.HelpNavigationBarBottomAdapter

class CustomHelpNavigationBarBottomAdapter : HelpNavigationBarBottomAdapter {

    var binding: CustomHelpNavigationBarBottomBinding? = null

    override fun setOnBackClickListener(listener: View.OnClickListener?) {
        binding?.imageButtonGoBack?.setOnClickListener(listener)
    }

    override fun onCreateView(container: ViewGroup): View {

        val viewBinding = CustomHelpNavigationBarBottomBinding.inflate(
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
