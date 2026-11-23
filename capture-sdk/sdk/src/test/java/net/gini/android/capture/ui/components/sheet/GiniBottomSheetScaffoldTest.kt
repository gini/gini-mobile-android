package net.gini.android.capture.ui.components.sheet

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniSheetColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Renders [GiniBottomSheetScaffold] under Robolectric: the sheet shows its content above its
 * optional footer, announces its pane title and offers no way to dismiss it.
 */
@RunWith(AndroidJUnit4::class)
class GiniBottomSheetScaffoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the sheet shows the content above the footer`() {
        setSheet()

        val content = composeRule.onNodeWithText(CONTENT).fetchSemanticsNode().boundsInRoot
        val footer = composeRule.onNodeWithText(FOOTER).fetchSemanticsNode().boundsInRoot
        assertThat(footer.top).isAtLeast(content.bottom)
    }

    @Test
    fun `the footer is pushed to the bottom while the content is short`() {
        setSheet()

        val content = composeRule.onNodeWithText(CONTENT).fetchSemanticsNode().boundsInRoot
        val footer = composeRule.onNodeWithText(FOOTER).fetchSemanticsNode().boundsInRoot
        // More than the paddings between the two: the weighted spacer takes the free height
        assertThat(footer.top - content.bottom).isGreaterThan(with(composeRule.density) { 64.dp.toPx() })
    }

    @Test
    fun `the footer scrolls with content that does not fit`() {
        composeRule.setContent {
            GiniTheme {
                GiniBottomSheetScaffold(
                    footer = { Text(text = FOOTER) },
                ) {
                    Text(modifier = Modifier.height(2000.dp), text = CONTENT)
                }
            }
        }

        composeRule.onNodeWithText(CONTENT).assertExists()
        composeRule.onNodeWithText(FOOTER).performScrollTo().assertExists()
    }

    @Test
    fun `the sheet works without a footer`() {
        composeRule.setContent {
            GiniTheme { GiniBottomSheetScaffold { Text(text = CONTENT) } }
        }

        composeRule.onNodeWithText(CONTENT).assertExists()
        composeRule.onNodeWithText(FOOTER).assertDoesNotExist()
    }

    @Test
    fun `the pane title is announced when the sheet opens`() {
        setSheet()

        composeRule.onNode(hasPaneTitle(PANE_TITLE)).assertExists()
    }

    @Test
    fun `without a pane title the sheet does not invent one of its own`() {
        composeRule.setContent {
            GiniTheme { GiniBottomSheetScaffold { Text(text = CONTENT) } }
        }

        composeRule.onAllNodes(hasPaneTitle(PANE_TITLE)).assertCountEquals(0)
    }

    @Test
    fun `the sheet stays open when asked to dismiss`() {
        setSheet()

        // Material offers the dismiss action on the sheet and a click on the scrim only for
        // sheets that can be dismissed; whichever is still offered must not close this one.
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .fetchSemanticsNodes()
            .forEach { node ->
                composeRule.runOnUiThread { node.config[SemanticsActions.Dismiss].action?.invoke() }
            }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(CONTENT).assertExists()
        composeRule.onNodeWithText(FOOTER).assertExists()
    }

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun `on tablets the content keeps the reading width`() {
        setSheet()

        val sheetWidth = composeRule.onNode(hasPaneTitle(PANE_TITLE)).fetchSemanticsNode().size.width
        val content = composeRule.onNodeWithText(CONTENT).fetchSemanticsNode().boundsInRoot
        assertThat(content.left).isGreaterThan(0f)
        assertThat(content.right).isLessThan(sheetWidth.toFloat())
    }

    @Test
    fun `the sheet follows the state its host keeps`() {
        var content by mutableStateOf(CONTENT)
        var showFooter by mutableStateOf(false)
        var paneTitle by mutableStateOf<String?>(null)
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the sheet's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                GiniBottomSheetScaffold(
                    modifier = Modifier,
                    paneTitle = paneTitle,
                    colors = GiniBottomSheetScaffoldColors.colors(),
                    footer = if (showFooter) {
                        { Text(text = FOOTER) }
                    } else {
                        null
                    },
                ) {
                    Text(text = content)
                }
            }
        }

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()
        composeRule.onNodeWithText(CONTENT).assertExists()
        composeRule.onNodeWithText(FOOTER).assertDoesNotExist()

        content = OTHER_CONTENT
        showFooter = true
        paneTitle = PANE_TITLE

        composeRule.onNodeWithText(OTHER_CONTENT).assertExists()
        composeRule.onNodeWithText(FOOTER).assertExists()
        composeRule.onNode(hasPaneTitle(PANE_TITLE)).assertExists()
    }

    @Test
    fun `the default colors are the sheet colors of the theme`() {
        lateinit var colors: GiniBottomSheetScaffoldColors
        lateinit var expected: GiniBottomSheetScaffoldColors
        lateinit var passed: GiniBottomSheetScaffoldColors
        composeRule.setContent {
            GiniTheme {
                colors = GiniBottomSheetScaffoldColors.colors()
                expected = GiniBottomSheetScaffoldColors(
                    containerColor = GiniSheetColors.container,
                    scrimColor = GiniSheetColors.scrim,
                )
                passed = GiniBottomSheetScaffoldColors.colors(
                    containerColor = Color.Red,
                    scrimColor = Color.Green,
                )
            }
        }

        composeRule.runOnIdle {
            assertThat(colors).isEqualTo(expected)
            assertThat(colors.containerColor).isNotEqualTo(Color.Unspecified)
            assertThat(passed).isEqualTo(GiniBottomSheetScaffoldColors(Color.Red, Color.Green))
            assertThat(GiniBottomSheetScaffoldColors().scrimColor).isEqualTo(Color.Unspecified)
        }
    }

    private fun setSheet() {
        composeRule.setContent {
            GiniTheme {
                GiniBottomSheetScaffold(
                    paneTitle = PANE_TITLE,
                    footer = { Text(text = FOOTER) },
                ) {
                    Text(text = CONTENT)
                }
            }
        }
    }

    private fun hasPaneTitle(title: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title)

    private companion object {
        const val PANE_TITLE = "Different IBANs found on this document"
        const val CONTENT = "Content"
        const val OTHER_CONTENT = "Other content"
        const val FOOTER = "Footer"
        const val HOST_LABEL = "Host"

        // Smallest width picks values-sw600dp (gc_is_tablet); width and height are spelled out so
        // the screen does not depend on how Robolectric derives them from the smallest width.
        const val TABLET_QUALIFIERS = "sw600dp-w600dp-h960dp"
    }
}
