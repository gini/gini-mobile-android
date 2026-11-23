package net.gini.android.capture.ui.components.card

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValue
import net.gini.android.capture.ui.theme.GiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders [GiniInfoCard] under Robolectric: it shows its content and resolves its colors. */
@RunWith(AndroidJUnit4::class)
class GiniInfoCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the card shows its content in order`() {
        composeRule.setContent {
            GiniTheme {
                GiniInfoCard {
                    Text(text = FIRST)
                    Text(text = SECOND)
                }
            }
        }

        val first = composeRule.onNodeWithText(FIRST).fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithText(SECOND).fetchSemanticsNode().boundsInRoot
        assertThat(second.top).isAtLeast(first.bottom)
    }

    @Test
    fun `the card pads its content`() {
        composeRule.setContent {
            GiniTheme {
                GiniInfoCard(colors = GiniInfoCardColors.colors(containerColor = Color.Red)) {
                    GiniLabeledValue(label = FIRST, value = SECOND)
                }
            }
        }

        val row = composeRule.onNodeWithText(FIRST, useUnmergedTree = true).fetchSemanticsNode()
        assertThat(row.positionInRoot.x).isGreaterThan(0f)
        assertThat(row.positionInRoot.y).isGreaterThan(0f)
        composeRule.onNodeWithText(FIRST).onParent().assertHeightIsAtLeast(CONTENT_PADDING * 2)
    }

    @Test
    fun `the default container color comes from the theme`() {
        lateinit var colors: GiniInfoCardColors
        var expected = Color.Unspecified
        composeRule.setContent {
            GiniTheme {
                colors = GiniInfoCardColors.colors()
                expected = GiniTheme.colorScheme.placeholder.background
            }
        }

        composeRule.runOnIdle {
            assertThat(colors).isEqualTo(GiniInfoCardColors(containerColor = expected))
            assertThat(colors.containerColor).isNotEqualTo(Color.Unspecified)
        }
    }

    @Test
    fun `a passed container color replaces the default`() {
        lateinit var colors: GiniInfoCardColors
        composeRule.setContent {
            GiniTheme { colors = GiniInfoCardColors.colors(containerColor = Color.Red) }
        }

        composeRule.runOnIdle {
            assertThat(colors.containerColor).isEqualTo(Color.Red)
            assertThat(GiniInfoCardColors().containerColor).isEqualTo(Color.Unspecified)
        }
    }

    private companion object {
        const val FIRST = "Sender"
        const val SECOND = "Elektro Brandt GmbH · Germany"
        val CONTENT_PADDING = 12.dp
    }
}
