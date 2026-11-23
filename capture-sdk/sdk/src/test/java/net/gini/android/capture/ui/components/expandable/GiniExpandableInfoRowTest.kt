package net.gini.android.capture.ui.components.expandable

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniSheetColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders [GiniExpandableInfoRow] under Robolectric and checks the hoisted expanded state: the
 * body is shown only when expanded, a tap reports the opposite state, and the row tells
 * accessibility services its state and what a tap does.
 */
@RunWith(AndroidJUnit4::class)
class GiniExpandableInfoRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
    private val reported = mutableListOf<Boolean>()

    @Test
    fun `a collapsed row hides the body and offers to expand`() {
        setRow(expanded = false)

        composeRule.onNodeWithText(BODY).assertDoesNotExist()
        row()
            .assert(hasStateDescription(string(R.string.gc_a11y_collapsed)))
            .assert(hasClickLabel(string(R.string.gc_a11y_expand)))
    }

    @Test
    fun `an expanded row shows the body and offers to collapse`() {
        setRow(expanded = true)

        composeRule.onNodeWithText(BODY, useUnmergedTree = true).assertExists()
        row()
            .assert(hasText(BODY))
            .assert(hasStateDescription(string(R.string.gc_a11y_expanded)))
            .assert(hasClickLabel(string(R.string.gc_a11y_collapse)))
    }

    @Test
    fun `a tap on a collapsed row asks to expand`() {
        setRow(expanded = false)

        row().performClick()

        composeRule.runOnIdle { assertThat(reported).containsExactly(true) }
        // The state is hoisted: the row does not expand by itself
        composeRule.onNodeWithText(BODY).assertDoesNotExist()
    }

    @Test
    fun `a tap on an expanded row asks to collapse`() {
        setRow(expanded = true)

        row().performClick()

        composeRule.runOnIdle { assertThat(reported).containsExactly(false) }
    }

    @Test
    fun `the row is one button of at least the minimum touch target height`() {
        setRow(expanded = false)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        row()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `the icons are hidden from accessibility services`() {
        setRow(expanded = true)

        composeRule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
                useUnmergedTree = true,
            )
            .assertCountEquals(0)
    }

    @Test
    fun `the row follows the state its host keeps`() {
        var expanded by mutableStateOf(false)
        var title by mutableStateOf(TITLE)
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the row's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                GiniExpandableInfoRow(
                    modifier = Modifier.padding(8.dp),
                    title = title,
                    body = BODY,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    icon = painterResource(R.drawable.gc_shield_icon),
                    colors = GiniExpandableInfoRowColors.colors(),
                )
            }
        }

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()
        composeRule.onNodeWithText(BODY).assertDoesNotExist()

        row().performClick()
        composeRule.onNodeWithText(BODY, useUnmergedTree = true).assertExists()
        row().assert(hasStateDescription(string(R.string.gc_a11y_expanded)))

        title = OTHER_TITLE
        composeRule.onNodeWithText(OTHER_TITLE, useUnmergedTree = true).assertExists()

        composeRule.onNode(hasClickAction()).performClick()
        composeRule.onNodeWithText(BODY).assertDoesNotExist()
        composeRule.onNode(hasClickAction())
            .assert(hasStateDescription(string(R.string.gc_a11y_collapsed)))
    }

    @Test
    fun `the default colors come from the theme`() {
        lateinit var colors: GiniExpandableInfoRowColors
        lateinit var expected: GiniExpandableInfoRowColors
        composeRule.setContent {
            GiniTheme {
                colors = GiniExpandableInfoRowColors.colors()
                expected = GiniExpandableInfoRowColors(
                    containerColor = GiniSheetColors.infoRowContainer,
                    iconTint = GiniTheme.colorScheme.text.accent,
                    titleColor = GiniTheme.colorScheme.text.primary,
                    bodyColor = GiniSheetColors.secondaryText,
                    chevronTint = GiniTheme.colorScheme.text.primary,
                )
            }
        }

        composeRule.runOnIdle {
            assertThat(colors).isEqualTo(expected)
            assertThat(colors.titleColor).isNotEqualTo(Color.Unspecified)
        }
    }

    @Test
    fun `passed colors replace the defaults`() {
        lateinit var colors: GiniExpandableInfoRowColors
        composeRule.setContent {
            GiniTheme {
                colors = GiniExpandableInfoRowColors.colors(
                    containerColor = Color.Red,
                    iconTint = Color.Green,
                    titleColor = Color.Blue,
                    bodyColor = Color.Yellow,
                    chevronTint = Color.Cyan,
                )
            }
        }

        composeRule.runOnIdle {
            assertThat(colors).isEqualTo(
                GiniExpandableInfoRowColors(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan)
            )
            assertThat(GiniExpandableInfoRowColors().containerColor).isEqualTo(Color.Unspecified)
        }
    }

    private fun setRow(expanded: Boolean) {
        composeRule.setContent {
            GiniTheme {
                GiniExpandableInfoRow(
                    title = TITLE,
                    body = BODY,
                    expanded = expanded,
                    onExpandedChange = { reported += it },
                    icon = painterResource(R.drawable.gc_shield_icon),
                )
            }
        }
    }

    private fun row(): SemanticsNodeInteraction = composeRule.onNode(hasText(TITLE) and hasClickAction())

    private fun hasStateDescription(value: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun hasClickLabel(label: String): SemanticsMatcher =
        SemanticsMatcher("click action is labelled '$label'") { node ->
            node.config.getOrNull(SemanticsActions.OnClick)?.label == label
        }

    private fun string(@StringRes id: Int): String = context.getString(id)

    private companion object {
        const val TITLE = "How do I protect myself?"
        const val OTHER_TITLE = "How can I check the IBAN?"
        const val BODY = "Documents are sometimes altered to redirect payments."
        const val HOST_LABEL = "Host"
    }
}
