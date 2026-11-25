package net.gini.android.capture.analysis.fraud

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
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
import java.util.Locale

/**
 * Renders the IBAN mismatch warning under Robolectric and checks, for each of the three cases
 * (QR vs printed, country, combined), which texts and IBAN rows it shows, and where its two
 * actions lead: "Cancel transfer" reports the cancellation, "Proceed anyway" either reports at
 * once (country case, nothing to choose) or opens the IBAN selection first.
 *
 * Buttons and rows are driven through their semantics click action rather than injected touches:
 * the warning and the selection each live in their own window, and the test is about the flow,
 * not about where the sheets end up on Robolectric's screen.
 */
@RunWith(AndroidJUnit4::class)
class IBANMismatchWarningScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
    private var cancelCount = 0
    private val proceededWith = mutableListOf<String?>()

    // region Content

    @Test
    fun `QR vs printed - shows both IBANs under the list title and no sender`() {
        setScreen(QR_VS_PRINTED_ARGS)

        heading(R.string.gc_fraud_iban_mismatch_title).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_iban_mismatch_description)).assertExists()
        heading(R.string.gc_fraud_detected_ibans_title).assertExists()
        ibanRow(R.string.gc_fraud_qr_iban_label, QR_IBAN).assertExists()
        ibanRow(R.string.gc_fraud_printed_iban_label, PRINTED_IBAN).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_sender_label)).assertDoesNotExist()
        composeRule.onAllNodes(hasCountryHint()).assertCountEquals(0)
    }

    @Test
    fun `country - lists the sender and the document IBAN with its country hint, without a list title`() {
        setScreen(COUNTRY_ARGS)

        heading(R.string.gc_fraud_country_mismatch_title).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_country_mismatch_description)).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_detected_ibans_title)).assertDoesNotExist()
        composeRule
            .onNode(hasText(string(R.string.gc_fraud_sender_label)) and hasText(senderWithCountry("DE")))
            .assertExists()
        ibanRow(R.string.gc_fraud_document_iban_label, QR_IBAN)
            .assert(hasContentDescription(countryHint("PL"), substring = true))
        composeRule.onAllNodes(hasContentDescription(string(R.string.gc_fraud_qr_iban_label), substring = true))
            .assertCountEquals(0)
    }

    @Test
    fun `country - a sender without a name is shown by its country alone`() {
        setScreen(COUNTRY_ARGS.copy(senderName = null))

        composeRule
            .onNode(hasText(string(R.string.gc_fraud_sender_label)) and hasText(country("DE")))
            .assertExists()
        composeRule.onNodeWithText(SENDER, substring = true).assertDoesNotExist()
    }

    @Test
    fun `combined - shows the sender on its card above all IBANs and hints only the foreign ones`() {
        setScreen(COMBINED_ARGS)

        heading(R.string.gc_fraud_combined_mismatch_title).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_combined_mismatch_description)).assertExists()
        heading(R.string.gc_fraud_detected_ibans_title).assertExists()
        val sender = composeRule
            .onNode(hasText(string(R.string.gc_fraud_sender_label)) and hasText(senderWithCountry("DE")))
            .fetchSemanticsNode()
        val listTitle = heading(R.string.gc_fraud_detected_ibans_title).fetchSemanticsNode()
        assertThat(sender.boundsInRoot.bottom).isAtMost(listTitle.boundsInRoot.top)
        ibanRow(R.string.gc_fraud_qr_iban_label, QR_IBAN)
            .assert(hasContentDescription(countryHint("PL"), substring = true))
        ibanRow(R.string.gc_fraud_printed_iban_label, PRINTED_IBAN).assert(!hasCountryHint())
        ibanRow(R.string.gc_fraud_printed_iban_label, SECOND_PRINTED_IBAN)
            .assert(hasContentDescription(countryHint("AT"), substring = true))
    }

    @Test
    fun `the title is the pane title of the sheet`() {
        setScreen(QR_VS_PRINTED_ARGS)

        composeRule
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    string(R.string.gc_fraud_iban_mismatch_title),
                )
            )
            .assertExists()
    }

    @Test
    fun `the protection advice expands and collapses`() {
        setScreen(QR_VS_PRINTED_ARGS)
        composeRule.onNodeWithText(string(R.string.gc_fraud_protection_body)).assertDoesNotExist()

        protectionRow().click()
        composeRule
            .onNodeWithText(string(R.string.gc_fraud_protection_body), useUnmergedTree = true)
            .assertExists()

        protectionRow().click()
        composeRule.onNodeWithText(string(R.string.gc_fraud_protection_body)).assertDoesNotExist()
    }

    @Test
    fun `the screen follows new arguments`() {
        var args by mutableStateOf(QR_VS_PRINTED_ARGS)
        composeRule.setContent {
            GiniTheme {
                IBANMismatchWarningScreen(
                    args = args,
                    onCancel = { cancelCount++ },
                    onProceed = { proceededWith += it },
                    composableProviderConfig = null,
                )
            }
        }
        heading(R.string.gc_fraud_iban_mismatch_title).assertExists()

        args = COUNTRY_ARGS

        heading(R.string.gc_fraud_country_mismatch_title).assertExists()
        composeRule.onNodeWithText(string(R.string.gc_fraud_detected_ibans_title)).assertDoesNotExist()
        // The new arguments offer nothing to choose, so "Proceed anyway" reports at once
        button(R.string.gc_proceed_anyway).click()
        assertProceededWith(null)
    }

    @Test
    fun `the screen keeps its state when its host recomposes`() {
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the screen's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                IBANMismatchWarningScreen(
                    args = COMBINED_ARGS,
                    onCancel = { cancelCount++ },
                    onProceed = { proceededWith += it },
                    composableProviderConfig = null,
                )
            }
        }
        protectionRow().click()
        button(R.string.gc_proceed_anyway).click()
        selectionRow(QR_IBAN).click()

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()

        // Still expanded, still open, still chosen
        composeRule
            .onNodeWithText(string(R.string.gc_fraud_protection_body), useUnmergedTree = true)
            .assertExists()
        selectionTitle().assertExists()
        button(R.string.gc_fraud_select_iban_continue).click()
        assertProceededWith(QR_IBAN)
    }

    @Test
    fun `country names follow the language of the SDK strings`() {
        lateinit var locale: Locale
        composeRule.setContent { locale = countryNameLocale() }

        composeRule.runOnIdle {
            assertThat(locale.toLanguageTag())
                .isEqualTo(string(R.string.gc_fraud_country_name_locale))
        }
    }

    // endregion

    // region Actions

    @Test
    fun `Cancel transfer reports the cancellation only`() {
        setScreen(COMBINED_ARGS)

        button(R.string.gc_cancel_transfer).click()

        composeRule.runOnIdle {
            assertThat(cancelCount).isEqualTo(1)
            assertThat(proceededWith).isEmpty()
        }
        selectionTitle().assertDoesNotExist()
    }

    @Test
    fun `country - Proceed anyway reports at once without a chosen IBAN`() {
        setScreen(COUNTRY_ARGS)

        button(R.string.gc_proceed_anyway).click()

        assertProceededWith(null)
        selectionTitle().assertDoesNotExist()
    }

    @Test
    fun `QR vs printed - Proceed anyway opens the selection and reports nothing yet`() {
        setScreen(QR_VS_PRINTED_ARGS)
        selectionTitle().assertDoesNotExist()

        button(R.string.gc_proceed_anyway).click()

        selectionTitle().assertExists()
        composeRule.onAllNodes(isSelectable()).assertCountEquals(2)
        composeRule.runOnIdle {
            assertThat(proceededWith).isEmpty()
            assertThat(cancelCount).isEqualTo(0)
        }
    }

    @Test
    fun `combined - the IBAN chosen on the selection is reported and the selection closes`() {
        setScreen(COMBINED_ARGS)
        button(R.string.gc_proceed_anyway).click()
        composeRule.onAllNodes(isSelectable()).assertCountEquals(3)

        selectionRow(SECOND_PRINTED_IBAN).click()
        button(R.string.gc_fraud_select_iban_continue).click()

        assertProceededWith(SECOND_PRINTED_IBAN)
        selectionTitle().assertDoesNotExist()
    }

    @Test
    fun `Continue without a choice keeps the selection open and reports nothing`() {
        setScreen(QR_VS_PRINTED_ARGS)
        button(R.string.gc_proceed_anyway).click()

        button(R.string.gc_fraud_select_iban_continue).click()

        composeRule.onNodeWithText(string(R.string.gc_fraud_select_iban_error)).assertExists()
        selectionTitle().assertExists()
        composeRule.runOnIdle { assertThat(proceededWith).isEmpty() }
    }

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun `tablet - dismissing the selection returns to the warning, which can open it again`() {
        setScreen(QR_VS_PRINTED_ARGS)
        button(R.string.gc_proceed_anyway).click()

        composeRule
            .onNode(
                SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss) and
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.PaneTitle,
                        string(R.string.gc_fraud_select_iban_title),
                    ),
            )
            .performSemanticsAction(SemanticsActions.Dismiss)

        selectionTitle().assertDoesNotExist()
        heading(R.string.gc_fraud_iban_mismatch_title).assertExists()
        composeRule.runOnIdle {
            assertThat(proceededWith).isEmpty()
            assertThat(cancelCount).isEqualTo(0)
        }

        button(R.string.gc_proceed_anyway).click()
        selectionRow(QR_IBAN).click()
        button(R.string.gc_fraud_select_iban_continue).click()

        assertProceededWith(QR_IBAN)
    }

    // endregion

    private fun setScreen(args: IBANMismatchArgs) {
        composeRule.setContent {
            GiniTheme {
                IBANMismatchWarningScreen(
                    args = args,
                    onCancel = { cancelCount++ },
                    onProceed = { proceededWith += it },
                    composableProviderConfig = GiniComposableStyleProviderConfig(),
                )
            }
        }
    }

    private fun heading(@StringRes id: Int): SemanticsNodeInteraction =
        composeRule.onNode(
            hasText(string(id)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        )

    /**
     * A row of the warning's IBAN list. It is one node described by its label, the spelled-out
     * IBAN and its hint; the selection's rows carry the same description but are selectable.
     */
    private fun ibanRow(@StringRes label: Int, iban: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasContentDescription("${string(label)}, ${formatIbanForSpeech(iban)}", substring = true) and
                !isSelectable()
        )

    private fun selectionRow(iban: String): SemanticsNodeInteraction =
        composeRule.onNode(
            hasContentDescription(formatIbanForSpeech(iban), substring = true) and isSelectable()
        )

    private fun selectionTitle(): SemanticsNodeInteraction =
        composeRule.onNodeWithText(string(R.string.gc_fraud_select_iban_title))

    private fun protectionRow(): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(R.string.gc_fraud_protection_title)) and hasClickAction())

    private fun button(@StringRes text: Int): SemanticsNodeInteraction =
        composeRule.onNode(hasText(string(text)) and hasClickAction())

    private fun SemanticsNodeInteraction.click(): SemanticsNodeInteraction =
        performSemanticsAction(SemanticsActions.OnClick)

    /** The country name in the language of the SDK strings the test runs with. */
    private fun country(code: String): String =
        countryDisplayName(code, Locale.forLanguageTag(string(R.string.gc_fraud_country_name_locale)))

    private fun senderWithCountry(countryCode: String): String =
        context.getString(R.string.gc_fraud_sender_with_country, SENDER, country(countryCode))

    private fun countryHint(countryCode: String): String =
        context.getString(R.string.gc_fraud_iban_country_hint, country(countryCode))

    /** Any country hint: the hint's text without the country name it is built around. */
    private fun hasCountryHint(): SemanticsMatcher =
        hasContentDescription(
            context.getString(R.string.gc_fraud_iban_country_hint, "").trim(),
            substring = true,
        )

    private fun assertProceededWith(iban: String?) {
        composeRule.runOnIdle {
            assertThat(proceededWith).containsExactly(iban)
            assertThat(cancelCount).isEqualTo(0)
        }
    }

    private fun string(@StringRes id: Int): String = context.getString(id)

    private companion object {
        const val QR_IBAN = "PL61109010140000071219812874"
        const val PRINTED_IBAN = "DE89370400440532013000"
        const val SECOND_PRINTED_IBAN = "AT611904300234573201"
        const val SENDER = "Elektro Brandt GmbH"
        const val HOST_LABEL = "Host"

        val QR_VS_PRINTED_ARGS = IBANMismatchArgs(
            type = IBANMismatchType.QR_VS_PRINTED,
            qrIban = IBANMismatchIban(QR_IBAN),
            printedIbans = listOf(IBANMismatchIban(PRINTED_IBAN)),
        )

        val COUNTRY_ARGS = IBANMismatchArgs(
            type = IBANMismatchType.COUNTRY,
            qrIban = null,
            printedIbans = listOf(IBANMismatchIban(QR_IBAN, hintCountryCode = "PL")),
            senderName = SENDER,
            senderCountryCode = "DE",
        )

        val COMBINED_ARGS = IBANMismatchArgs(
            type = IBANMismatchType.COMBINED,
            qrIban = IBANMismatchIban(QR_IBAN, hintCountryCode = "PL"),
            printedIbans = listOf(
                IBANMismatchIban(PRINTED_IBAN),
                IBANMismatchIban(SECOND_PRINTED_IBAN, hintCountryCode = "AT"),
            ),
            senderName = SENDER,
            senderCountryCode = "DE",
        )

        // Smallest width picks values-sw600dp (gc_is_tablet); width and height are spelled out so
        // the screen does not depend on how Robolectric derives them from the smallest width.
        const val TABLET_QUALIFIERS = "sw600dp-w600dp-h960dp"
    }
}
