package net.gini.android.bank.sdk.capture.skonto.help

import net.gini.android.capture.view.InjectedViewAdapter

interface SkontoHelpNavigationBarBottomAdapter : InjectedViewAdapter {

    /**
     * Set the click listener for the back button.
     *
     * @param onClick the click function for the back button
     */
    fun setOnBackClickListener(onClick: () -> Unit)

}
