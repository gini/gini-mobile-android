package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.capture.view.InjectedViewAdapter

interface SkontoNavigationBarBottomAdapter : InjectedViewAdapter {

    /**
     * Set the click listener for the proceed button.
     *
     * @param listener the click listener for the button
     */
    fun setOnProceedClickListener(onClick: () -> Unit)

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

    /**
     * Called when the total amount with currency code updated
     *
     * @param amount price string with currency symbol.
     * Example value: `"100.00 EUR"`
     */
    fun onTotalAmountUpdated(amount: String)

    /**
     * Called when Skonto percentage badge text updated.
     *
     * @param text formatted text.
     * Example value: "3% Skonto discount"
     */
    fun onSkontoPercentageBadgeUpdated(text: String)

    /**
     * Called when visibility of Skonto badge should be changed.
     *
     * @param isVisible visibility flag
     */
    fun onSkontoPercentageBadgeVisibilityUpdate(isVisible: Boolean)

    /**
     * Called when Skonto savings amount text updated.
     *
     * @param text formatted text.
     * Example value: `"Save 100.00 EUR"`
     */
    fun onSkontoSavingsAmountUpdated(text: String)

    /**
     * Called when Skonto savings amount visibility updated.
     *
     * @param isVisible visibility flag
     */
    fun onSkontoSavingsAmountVisibilityUpdated(isVisible: Boolean)

}