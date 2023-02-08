package net.gini.android.bank.sdk.capture.digitalinvoice.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.gini.android.bank.sdk.databinding.GbsDigitalInvoiceOnboardingNavigationBarBottomBinding
import net.gini.android.capture.internal.ui.setIntervalClickListener
import net.gini.android.capture.view.InjectedViewAdapter

/**
 * Adapter for injecting a custom bottom navigation bar on the onboarding screen.
 */
interface DigitalInvoiceOnboardingNavigationBarBottomAdapter: InjectedViewAdapter {

    /**
     * Set the click listener on get started button
     *
     * @param listener the click listener for the button
     */
    fun setGetStartedButtonClickListener(listener: View.OnClickListener)
}

class DefaultDigitalInvoiceOnboardingNavigationBarBottomAdapter: DigitalInvoiceOnboardingNavigationBarBottomAdapter {

    var viewBinding: GbsDigitalInvoiceOnboardingNavigationBarBottomBinding? = null


    override fun setGetStartedButtonClickListener(listener: View.OnClickListener) {
        viewBinding?.gbsBarBottomNextButton?.setIntervalClickListener(listener)
    }

    override fun onCreateView(container: ViewGroup): View {
        val binding = GbsDigitalInvoiceOnboardingNavigationBarBottomBinding.inflate(LayoutInflater.from(container.context), container, false)

        viewBinding = binding

        return viewBinding!!.root
    }

    override fun onDestroy() {
        viewBinding = null
    }

}