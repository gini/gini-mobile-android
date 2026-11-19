package net.gini.android.capture.ui.components.labeledvalue

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.gini.android.capture.ui.theme.GiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks what [GiniLabeledValue] hands to accessibility services.
 *
 * Compose gives TalkBack the unmerged tree: the row is the focusable node, and TalkBack speaks
 * the row's own description followed by the texts of its children. A row that carries a
 * description and still exposes its texts is therefore read twice.
 *
 * The test's unmerged tree lists every node, also those under `clearAndSetSemantics`, which
 * accessibility services never get. [isExposedToAccessibility] filters those out.
 */
@RunWith(AndroidJUnit4::class)
class GiniLabeledValueTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a row with a spoken value exposes only its description`() {
        setRow(valueContentDescription = SPOKEN_VALUE)

        composeRule.onNode(hasContentDescription(DESCRIPTION), useUnmergedTree = true).assertExists()
        composeRule
            .onAllNodes(
                hasAnyAncestor(hasContentDescription(DESCRIPTION)) and
                    (hasText(LABEL) or hasText(VALUE) or hasText(HINT)) and
                    isExposedToAccessibility(),
                useUnmergedTree = true,
            )
            .assertCountEquals(0)
    }

    @Test
    fun `a row without a spoken value is read from its texts`() {
        setRow(valueContentDescription = null)

        composeRule.onNodeWithText(LABEL, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(VALUE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(HINT, useUnmergedTree = true).assertExists()
        composeRule.onNode(hasText(LABEL) and hasText(VALUE) and hasText(HINT)).assertExists()
    }

    /** Matches nodes that no ancestor has cleared from the semantics tree. */
    private fun isExposedToAccessibility(): SemanticsMatcher =
        SemanticsMatcher("is not under clearAndSetSemantics") { node ->
            generateSequence(node.parent) { it.parent }.none { it.config.isClearingSemantics }
        }

    private fun setRow(valueContentDescription: String?) {
        composeRule.setContent {
            GiniTheme {
                GiniLabeledValue(
                    label = LABEL,
                    value = VALUE,
                    hint = HINT,
                    valueContentDescription = valueContentDescription,
                )
            }
        }
    }

    private companion object {
        const val LABEL = "Printed on the document"
        const val VALUE = "DE89 3704 0044 0532 0130 00"
        const val HINT = "From Germany, not the sender's country"
        const val SPOKEN_VALUE = "D E 8 9, 3 7 0 4, 0 0 4 4, 0 5 3 2, 0 1 3 0, 0 0"
        const val DESCRIPTION = "$LABEL, $SPOKEN_VALUE, $HINT"
    }
}
