package net.gini.android.bank.sdk.capture.skonto

/**
 * Compose test tags for the Skonto screen, consumed by the example app's UI tests.
 *
 * These exist because the Skonto screen is Compose and the suite has nothing else stable
 * to address: the discount toggle is a bare [net.gini.android.capture.ui.components.switcher.GiniSwitch]
 * with no text of its own, so without a tag the only selector left is
 * `UiSelector().checkable(true)`, which silently starts matching a different control the
 * moment a second switch is added to the screen.
 *
 * A tag is not a `contentDescription`: TalkBack does not read it, so adding one here
 * changes nothing about what a real user hears. Never repurpose a `contentDescription`
 * as a test hook for the opposite reason — that text *is* spoken.
 *
 * For UiAutomator to see these as resource ids, the screen's `ComposeView` root opts in
 * with `Modifier.semantics { testTagsAsResourceId = true }` — see
 * [net.gini.android.bank.sdk.capture.skonto.SkontoFragment.onCreateView]. The property is
 * read from the nearest ancestor semantics node, so the opt-in has to be repeated for any
 * other Compose root (the digital-invoice Skonto screen has its own, and does not inherit
 * this one).
 *
 * This object is `internal`, so `bank-sdk:example-app`'s androidTest cannot import it. The
 * test side keeps its own copy of these literals in
 * `bank-sdk/example-app/src/androidTest/java/net/gini/android/bank/sdk/exampleapp/ui/screens/SkontoScreen.kt`
 * — the two must be edited together, and are greppable by the literal string.
 */
internal object SkontoTestTags {

    /** The "Mit Skonto" / "With Skonto discount" toggle in the discount section. */
    const val DISCOUNT_SWITCH = "skontoDiscountSwitch"

    /** The discount section's title text. */
    const val DISCOUNT_TITLE = "skontoDiscountTitle"

    /** The "Betrag nach Abzug" / final-amount input. */
    const val FINAL_AMOUNT_FIELD = "skontoFinalAmountField"

    /** The "Ablaufdatum Skonto" / expiry-date input. */
    const val EXPIRY_DATE_FIELD = "skontoExpiryDateField"

    /**
     * The footer's total amount. Applied in both footer variants — the footer renders
     * either inside the scrolling column (landscape) or in the `Scaffold`'s `bottomBar`,
     * and each path splits again into a landscape and a portrait composable.
     */
    const val FOOTER_TOTAL = "skontoFooterTotal"

    /** The footer's "Confirm and proceed" button, in both footer variants. */
    const val PROCEED_BUTTON = "skontoProceedButton"
}
