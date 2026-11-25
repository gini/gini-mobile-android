package net.gini.android.capture.analysis.fraud

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.gini.android.capture.R
import net.gini.android.capture.internal.iban.formatIbanForSpeech
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Renders the `@Preview` composables of the IBAN mismatch warning and the IBAN selection sheet
 * under Robolectric, in light and in dark mode, and checks that each shows the state its name
 * promises. A preview that no longer composes fails here instead of silently breaking in the IDE.
 */
@RunWith(AndroidJUnit4::class)
class FraudSheetPreviewsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // region IBAN mismatch warning

    @Test
    fun `warning, QR vs printed`() = rendering({ IBANMismatchQrVsPrintedPreview() }) {
        composeRule.onNodeWithText(string(R.string.gc_fraud_iban_mismatch_title)).assertExists()
        ibanRows().assertCountEquals(2)
        assertWarningFooter()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `warning, QR vs printed, dark`() = `warning, QR vs printed`()

    @Test
    fun `warning, country`() = rendering({ IBANMismatchCountryPreview() }) {
        composeRule.onNodeWithText(string(R.string.gc_fraud_country_mismatch_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_detected_ibans_title)).assertDoesNotExist()
        ibanRows().assertCountEquals(1)
        assertWarningFooter()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `warning, country, dark`() = `warning, country`()

    @Test
    fun `warning, combined`() = rendering({ IBANMismatchCombinedPreview() }) {
        composeRule.onNodeWithText(string(R.string.gc_fraud_combined_mismatch_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_protection_body)).assertDoesNotExist()
        ibanRows().assertCountEquals(2)
        assertWarningFooter()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `warning, combined, dark`() = `warning, combined`()

    @Test
    fun `warning, combined with two printed IBANs, expanded`() =
        rendering({ IBANMismatchCombinedTwoPrintedPreview() }) {
            composeRule.onNodeWithText(string(R.string.gc_fraud_combined_mismatch_title)).assertExists()
            composeRule
                .onNodeWithText(string(R.string.gc_fraud_protection_body), useUnmergedTree = true)
                .assertExists()
            ibanRows().assertCountEquals(3)
        }

    // endregion

    // region IBAN selection sheet

    @Test
    fun `selection, initial`() = rendering({ IBANSelectionInitialPreview() }) {
        composeRule.onAllNodes(isSelectable()).assertCountEquals(2)
        composeRule.onAllNodes(isSelected()).assertCountEquals(0)
        errorText().assertDoesNotExist()
        assertSelectionFooter()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `selection, initial, dark`() = `selection, initial`()

    @Test
    fun `selection, error`() = rendering({ IBANSelectionErrorPreview() }) {
        composeRule.onAllNodes(isSelected()).assertCountEquals(0)
        errorText().assertExists()
        composeRule.onAllNodes(isSelectable() and hasError()).assertCountEquals(2)
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `selection, error, dark`() = `selection, error`()

    @Test
    fun `selection, selected`() = rendering({ IBANSelectionSelectedPreview() }) {
        composeRule.onNode(isSelected())
            .assert(hasContentDescription(formatIbanForSpeech(QR_IBAN), substring = true))
        errorText().assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `selection, selected, dark`() = `selection, selected`()

    @Test
    fun `selection, combined, selected`() = rendering({ IBANSelectionCombinedSelectedPreview() }) {
        composeRule.onAllNodes(isSelectable()).assertCountEquals(3)
        composeRule.onAllNodes(isSelected()).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `selection, combined, selected, dark`() = `selection, combined, selected`()

    @Test
    fun `selection, combined, error`() = rendering({ IBANSelectionCombinedErrorPreview() }) {
        composeRule.onAllNodes(isSelectable() and hasError()).assertCountEquals(3)
        errorText().assertExists()
    }

    // endregion

    /** Renders [preview] in the mode of the calling test's qualifiers and runs [assertions]. */
    private fun rendering(preview: @Composable () -> Unit, assertions: () -> Unit) {
        composeRule.setContent { preview() }
        assertions()
    }

    /** The IBAN rows: each is one node described by its label and the spelled-out IBAN. */
    private fun ibanRows() =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))

    private fun assertWarningFooter() {
        composeRule.onNode(hasText(string(R.string.gc_cancel_transfer)) and hasClickAction()).assertExists()
        composeRule.onNode(hasText(string(R.string.gc_proceed_anyway)) and hasClickAction()).assertExists()
    }

    private fun assertSelectionFooter() {
        composeRule.onNodeWithText(string(R.string.gc_fraud_select_iban_footnote)).assertExists()
        composeRule
            .onNode(hasText(string(R.string.gc_fraud_select_iban_continue)) and hasClickAction())
            .assertExists()
    }

    private fun errorText() = composeRule.onNodeWithText(string(R.string.gc_fraud_select_iban_error))

    private fun hasError(): SemanticsMatcher =
        SemanticsMatcher.expectValue(
            SemanticsProperties.Error,
            string(R.string.gc_fraud_select_iban_error),
        )

    private fun string(@StringRes id: Int): String = context.getString(id)

    private companion object {
        const val NIGHT = "night"

        /** The QR IBAN of the selection previews. */
        const val QR_IBAN = "PL12100001110100100012345678"
    }
}
