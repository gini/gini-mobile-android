package net.gini.android.capture.ui.components.footer

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.theme.GiniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Renders the footer with both buttons under Robolectric and checks where they end up: stacked
 * on phones in portrait, side by side on phones in landscape and on tablets in both
 * orientations.
 */
@RunWith(AndroidJUnit4::class)
class GiniActionFooterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `phone in portrait stacks the buttons`() {
        setFooter(expectTablet = false, expectLandscape = false)

        assertStacked()
    }

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE_QUALIFIERS)
    fun `phone in landscape places the buttons side by side`() {
        setFooter(expectTablet = false, expectLandscape = true)

        assertSideBySide()
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT_QUALIFIERS)
    fun `tablet in portrait places the buttons side by side`() {
        setFooter(expectTablet = true, expectLandscape = false)

        assertSideBySide()
    }

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE_QUALIFIERS)
    fun `tablet in landscape places the buttons side by side`() {
        setFooter(expectTablet = true, expectLandscape = true)

        assertSideBySide()
    }

    private fun setFooter(expectTablet: Boolean, expectLandscape: Boolean) {
        // Guards the premise of the test: the qualifiers have to pick the device under test
        val resources = context.resources
        assertThat(resources.getBoolean(R.bool.gc_is_tablet)).isEqualTo(expectTablet)
        assertThat(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            .isEqualTo(expectLandscape)

        composeRule.setContent {
            GiniTheme {
                GiniActionFooter(
                    primaryText = PRIMARY_TEXT,
                    onPrimaryClick = {},
                    secondaryText = SECONDARY_TEXT,
                    onSecondaryClick = {},
                    composableProviderConfig = GiniComposableStyleProviderConfig(),
                )
            }
        }
    }

    private fun assertSideBySide() {
        val primary = bounds(PRIMARY_TEXT)
        val secondary = bounds(SECONDARY_TEXT)

        assertThat(secondary.top).isEqualTo(primary.top)
        assertThat(secondary.left).isAtLeast(primary.right)
    }

    private fun assertStacked() {
        val primary = bounds(PRIMARY_TEXT)
        val secondary = bounds(SECONDARY_TEXT)

        assertThat(secondary.left).isEqualTo(primary.left)
        assertThat(secondary.top).isAtLeast(primary.bottom)
    }

    private fun bounds(text: String): Rect =
        composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    private companion object {
        const val PRIMARY_TEXT = "Cancel transfer"
        const val SECONDARY_TEXT = "Proceed anyway"
        const val PHONE_LANDSCAPE_QUALIFIERS = "w640dp-h360dp-land"
        const val TABLET_PORTRAIT_QUALIFIERS = "sw600dp-w600dp-h960dp-port"
        const val TABLET_LANDSCAPE_QUALIFIERS = "sw600dp-w960dp-h600dp-land"
    }
}
