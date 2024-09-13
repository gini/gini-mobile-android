package net.gini.android.merchant.sdk.util

/**
 * Represents the currently visible screen presented in [PaymentFragment]
 */
sealed class DisplayedScreen {
    /**
     * Default state - nothing visible
     */
    object Nothing: DisplayedScreen()

    /**
     * Entrypoint to the payment flow - shows which bank is selected
     * (or prompts the user to choose a payment provider if there is no previous selection) for making the payment.
     */
    object PaymentComponentBottomSheet : DisplayedScreen()

    /**
     * Bottom sheet for selecting a bank to pay with.
     */
    object BankSelectionBottomSheet : DisplayedScreen()

    /**
     * More information screen.
     */
    object MoreInformationFragment : DisplayedScreen()

    /**
     * Prompt for the user to install the selected banking app.
     */
    object InstallAppBottomSheet: DisplayedScreen()

    /**
     * Prompt for the user to select another application to share the payment request through.
     */
    object OpenWithBottomSheet: DisplayedScreen()

    /**
     * OS native share sheet.
     */
    object ShareSheet: DisplayedScreen()

    /**
     * Payment details review screen.
     */
    object ReviewBottomSheet: DisplayedScreen()

    companion object {
        fun toDisplayedScreen(screen: net.gini.android.internal.payment.util.DisplayedScreen): DisplayedScreen = when (screen) {
            net.gini.android.internal.payment.util.DisplayedScreen.Nothing -> Nothing
            net.gini.android.internal.payment.util.DisplayedScreen.BankSelectionBottomSheet -> BankSelectionBottomSheet
            net.gini.android.internal.payment.util.DisplayedScreen.InstallAppBottomSheet -> InstallAppBottomSheet
            net.gini.android.internal.payment.util.DisplayedScreen.MoreInformationFragment -> MoreInformationFragment
            net.gini.android.internal.payment.util.DisplayedScreen.OpenWithBottomSheet -> OpenWithBottomSheet
            net.gini.android.internal.payment.util.DisplayedScreen.ReviewScreen -> ReviewBottomSheet
            net.gini.android.internal.payment.util.DisplayedScreen.ShareSheet -> ShareSheet
        }
    }
}