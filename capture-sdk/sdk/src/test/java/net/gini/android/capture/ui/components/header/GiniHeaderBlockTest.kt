package net.gini.android.capture.ui.components.header

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.colors.GiniSheetColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders [GiniHeaderBlock] under Robolectric and checks what it shows and what it hands to
 * accessibility services: the title is a heading, the description is optional and the icon is
 * decorative.
 */
@RunWith(AndroidJUnit4::class)
class GiniHeaderBlockTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the title is announced as a heading`() {
        setHeader(description = DESCRIPTION)

        composeRule.onNodeWithText(TITLE).assert(isHeading())
    }

    @Test
    fun `the description is shown below the title and is not a heading`() {
        setHeader(description = DESCRIPTION)

        composeRule.onNodeWithText(DESCRIPTION).assertExists().assert(!isHeading())
    }

    @Test
    fun `without a description only the title is shown`() {
        setHeader(description = null)

        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(DESCRIPTION).assertDoesNotExist()
        composeRule.onAllNodes(hasAnyText(), useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `the icon is hidden from accessibility services`() {
        setHeader(description = DESCRIPTION)

        composeRule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
                useUnmergedTree = true,
            )
            .assertCountEquals(0)
    }

    @Test
    fun `the default colors come from the theme`() {
        lateinit var colors: GiniHeaderBlockColors
        lateinit var expected: GiniHeaderBlockColors
        composeRule.setContent {
            GiniTheme {
                colors = GiniHeaderBlockColors.colors()
                expected = GiniHeaderBlockColors(
                    iconContainerColor = GiniTheme.colorScheme.placeholder.background,
                    iconTint = GiniTheme.colorScheme.text.accent,
                    titleColor = GiniTheme.colorScheme.text.primary,
                    descriptionColor = GiniSheetColors.secondaryText,
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
        lateinit var partly: GiniHeaderBlockColors
        lateinit var fully: GiniHeaderBlockColors
        lateinit var defaults: GiniHeaderBlockColors
        composeRule.setContent {
            GiniTheme {
                defaults = GiniHeaderBlockColors.colors()
                partly = GiniHeaderBlockColors.colors(iconContainerColor = Color.Red, iconTint = Color.Green)
                fully = GiniHeaderBlockColors.colors(
                    iconContainerColor = Color.Red,
                    iconTint = Color.Green,
                    titleColor = Color.Blue,
                    descriptionColor = Color.Yellow,
                )
            }
        }

        composeRule.runOnIdle {
            assertThat(partly)
                .isEqualTo(defaults.copy(iconContainerColor = Color.Red, iconTint = Color.Green))
            assertThat(fully)
                .isEqualTo(GiniHeaderBlockColors(Color.Red, Color.Green, Color.Blue, Color.Yellow))
            assertThat(GiniHeaderBlockColors()).isEqualTo(
                GiniHeaderBlockColors(
                    Color.Unspecified,
                    Color.Unspecified,
                    Color.Unspecified,
                    Color.Unspecified,
                )
            )
        }
    }

    @Test
    fun `a new title, description and tint replace the old ones`() {
        var title by mutableStateOf(TITLE)
        var description by mutableStateOf<String?>(null)
        var tint by mutableStateOf(Color.Red)
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the header's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                GiniHeaderBlock(
                    modifier = Modifier.padding(8.dp),
                    title = title,
                    description = description,
                    icon = painterResource(R.drawable.gc_fraud_alert_icon),
                    colors = GiniHeaderBlockColors.colors(iconTint = tint),
                )
            }
        }
        composeRule.onNodeWithText(TITLE).assertExists()

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()
        composeRule.onNodeWithText(TITLE).assertExists()

        title = OTHER_TITLE
        description = DESCRIPTION
        tint = Color.Green

        composeRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeRule.onNodeWithText(OTHER_TITLE).assert(isHeading())
        composeRule.onNodeWithText(DESCRIPTION).assertExists()

        description = null

        composeRule.onNodeWithText(DESCRIPTION).assertDoesNotExist()
    }

    private fun setHeader(description: String?) {
        composeRule.setContent {
            GiniTheme {
                GiniHeaderBlock(
                    title = TITLE,
                    description = description,
                    icon = painterResource(R.drawable.gc_fraud_alert_icon),
                )
            }
        }
    }

    private fun isHeading(): SemanticsMatcher =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun hasAnyText(): SemanticsMatcher =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)

    private companion object {
        const val TITLE = "Different IBANs found on this document"
        const val OTHER_TITLE = "IBAN from a different country"
        const val HOST_LABEL = "Host"
        const val DESCRIPTION = "The IBAN in the QR code is different from the printed one"
    }
}
