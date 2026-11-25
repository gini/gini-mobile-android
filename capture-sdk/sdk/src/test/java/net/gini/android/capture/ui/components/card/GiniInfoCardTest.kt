package net.gini.android.capture.ui.components.card

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    fun `the card follows the state its host keeps`() {
        var content by mutableStateOf(FIRST)
        var container by mutableStateOf(Color.Red)
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the card's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                GiniInfoCard(
                    modifier = Modifier.padding(8.dp),
                    colors = GiniInfoCardColors.colors(containerColor = container),
                ) {
                    Text(text = content)
                }
            }
        }

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()
        composeRule.onNodeWithText(FIRST).assertExists()

        content = SECOND
        container = Color.Green

        composeRule.onNodeWithText(FIRST).assertDoesNotExist()
        composeRule.onNodeWithText(SECOND).assertExists()
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
        const val HOST_LABEL = "Host"
        val CONTENT_PADDING = 12.dp
    }
}
