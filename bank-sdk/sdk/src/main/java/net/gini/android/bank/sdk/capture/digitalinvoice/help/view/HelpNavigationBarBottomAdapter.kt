package net.gini.android.bank.sdk.capture.digitalinvoice.help.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.databinding.GbsHelpNavigationBarBottomBinding
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Implement this interface to add back navigation to a custom view on bottom navigation bar
 * and set it as the {@link GiniBank helpNavigationBarBottomAdapter}.
 */
interface HelpNavigationBarBottomAdapter: InjectedViewAdapter {

    /**
     * Sets a click listener on back button
     *
     * @param listener the click listener for the button
     */
    fun setOnBackButtonClickListener(click: View.OnClickListener)

}

/**
 * Internal use only.
 *
 */
class DefaultHelpNavigationBarBottomAdapter: HelpNavigationBarBottomAdapter {
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
