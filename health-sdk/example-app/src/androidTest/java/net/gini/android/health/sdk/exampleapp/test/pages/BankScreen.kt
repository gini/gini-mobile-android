package net.gini.android.health.sdk.exampleapp.test.pages

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.gini.android.health.sdk.exampleapp.test.testdata.TestData
import org.junit.Assert.assertEquals

/**
 * Page Object Model for the bank-sdk example app payment screen.
 *
 * Hybrid Espresso + UiAutomator approach:
 *  1. Espresso drives the Health app up to and including tapping "Pay"
 *  2. [switchToBankApp] switches context via launchIntentForPackage
 *     (bank app is pre-installed by Gradle before the tests run)
 *  3. UiAutomator verifies Bank app fields and taps Pay
 *  4. [tapPayAndReturnToBusiness] waits for the deep-link return to Health app
 *  5. Espresso regains control of the Health app
 */
class BankScreen {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)

    companion object {
        private const val LAUNCH_TIMEOUT_MS = 5_000L
        private const val FIELD_TIMEOUT_MS  = 3_000L
    }

    /**
     * Switches context from the Health app to the Bank app.
     *
     * The Health SDK already fires the payment intent when "Pay" is tapped,
     * so the Bank app may already be in the foreground. If not, we explicitly
     * launch it via [android.content.pm.PackageManager.getLaunchIntentForPackage].
     */
    fun switchToBankApp(): BankScreen {

        val bankAppVisible = device.wait(
            Until.hasObject(By.pkg(TestData.AppPackages.BANK_SDK_EXAMPLE_APP).depth(0)),
            LAUNCH_TIMEOUT_MS
        )

        if (!bankAppVisible) {
            // Fallback: explicitly launch the bank app
            val launchIntent = instrumentation.targetContext.packageManager
                .getLaunchIntentForPackage(TestData.AppPackages.BANK_SDK_EXAMPLE_APP)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }

            requireNotNull(launchIntent) {
                "Bank app not installed: ${TestData.AppPackages.BANK_SDK_EXAMPLE_APP}"
            }

            instrumentation.targetContext.startActivity(launchIntent)

            device.wait(
                Until.hasObject(By.pkg(TestData.AppPackages.BANK_SDK_EXAMPLE_APP).depth(0)),
                LAUNCH_TIMEOUT_MS
            )
        }

        return this
    }

    /**
     * Uses UiAutomator to verify all 4 extracted payment fields in the Bank app.
     * Must be called after [switchToBankApp].
     */
    fun verifyPaymentDetails(
        expectedRecipient: String,
        expectedIban: String,
        expectedAmount: String,
        expectedPurpose: String
    ): BankScreen {
        // Verify Recipient
        val recipientField = device.wait(
            Until.findObject(By.res(TestData.AppPackages.BANK_SDK_EXAMPLE_APP, "recipient")),
            FIELD_TIMEOUT_MS
        )
        val actualRecipient = recipientField?.text ?: ""
        println("✅ Bank App Recipient: $actualRecipient")
        assertEquals("Recipient mismatch in bank app", expectedRecipient, actualRecipient)

        // Verify IBAN
        val actualIban = device
            .findObject(By.res(TestData.AppPackages.BANK_SDK_EXAMPLE_APP, "iban"))
            ?.text ?: ""
        println("✅ Bank App IBAN: $actualIban")
        assertEquals("IBAN mismatch in bank app", expectedIban, actualIban)

        // Verify Amount
        val actualAmount = device
            .findObject(By.res(TestData.AppPackages.BANK_SDK_EXAMPLE_APP, "amount"))
            ?.text ?: ""
        println("✅ Bank App Amount: $actualAmount")
        assertEquals("Amount mismatch in bank app", expectedAmount, actualAmount)

        // Verify Purpose
        val actualPurpose = device
            .findObject(By.res(TestData.AppPackages.BANK_SDK_EXAMPLE_APP, "purpose"))
            ?.text ?: ""
        println("✅ Bank App Purpose: $actualPurpose")
        if (expectedPurpose.isNotEmpty()) {
            assertEquals("Purpose mismatch in bank app", expectedPurpose, actualPurpose)
        }

        return this
    }

    /**
     * Taps the Pay (resolve_payment) button in the Bank app,
     * waits for the "Return to Business" button to confirm payment resolved,
     * taps it to trigger the deep-link back to the Health app,
     * then waits for the Health app to return to the foreground.
     * After this call, Espresso regains control of the Health app.
     */
    fun tapPayAndReturnToBusiness(): BankScreen {
        // Tap resolve_payment (UiAutomator — cross-app)
        device.findObject(
            By.res(TestData.AppPackages.BANK_SDK_EXAMPLE_APP, "resolve_payment")
        )?.click()

        // Wait for "Return to Business" button — confirms payment was processed
        device.wait(
            Until.findObject(
                By.res(TestData.AppPackages.BANK_SDK_EXAMPLE_APP, "return_to_payment_initiator_app")
            ),
            FIELD_TIMEOUT_MS
        )

        // Tap it — triggers deep-link back to Health app
        device.findObject(
            By.res(TestData.AppPackages.BANK_SDK_EXAMPLE_APP, "return_to_payment_initiator_app")
        )?.click()

        // Wait for the Health app to return to the foreground — Espresso takes over from here
        device.wait(
            Until.hasObject(By.pkg(TestData.AppPackages.HEALTH_SDK_EXAMPLE_APP).depth(0)),
            LAUNCH_TIMEOUT_MS
        )

        return this
    }
}
