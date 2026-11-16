package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity

/**
 * Sets the example app's "Screen custom loading indicator" option through MainActivity's own
 * [ConfigurationViewModel], the same hook [PaymentHintConfigurator] uses. When it is on, the app
 * hands the SDK its Lottie `CustomLottiLoadingIndicatorAdapter` for the Analysis screen.
 *
 * The configuration is applied to the SDK when the capture flow starts, so this must be called
 * before clicking the photo payment button.
 */
object ScreenLoadingIndicatorConfigurator {

    fun applyScreenCustomLoadingIndicator(
        scenario: ActivityScenario<MainActivity>,
        enabled: Boolean
    ) {
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[ConfigurationViewModel::class.java]
            viewModel.setConfiguration(
                viewModel.configurationFlow.value.copy(
                    isScreenCustomLoadingIndicatorEnabled = enabled
                )
            )
        }
    }
}
