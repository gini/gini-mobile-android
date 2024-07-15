package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.capture.view.InjectedViewAdapter

interface SkontoNavigationBarBottomAdapter : InjectedViewAdapter {

    /**
     * Set the click listener for the help button.
     *
     * @param onClick the click function for the help button
     */
    fun setOnHelpClickListener(onClick: () -> Unit)

    /**
     * Set the click listener for the back button.
     *
     * @param onClick the click function for the back button
     */
    fun setOnBackClickListener(onClick: () -> Unit)

    /**
     * Set the click listener for the proceed button.
     *
     * @param listener the click listener for the button
     */
    fun setOnProceedClickListener(onClick: () -> Unit)

    /**
     * Enable or disable the proceed button.
     *
     * @param enabled for enabling or disabling the button
     */
    fun setProceedButtonEnabled(enabled: Boolean)

    /**
     * Set the total price.
     *
     * @param text price string with currency symbol
     */
    fun setTotalPriceText(text: String)

    fun setDiscountLabelVisible(visible: Boolean)

    fun setDiscountLabelText(text: String)
}