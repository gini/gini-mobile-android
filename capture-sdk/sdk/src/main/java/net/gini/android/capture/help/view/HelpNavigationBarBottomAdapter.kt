package net.gini.android.capture.help.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.capture.databinding.GcHelpNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Implement this interface if you would like custom action triggered when tapping back navigation
 * and pass it to the {@link GiniCapture.Builder#setHelpNavigationBarBottomAdapter(HelpNavigationBarBottomAdapter)}
 */
interface HelpNavigationBarBottomAdapter : InjectedViewAdapter {

    fun setOnBackClickListener(listener: View.OnClickListener?)
}

/**
 * Internal use only.
 */
internal class DefaultHelpNavigationBarBottomAdapter: HelpNavigationBarBottomAdapter {

    var binding: GcHelpNavigationBarBottomBinding? = null

    override fun setOnBackClickListener(listener: View.OnClickListener?) {
       binding?.gcGoBack?.setOnClickListener(listener)
    }

    override fun onCreateView(container: ViewGroup): View {

        val viewBinding = GcHelpNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)
        binding = viewBinding

        return viewBinding.root
    }

    override fun onDestroy() {
        binding = null
    }
}
