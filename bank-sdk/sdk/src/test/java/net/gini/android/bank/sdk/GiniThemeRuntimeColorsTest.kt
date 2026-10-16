package net.gini.android.bank.sdk

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.internal.ui.runtimecolors.giniFixedColorPrimitives
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives
import net.gini.android.capture.ui.theme.colors.GiniColorScheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that Compose screens wrapped in [GiniTheme] use the colors of the
 * [CaptureConfiguration.customResourceProvider].
 */
@RunWith(AndroidJUnit4::class)
class GiniThemeRuntimeColorsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        GiniBank.cleanupCapture(context)
    }

    @Test
    fun `GiniTheme uses the provider color for accent01 slots`() {
        configure { name, _ -> if (name == "gc_accent_01") MAGENTA else null }

        val scheme = captureScheme()

        assertThat(scheme.button.container).isEqualTo(Color(MAGENTA))
    }

    @Test
    fun `GiniTheme keeps the resource color when the provider returns null`() {
        configure { _, _ -> null }

        val scheme = captureScheme()

        assertThat(scheme.button.container)
            .isEqualTo(Color(context.getColor(net.gini.android.capture.R.color.gc_accent_01)))
    }

    @Test
    fun `fixed palette uses provider light colors with a provider and defaults without one`() {
        configure { name, isDarkMode -> if (name == "gc_dark_01" && !isDarkMode) MAGENTA else null }
        lateinit var withProvider: GiniColorPrimitives
        composeTestRule.setContent { withProvider = giniFixedColorPrimitives() }
        composeTestRule.waitForIdle()

        assertThat(withProvider.dark01).isEqualTo(Color(MAGENTA))
        assertThat(withProvider.light01).isEqualTo(GiniColorPrimitives().light01)
    }

    @Test
    fun `fixed palette equals the defaults without a provider`() {
        GiniBank.setCaptureConfiguration(context, CaptureConfiguration(networkService = mockk(relaxed = true)))
        lateinit var primitives: GiniColorPrimitives
        composeTestRule.setContent { primitives = giniFixedColorPrimitives() }
        composeTestRule.waitForIdle()

        assertThat(primitives).isEqualTo(GiniColorPrimitives())
    }

    private fun configure(provider: CustomResourceProvider) {
        GiniBank.setCaptureConfiguration(
            context,
            CaptureConfiguration(networkService = mockk(relaxed = true), customResourceProvider = provider)
        )
    }

    private fun captureScheme(): GiniColorScheme {
        lateinit var scheme: GiniColorScheme
        composeTestRule.setContent {
            GiniTheme(darkMode = false) {
                scheme = GiniTheme.colorScheme
            }
        }
        composeTestRule.waitForIdle()
        return scheme
    }

    private companion object {
        const val MAGENTA = 0xFFFF00FF.toInt()
    }
}
