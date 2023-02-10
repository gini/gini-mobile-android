package net.gini.android.bank.sdk.capture.digitalinvoice.help.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.databinding.GbsHelpNavigationBarBottomBinding
import net.gini.android.capture.internal.ui.IntervalClickListener
import net.gini.android.capture.internal.ui.setIntervalClickListener
import net.gini.android.capture.view.InjectedViewAdapter


/**
 * Implement this interface to add back navigation to a custom view on bottom navigation bar
 * and set it as the {@link GiniBank digitalInvoiceHelpNavigationBarBottomAdapter}.
 */
interface DigitalInvoiceHelpNavigationBarBottomAdapter: InjectedViewAdapter {

    /**
     * Sets a click listener on back button
     *
     * @param listener the click listener for the button
     */
    fun setOnBackButtonClickListener(listener: IntervalClickListener?)

}


/**
 * Internal use only.
 *
 */
class DefaultDigitalInvoiceHelpNavigationBarBottomAdapter: DigitalInvoiceHelpNavigationBarBottomAdapter {
    var viewBinding: GbsHelpNavigationBarBottomBinding? = null

    override fun setOnBackButtonClickListener(listener: IntervalClickListener?) {
        viewBinding?.gbsGoBack?.setOnClickListener(listener)
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
