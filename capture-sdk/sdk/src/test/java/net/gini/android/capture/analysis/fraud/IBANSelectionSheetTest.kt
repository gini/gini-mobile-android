package net.gini.android.capture.analysis.fraud

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.internal.iban.formatIbanForSpeech
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.theme.GiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Renders the IBAN selection sheet under Robolectric and checks its selection and error state
 * machine: "Continue" without a choice shows the error instead of reporting an IBAN, choosing a
 * row clears the error, and every row reports the raw IBAN it stands for.
 *
 * The state machine is the same in both containers, so the phone container (a modal bottom
 * sheet, the default qualifiers) carries the full set and the tablet container (a dialog,
 * `sw600dp`) repeats the essentials plus its own accessibility dismiss action.
 *
 * Rows and the button are driven through their semantics click action rather than injected
 * touches: the sheet lives in its own window and slides in, and the test is about the state
 * machine, not about where the sheet ends up on Robolectric's screen.
 */
@RunWith(AndroidJUnit4::class)
class IBANSelectionSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
    private val continuedWith = mutableListOf<String>()
    private var dismissCount = 0

    // region Phone: modal bottom sheet

    @Test
    fun `no row is selected initially`() {
        setSheet(expectTablet = false)

        composeRule.onAllNodes(isSelectable()).assertCountEquals(ALL_IBANS.size)
        composeRule.onAllNodes(isSelected()).assertCountEquals(0)
        errorText().assertDoesNotExist()
    }

    @Test
    fun `Continue without a choice shows the error and does not report an IBAN`() {
        setSheet(expectTablet = false)

        continueButton().click()

        errorText().assertExists()
        composeRule.onAllNodes(isSelectable())
            .assertCountEquals(ALL_IBANS.size)
            .assertAll(hasError(string(R.string.gc_fraud_select_iban_error)))
        assertNothingReported()
    }

    @Test
    fun `selecting a row clears the error`() {
        setSheet(expectTablet = false)
        continueButton().click()
        errorText().assertExists()

        row(QR_IBAN).click()

        errorText().assertDoesNotExist()
        composeRule.onAllNodes(isSelectable()).assertAll(hasNoError())
        row(QR_IBAN).assertIsSelected()
        // Choosing only clears the error, it takes "Continue" to report the choice
        assertNothingReported()
    }

    @Test
    fun `the QR row reports the raw QR IBAN on Continue`() {
        setSheet(expectTablet = false)

        row(QR_IBAN).click()
        continueButton().click()

        assertReported(QR_IBAN)
    }

    @Test
    fun `each printed row reports its own raw IBAN on Continue`() {
        setSheet(expectTablet = false)

        // Both rows carry the same label, only the IBAN tells them apart. The host removes the
        // sheet after "Continue"; left composed here, it takes the second choice as well.
        row(FIRST_PRINTED_IBAN).click()
        continueButton().click()
        row(SECOND_PRINTED_IBAN).click()
        continueButton().click()

        assertReported(FIRST_PRINTED_IBAN, SECOND_PRINTED_IBAN)
    }

    @Test
    fun `choosing another row moves the selection`() {
        setSheet(expectTablet = false)

        row(QR_IBAN).click()
        row(SECOND_PRINTED_IBAN).click()

        row(QR_IBAN).assertIsNotSelected()
        row(FIRST_PRINTED_IBAN).assertIsNotSelected()
        row(SECOND_PRINTED_IBAN).assertIsSelected()
        composeRule.onAllNodes(isSelected()).assertCountEquals(1)
    }

    @Test
    fun `the sheet lists the IBANs of new arguments and forgets nothing it was told`() {
        var args by mutableStateOf(ARGS)
        composeRule.setContent {
            GiniTheme {
                IBANSelectionSheet(
                    args = args,
                    onDismiss = { dismissCount++ },
                    onContinue = { continuedWith += it },
                    composableProviderConfig = null,
                )
            }
        }
        row(QR_IBAN).click()
        row(QR_IBAN).assertIsSelected()

        args = ARGS.copy(printedIbans = listOf(IBANMismatchIban(FIRST_PRINTED_IBAN, hintCountryCode = "DE")))

        composeRule.onAllNodes(isSelectable()).assertCountEquals(2)
        row(SECOND_PRINTED_IBAN).assertDoesNotExist()
        // The choice is the sheet's own state and outlives the new arguments
        row(QR_IBAN).assertIsSelected()
        continueButton().click()
        assertReported(QR_IBAN)
    }

    @Test
    fun `every Continue without a choice keeps the error and reports nothing`() {
        setSheet(expectTablet = false)

        continueButton().click()
        continueButton().click()

        errorText().assertExists()
        assertNothingReported()
    }

    // endregion

    // region Tablet: dialog

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun `tablet - Continue without a choice shows the error and does not report an IBAN`() {
        setSheet(expectTablet = true)

        continueButton().click()

        errorText().assertExists()
        composeRule.onAllNodes(isSelectable())
            .assertCountEquals(ALL_IBANS.size)
            .assertAll(hasError(string(R.string.gc_fraud_select_iban_error)))
        assertNothingReported()
    }

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun `tablet - a chosen row clears the error and is reported on Continue`() {
        setSheet(expectTablet = true)
        continueButton().click()
        errorText().assertExists()

        row(SECOND_PRINTED_IBAN).click()
        errorText().assertDoesNotExist()
        continueButton().click()

        assertReported(SECOND_PRINTED_IBAN)
    }

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun `tablet - the accessibility dismiss action closes the dialog`() {
        setSheet(expectTablet = true)

        composeRule
            .onNode(
                SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss) and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.PaneTitle,
                        string(R.string.gc_fraud_select_iban_title),
                    ),
            )
            .performSemanticsAction(SemanticsActions.Dismiss)

        composeRule.waitForIdle()
        assertThat(dismissCount).isEqualTo(1)
        assertThat(continuedWith).isEmpty()
    }

    // endregion

    private fun setSheet(expectTablet: Boolean) {
        // Guards the premise of the test: the qualifiers have to pick the container under test
        assertThat(context.resources.getBoolean(R.bool.gc_is_tablet)).isEqualTo(expectTablet)

        composeRule.setContent {
            GiniTheme {
                IBANSelectionSheet(
                    args = ARGS,
                    onDismiss = { dismissCount++ },
                    onContinue = { continuedWith += it },
                    composableProviderConfig = GiniComposableStyleProviderConfig(),
                )
            }
        }
    }

    /**
     * The row of [iban]. A row is one node described by its label and the spelled-out IBAN; the
     * label repeats on the printed rows, so the IBAN identifies the row.
     */
    private fun row(iban: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasContentDescription(formatIbanForSpeech(iban), substring = true) and isSelectable()
        )

    private fun continueButton(): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(R.string.gc_fraud_select_iban_continue)) and hasClickAction())

    private fun errorText(): SemanticsNodeInteraction =
        composeRule.onNodeWithText(string(R.string.gc_fraud_select_iban_error))

    private fun SemanticsNodeInteraction.click(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    private fun hasError(message: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Error, message)

    private fun hasNoError(): SemanticsMatcher =
        SemanticsMatcher.keyNotDefined(SemanticsProperties.Error)

    private fun assertReported(vararg ibans: String) {
        composeRule.waitForIdle()
        assertThat(continuedWith).containsExactlyElementsIn(ibans).inOrder()
        assertThat(dismissCount).isEqualTo(0)
    }

    private fun assertNothingReported() {
        composeRule.waitForIdle()
        assertThat(continuedWith).isEmpty()
        assertThat(dismissCount).isEqualTo(0)
    }

    private fun string(@StringRes id: Int): String = context.getString(id)

    private companion object {
        const val QR_IBAN = "PL61109010140000071219812874"
        const val FIRST_PRINTED_IBAN = "DE89370400440532013000"
        const val SECOND_PRINTED_IBAN = "AT611904300234573201"

        val ALL_IBANS = listOf(QR_IBAN, FIRST_PRINTED_IBAN, SECOND_PRINTED_IBAN)

        val ARGS = IBANSelectionArgs(
            qrIban = IBANMismatchIban(QR_IBAN),
            printedIbans = listOf(
                IBANMismatchIban(FIRST_PRINTED_IBAN),
                IBANMismatchIban(SECOND_PRINTED_IBAN),
            ),
        )

        // Smallest width picks values-sw600dp (gc_is_tablet); width and height are spelled out so
        // the screen does not depend on how Robolectric derives them from the smallest width.
        const val TABLET_QUALIFIERS = "sw600dp-w600dp-h960dp"
    }
}
