package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity

/**
 * Sets the Transaction Docs flag through MainActivity's own [ConfigurationViewModel], the same hook
 * [PaymentHintConfigurator] and [CreditNoteHintConfigurator] use. `ConfigurationViewModel` forwards
 * it to the SDK as `transactionDocsEnabled`.
 *
 * ## Why this replaced a UI toggle
 *
 * `WarningBottomSheetTestBase.setup` used to turn the flag off by driving the Settings screen —
 * open Settings, tap `switch_transactionDocsFeature`, press back. That runs before EVERY test in
 * every warning-sheet suite, and it broke `CreditNoteWarningTextScalingTests` on BrowserStack: at a
 * 2.0 font scale the taller content moved the switch under the navigation bar, the tap missed, the
 * app lost the foreground, and the `pressBack()` that followed threw
 * `NoActivityResumedException` — in `@Before`, so the test never started.
 *
 * That is the same trap documented on `ConfigurationScreen.clickCreditNoteHintSwitch`. Setting the
 * flag directly removes three UI interactions from the setup of every test in these suites, so none
 * of them can fail for a layout reason before reaching their own assertions.
 */
object TransactionDocsConfigurator {

    fun applyTransactionDocsConfiguration(
        scenario: ActivityScenario<MainActivity>,
        transactionDocsEnabled: Boolean
    ) {
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[ConfigurationViewModel::class.java]
            viewModel.setConfiguration(
                viewModel.configurationFlow.value.copy(
                    isTransactionDocsEnabled = transactionDocsEnabled
                )
            )
        }
    }
}
