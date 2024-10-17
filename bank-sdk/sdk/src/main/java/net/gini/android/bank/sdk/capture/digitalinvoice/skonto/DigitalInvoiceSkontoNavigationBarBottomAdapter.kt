package net.gini.android.bank.sdk.capture.digitalinvoice.skonto

import net.gini.android.capture.view.InjectedViewAdapter

interface DigitalInvoiceSkontoNavigationBarBottomAdapter : InjectedViewAdapter {

    /**
     * Set the click listener for the back button.
     *
     * @param onClick the click function for the back button
     */
    fun setOnBackClickListener(onClick: () -> Unit)

    /**
     * Set the click listener for the help button.
     *
     * @param onClick the click function for the help button
     */
    fun setOnHelpClickListener(onClick: () -> Unit)
}
