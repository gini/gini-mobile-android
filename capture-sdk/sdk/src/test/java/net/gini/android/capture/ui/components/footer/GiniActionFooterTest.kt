package net.gini.android.capture.ui.components.footer

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.components.button.filled.GiniButtonColors
import net.gini.android.capture.ui.components.button.outlined.GiniOutlinedButtonColors
import net.gini.android.capture.ui.theme.GiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Renders [GiniActionFooter] under Robolectric and checks its two layouts (stacked on phones in
 * portrait, side by side on phones in landscape and on tablets), the optional secondary button
 * and the click callbacks.
 */
@RunWith(AndroidJUnit4::class)
class GiniActionFooterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var primaryClicks = 0
    private var secondaryClicks = 0

    @Test
    fun `in portrait the buttons are stacked, primary first, and equally wide`() {
        setFooter(secondaryText = SECONDARY)

        val primary = bounds(PRIMARY)
        val secondary = bounds(SECONDARY)
        assertThat(secondary.top).isAtLeast(primary.bottom)
        assertThat(secondary.left).isEqualTo(primary.left)
        assertThat(secondary.width).isEqualTo(primary.width)
    }

    @Test
    @Config(qualifiers = "land")
    fun `in landscape the buttons sit side by side, equally wide and tall`() {
        setFooter(secondaryText = SECONDARY)

        assertSideBySide()
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT_QUALIFIERS)
    fun `on a tablet in portrait the buttons sit side by side`() {
        assertDevice(expectTablet = true, expectLandscape = false)
        setFooter(secondaryText = SECONDARY)

        assertSideBySide()
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE_QUALIFIERS)
    fun `on a tablet in landscape the buttons sit side by side`() {
        assertDevice(expectTablet = true, expectLandscape = true)
        setFooter(secondaryText = SECONDARY)

        assertSideBySide()
    }

    @Test
    fun `the horizontal layout can be forced in portrait`() {
        setFooter(secondaryText = SECONDARY, horizontal = true)

        assertSideBySide()
    }

    @Test
    @Config(qualifiers = "land")
    fun `the stacked layout can be forced in landscape`() {
        setFooter(secondaryText = SECONDARY, horizontal = false)

        assertThat(bounds(SECONDARY).top).isAtLeast(bounds(PRIMARY).bottom)
    }

    @Test
    fun `without a secondary text only the primary button is shown`() {
        setFooter(secondaryText = null)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNodeWithText(PRIMARY).assertExists()
    }

    @Test
    fun `a lone primary button takes the full width in the horizontal layout as well`() {
        setFooter(secondaryText = null, horizontal = true)

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        val rootWidth = composeRule.onRoot().fetchSemanticsNode().size.width
        assertThat(bounds(PRIMARY).width).isEqualTo(rootWidth.toFloat())
    }

    @Test
    fun `each button reports its own taps`() {
        setFooter(secondaryText = SECONDARY)

        button(PRIMARY).performClick()
        composeRule.runOnIdle {
            assertThat(primaryClicks).isEqualTo(1)
            assertThat(secondaryClicks).isEqualTo(0)
        }

        button(SECONDARY).performClick()
        button(SECONDARY).performClick()
        composeRule.runOnIdle {
            assertThat(primaryClicks).isEqualTo(1)
            assertThat(secondaryClicks).isEqualTo(2)
        }
    }

    @Test
    fun `each button reports its own taps in the horizontal layout`() {
        setFooter(secondaryText = SECONDARY, horizontal = true)

        button(SECONDARY).performClick()
        button(PRIMARY).performClick()

        composeRule.runOnIdle {
            assertThat(primaryClicks).isEqualTo(1)
            assertThat(secondaryClicks).isEqualTo(1)
        }
    }

    @Test
    fun `a secondary button without a listener ignores taps`() {
        composeRule.setContent {
            GiniTheme {
                GiniActionFooter(
                    primaryText = PRIMARY,
                    onPrimaryClick = { primaryClicks++ },
                    secondaryText = SECONDARY,
                    composableProviderConfig = null,
                )
            }
        }

        button(SECONDARY).performClick()

        composeRule.runOnIdle { assertThat(primaryClicks).isEqualTo(0) }
    }

    @Test
    fun `the footer follows the state its host keeps`() {
        var primaryText by mutableStateOf(PRIMARY)
        var secondaryText by mutableStateOf<String?>(null)
        var horizontal by mutableStateOf(false)
        var hostRecompositions by mutableIntStateOf(0)
        composeRule.setContent {
            GiniTheme {
                // Read here so the host recomposes while the footer's inputs stay the same
                Text(text = "$HOST_LABEL $hostRecompositions")
                GiniActionFooter(
                    modifier = Modifier.padding(8.dp),
                    primaryText = primaryText,
                    onPrimaryClick = { primaryClicks++ },
                    secondaryText = secondaryText,
                    onSecondaryClick = { secondaryClicks++ },
                    composableProviderConfig = GiniComposableStyleProviderConfig(),
                    horizontal = horizontal,
                    primaryColors = GiniButtonColors.colors(),
                    secondaryColors = GiniOutlinedButtonColors.colors(),
                )
            }
        }

        hostRecompositions++
        composeRule.onNodeWithText("$HOST_LABEL 1").assertExists()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)

        secondaryText = SECONDARY
        assertThat(bounds(SECONDARY).top).isAtLeast(bounds(PRIMARY).bottom)

        horizontal = true
        assertSideBySide()

        primaryText = OTHER_PRIMARY
        composeRule.onNodeWithText(PRIMARY).assertDoesNotExist()
        button(OTHER_PRIMARY).performClick()
        composeRule.runOnIdle { assertThat(primaryClicks).isEqualTo(1) }

        secondaryText = null
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    private fun setFooter(secondaryText: String?, horizontal: Boolean? = null) {
        composeRule.setContent {
            GiniTheme {
                if (horizontal == null) {
                    GiniActionFooter(
                        primaryText = PRIMARY,
                        onPrimaryClick = { primaryClicks++ },
                        secondaryText = secondaryText,
                        onSecondaryClick = { secondaryClicks++ },
                        composableProviderConfig = null,
                    )
                } else {
                    GiniActionFooter(
                        primaryText = PRIMARY,
                        onPrimaryClick = { primaryClicks++ },
                        secondaryText = secondaryText,
                        onSecondaryClick = { secondaryClicks++ },
                        composableProviderConfig = null,
                        horizontal = horizontal,
                    )
                }
            }
        }
    }

    /** Guards the premise of a test: the qualifiers have to pick the device under test. */
    private fun assertDevice(expectTablet: Boolean, expectLandscape: Boolean) {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        assertThat(resources.getBoolean(R.bool.gc_is_tablet)).isEqualTo(expectTablet)
        assertThat(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            .isEqualTo(expectLandscape)
    }

    private fun assertSideBySide() {
        val primary = bounds(PRIMARY)
        val secondary = bounds(SECONDARY)
        assertThat(secondary.left).isAtLeast(primary.right)
        assertThat(secondary.top).isEqualTo(primary.top)
        assertThat(secondary.height).isEqualTo(primary.height)
        assertThat(secondary.width).isWithin(1f).of(primary.width)
    }

    private fun button(text: String) = composeRule.onNode(hasText(text) and hasClickAction())

    private fun bounds(text: String): Rect = button(text).fetchSemanticsNode().boundsInRoot

    private companion object {
        const val PRIMARY = "Cancel transfer"
        const val OTHER_PRIMARY = "Stop"
        const val SECONDARY = "Proceed anyway"
        const val HOST_LABEL = "Host"
        const val TABLET_PORTRAIT_QUALIFIERS = "sw600dp-w600dp-h960dp-port"
        const val TABLET_LANDSCAPE_QUALIFIERS = "sw600dp-w960dp-h600dp-land"
    }
}
