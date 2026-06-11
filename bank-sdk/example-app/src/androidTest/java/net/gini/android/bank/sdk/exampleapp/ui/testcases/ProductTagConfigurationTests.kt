package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.ConfigurationViewModel
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.screens.ConfigurationScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.ProductTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests for productTag configuration in GiniBankConfiguration.
 *
 * sepaExtractions and cxExtractions are set via the Configuration screen UI toggle.
 * autoDetectExtractions and OtherProductTag have no UI toggle, so they are set by
 * accessing the ConfigurationViewModel directly through the activity.
 * All values are confirmed via GiniCapture.getInstance().productTag once the SDK
 * is running (after clickPhotoPaymentButton()).
 */
class ProductTagConfigurationTests {

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val configurationScreen = ConfigurationScreen()

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When no productTag is configured the SDK defaults to SepaExtractions.
     */
    @Test
    fun test1_defaultProductTagIsSepaExtractions() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()

        assertEquals(ProductTag.SepaExtractions, GiniCapture.getInstance().productTag)
    }

    /**
     * SepaExtractions can be set explicitly via the Configuration screen:
     * toggle CX on, then toggle it back off before starting the SDK.
     */
    @Test
    fun test2_sepaExtractionsCanBeSet() {
        mainScreen.clickSettingButton()
        configurationScreen.clickProductTagCxSwitch()  // SEPA → CX
        configurationScreen.clickProductTagCxSwitch()  // CX → SEPA (explicit)
        configurationScreen.assertProductTagSwitchIsUnchecked()
        pressBack()

        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()

        assertEquals(ProductTag.SepaExtractions, GiniCapture.getInstance().productTag)
    }

    /**
     * CxExtractions can be set via the Configuration screen toggle.
     */
    @Test
    fun test3_cxExtractionsCanBeSet() {
        mainScreen.clickSettingButton()
        configurationScreen.clickProductTagCxSwitch()  // SEPA → CX
        configurationScreen.assertProductTagSwitchIsChecked()
        pressBack()

        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()

        assertEquals(ProductTag.CxExtractions, GiniCapture.getInstance().productTag)
    }

    /**
     * AutoDetectExtractions can be set programmatically (no UI toggle exists for it).
     * The value is confirmed in the running SDK instance.
     */
    @Test
    fun test4_autoDetectExtractionsCanBeSet() {
        setProductTag(ProductTag.AutoDetectExtractions)

        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()

        val productTag = GiniCapture.getInstance().productTag
        assertEquals(ProductTag.AutoDetectExtractions, productTag)
        assertEquals("autoDetectExtractions", productTag.value)
    }

    /**
     * A custom/dynamic productTag value (OtherProductTag) can be set programmatically.
     * The string value is preserved and confirmed in the running SDK instance.
     */
    @Test
    fun test5_customProductTagCanBeSet() {
        val customValue = "myCustomProductTag"
        setProductTag(ProductTag.OtherProductTag(customValue))

        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()

        val productTag = GiniCapture.getInstance().productTag
        assertEquals(ProductTag.OtherProductTag(customValue), productTag)
        assertEquals(customValue, productTag.value)
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sets the productTag on MainActivity's ConfigurationViewModel directly.
     * Used for values that have no toggle in the Configuration screen UI
     * (AutoDetectExtractions, OtherProductTag).
     * ViewModelProvider returns the same scoped instance that MainActivity uses internally.
     */
    private fun setProductTag(productTag: ProductTag) {
        activityRule.scenario.onActivity { activity ->
            val vm = ViewModelProvider(activity)[ConfigurationViewModel::class.java]
            vm.setConfiguration(vm.configurationFlow.value.copy(productTag = productTag))
        }
    }
}
