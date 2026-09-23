package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity

/**
 * Sets the Return Assistant and Skonto flags through MainActivity's own
 * [ConfigurationViewModel], the same hook [PaymentHintConfigurator] and
 * [CreditNoteHintConfigurator] use. `ConfigurationViewModel.configureGiniBank` forwards them to
 * the SDK as `returnAssistantEnabled` / `skontoEnabled`, which is what
 * `CaptureFlowFragment.processOnFinishedResultSuccessState` branches on, so this must be called
 * before clicking the photo payment button.
 *
 * Both flags default to `true` in `ExampleAppBankConfiguration`, so a test that relies on them
 * would pass today without setting anything. They are still set explicitly: the two features
 * route the flow to different screens, so a test asserting which screen appears has to state
 * which of them was on, and it must not start passing or failing because a default changed.
 */
object ReturnAssistantSkontoConfigurator {

    fun applyFeatureConfiguration(
        scenario: ActivityScenario<MainActivity>,
        returnAssistantEnabled: Boolean,
        skontoEnabled: Boolean
    ) {
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[ConfigurationViewModel::class.java]
            viewModel.setConfiguration(
                viewModel.configurationFlow.value.copy(
                    isReturnAssistantEnabled = returnAssistantEnabled,
                    isSkontoEnabled = skontoEnabled
                )
            )
        }
    }
}
