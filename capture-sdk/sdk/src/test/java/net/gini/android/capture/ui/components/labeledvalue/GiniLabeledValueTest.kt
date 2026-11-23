package net.gini.android.capture.ui.components.labeledvalue

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniSheetColors
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

    @Test
    fun `the row follows the state its host keeps`() {
        var value by mutableStateOf(VALUE)
        var hint by mutableStateOf<String?>(null)
        var spokenValue by mutableStateOf<String?>(null)
        var showLeadingContent by mutableStateOf(false)
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the row's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                GiniLabeledValue(
                    modifier = Modifier.padding(8.dp),
                    label = LABEL,
                    value = value,
                    hint = hint,
                    valueContentDescription = spokenValue,
                    colors = GiniLabeledValueColors.colors(),
                    leadingContent = if (showLeadingContent) {
                        { Text(text = LEADING) }
                    } else {
                        null
                    },
                )
            }
        }

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()
        composeRule.onNode(hasText(LABEL) and hasText(VALUE)).assertExists()
        composeRule.onNodeWithText(HINT, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText(LEADING, useUnmergedTree = true).assertDoesNotExist()

        value = OTHER_VALUE
        hint = HINT
        showLeadingContent = true

        composeRule.onNode(hasText(LABEL) and hasText(OTHER_VALUE) and hasText(HINT)).assertExists()
        val leading = composeRule.onNodeWithText(LEADING, useUnmergedTree = true).fetchSemanticsNode()
        val label = composeRule.onNodeWithText(LABEL, useUnmergedTree = true).fetchSemanticsNode()
        assertThat(leading.boundsInRoot.right).isAtMost(label.boundsInRoot.left)

        spokenValue = SPOKEN_VALUE

        composeRule.onNode(hasContentDescription(DESCRIPTION), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the default colors come from the theme and passed colors replace them`() {
        lateinit var colors: GiniLabeledValueColors
        lateinit var expected: GiniLabeledValueColors
        lateinit var passed: GiniLabeledValueColors
        composeRule.setContent {
            GiniTheme {
                colors = GiniLabeledValueColors.colors()
                expected = GiniLabeledValueColors(
                    labelColor = GiniSheetColors.label,
                    valueColor = GiniTheme.colorScheme.text.primary,
                    hintColor = GiniTheme.colorScheme.errorHint.content,
                )
                passed = GiniLabeledValueColors.colors(
                    labelColor = Color.Red,
                    valueColor = Color.Green,
                    hintColor = Color.Blue,
                )
            }
        }

        composeRule.runOnIdle {
            assertThat(colors).isEqualTo(expected)
            assertThat(colors.labelColor).isNotEqualTo(Color.Unspecified)
            assertThat(passed).isEqualTo(GiniLabeledValueColors(Color.Red, Color.Green, Color.Blue))
            assertThat(GiniLabeledValueColors().valueColor).isEqualTo(Color.Unspecified)
        }
    }

    @Test
    fun `an item carries the texts of a row and its optional parts default to none`() {
        val item = GiniLabeledValueItem(label = LABEL, value = VALUE)

        assertThat(item.hint).isNull()
        assertThat(item.valueContentDescription).isNull()
        assertThat(item.copy(hint = HINT, valueContentDescription = SPOKEN_VALUE))
            .isEqualTo(GiniLabeledValueItem(LABEL, VALUE, HINT, SPOKEN_VALUE))
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
        const val OTHER_VALUE = "AT61 1904 3002 3457 3201"
        const val LEADING = "Leading"
        const val HOST_LABEL = "Host"
        const val HINT = "From Germany, not the sender's country"
        const val SPOKEN_VALUE = "D E 8 9, 3 7 0 4, 0 0 4 4, 0 5 3 2, 0 1 3 0, 0 0"
        const val DESCRIPTION = "$LABEL, $SPOKEN_VALUE, $HINT"
    }
}
