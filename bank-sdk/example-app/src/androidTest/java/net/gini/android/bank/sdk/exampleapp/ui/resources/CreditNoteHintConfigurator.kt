package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity

/**
 * Sets the Credit Note Hint flag through MainActivity's own [ConfigurationViewModel] — the
 * same hook [PaymentHintConfigurator] uses for the payment hints, and the very instance
 * `MainActivity.configureGiniBank()` reads. The configuration is applied to the SDK when the
 * capture flow starts, so this must be called before clicking the photo payment button.
 *
 * Note: this is the CLIENT-side flag. The server-side /configurations flag
 * `creditNoteHintEnabled` must be enabled for the `gini-mobile-test` client — the warning
 * needs both gates.
 *
 * ## Why not the Settings switch
 *
 * Driving the flag through `switch_creditNoteHint` is what the Xray steps describe, but the
 * tap is not portable across devices. The switch is the last view of the first toggle
 * section (`layout_feature_toggles.xml`), so Espresso's `scrollTo()` — which scrolls the
 * minimum amount — parks it flush against the bottom edge of the ScrollView viewport. The
 * example app does no window-inset handling and runs with targetSdk 36, where edge-to-edge is
 * mandatory, so on some screen sizes that spot sits under the navigation bar: the tap never
 * reaches the switch, the app loses the foreground, and the next Espresso action fails with
 * `NoActivityResumedException`. Observed on one of the two Pixels in a single BrowserStack
 * build — the same tests passed on the other.
 *
 * Reading the switch is safe (a `check()` never taps), so the switch is still covered by
 * `CreditNoteWarningTests.test1_creditNoteHintFlagIsEnabledByDefault`: a renamed or moved
 * switch still fails loudly there. Only the tap moved off the UI.
 */
object CreditNoteHintConfigurator {

    fun applyCreditNoteHintConfiguration(
        scenario: ActivityScenario<MainActivity>,
        creditNoteHintEnabled: Boolean
    ) {
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[ConfigurationViewModel::class.java]
            viewModel.setConfiguration(
                viewModel.configurationFlow.value.copy(
                    isCreditNoteHintEnabled = creditNoteHintEnabled
                )
            )
        }
    }
}
