package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.capture.GiniCapture

/**
 * Sets the payment-hint flags and threshold through the ConfigurationViewModel, the same
 * hook ProductTagConfigurationTests uses for values without a reliable UI toggle. The
 * configuration is applied to the SDK when the capture flow starts, so it must be called
 * before clicking the photo payment button.
 *
 * Note: these are the CLIENT-side flags. The server-side /configurations flags for the
 * gini-mobile-test client must both be true (verified 2026-08-17) — the sheet
 * needs both gates.
 */
object PaymentHintConfigurator {

    fun applyHintConfiguration(
        scenario: ActivityScenario<MainActivity>,
        paymentDueHintEnabled: Boolean,
        paymentScheduleHintEnabled: Boolean,
        paymentDueHintThresholdDays: Int = GiniCapture.PAYMENT_DUE_HINT_THRESHOLD_DAYS
    ) {
        scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[ConfigurationViewModel::class.java]
            viewModel.setConfiguration(
                viewModel.configurationFlow.value.copy(
                    isPaymentDueHintEnabled = paymentDueHintEnabled,
                    isPaymentScheduleHintEnabled = paymentScheduleHintEnabled,
                    paymentDueHintThresholdDays = paymentDueHintThresholdDays
                )
            )
        }
    }
}
