package net.gini.android.capture.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.gini.android.capture.ui.components.card.GiniInfoCardPreview
import net.gini.android.capture.ui.components.expandable.GiniExpandableInfoRowPreview
import net.gini.android.capture.ui.components.footer.GiniActionFooterPreview
import net.gini.android.capture.ui.components.header.GiniHeaderBlockPreview
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValuePreview
import net.gini.android.capture.ui.components.radio.GiniRadioButtonPreview
import net.gini.android.capture.ui.components.sheet.GiniBottomSheetScaffoldBodyPreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Renders the `@Preview` composables of the sheet components under Robolectric, in light and in
 * dark mode. A preview that no longer composes (a removed resource, a changed signature default,
 * a colour that is only defined for one mode) fails here instead of silently breaking in the IDE.
 */
@RunWith(AndroidJUnit4::class)
class GiniComponentPreviewsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `header block preview`() = rendering({ GiniHeaderBlockPreview() }) {
        composeRule.onNodeWithText("Different IBANs found on this document").assertExists()
        composeRule.onNodeWithText("The IBAN in the QR code is different from the printed one")
            .assertExists()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `header block preview, dark`() = `header block preview`()

    @Test
    fun `info card preview`() = rendering({ GiniInfoCardPreview() }) {
        composeRule.onNode(hasText("Sender") and hasText("Elektro Brandt GmbH · Germany")).assertExists()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `info card preview, dark`() = `info card preview`()

    @Test
    fun `expandable info row preview`() = rendering({ GiniExpandableInfoRowPreview() }) {
        composeRule.onAllNodes(hasText("How do I protect myself?") and hasClickAction())
            .assertCountEquals(2)
        val collapsedBody = "Documents are sometimes altered to redirect payments."
        composeRule.onAllNodesWithText(collapsedBody, useUnmergedTree = true).assertCountEquals(0)

        // The first row keeps its own state in the preview
        composeRule.onAllNodes(hasClickAction())[0].performClick()

        composeRule.onAllNodesWithText(collapsedBody, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `expandable info row preview, dark`() = `expandable info row preview`()

    @Test
    fun `action footer preview`() = rendering({ GiniActionFooterPreview() }) {
        composeRule.onAllNodes(hasText("Cancel transfer") and hasClickAction()).assertCountEquals(2)
        composeRule.onAllNodes(hasText("Proceed anyway") and hasClickAction()).assertCountEquals(2)
        composeRule.onAllNodes(hasText("Continue") and hasClickAction()).assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `action footer preview, dark`() = `action footer preview`()

    @Test
    fun `radio button preview`() = rendering({ GiniRadioButtonPreview() }) {
        composeRule.onAllNodes(isSelectable()).assertCountEquals(4)
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `radio button preview, dark`() = `radio button preview`()

    @Test
    fun `labeled value preview`() = rendering({ GiniLabeledValuePreview() }) {
        composeRule.onNode(hasText("In the QR code") and hasText("PL12 1000 0111 0100 1000")).assertExists()
        composeRule
            .onNode(hasContentDescription("Printed on the document", substring = true))
            .assertExists()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `labeled value preview, dark`() = `labeled value preview`()

    @Test
    fun `bottom sheet scaffold body preview`() = rendering({ GiniBottomSheetScaffoldBodyPreview() }) {
        composeRule.onNodeWithText("Content line 1").assertExists()
        composeRule.onNodeWithText("Content line 2").assertExists()
        composeRule.onNodeWithText("Footer").assertExists()
    }

    @Test
    @Config(qualifiers = NIGHT)
    fun `bottom sheet scaffold body preview, dark`() = `bottom sheet scaffold body preview`()

    /** Renders [preview] in the mode of the calling test's qualifiers and runs [assertions]. */
    private fun rendering(preview: @Composable () -> Unit, assertions: () -> Unit) {
        composeRule.setContent { preview() }
        assertions()
    }

    private companion object {
        const val NIGHT = "night"
    }
}
