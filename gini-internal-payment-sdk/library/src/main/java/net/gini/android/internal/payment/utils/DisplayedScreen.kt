package net.gini.android.internal.payment.utils

/**
 * Represents the currently visible screen presented in [PaymentFragment]
 */
sealed class DisplayedScreen {
    /**
     * Default state - nothing visible
     */
    object Nothing: DisplayedScreen()

    /**
     * Bottom sheet for selecting a bank to pay with.
     */
    object BankSelectionBottomSheet: DisplayedScreen()

    /**
     * More information screen.
     */
    object MoreInformationFragment: DisplayedScreen()

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
    object ReviewScreen: DisplayedScreen()
}