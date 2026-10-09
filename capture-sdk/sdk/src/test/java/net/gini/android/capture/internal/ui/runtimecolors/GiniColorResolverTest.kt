package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [GiniColorResolver]: provider lookup, fallback to resources, dark mode,
 * caching, error handling and theme attribute resolution.
 */
@RunWith(RobolectricTestRunner::class)
class GiniColorResolverTest {

    private lateinit var context: Context
    private lateinit var themedContext: Context

    private val calls = mutableListOf<Pair<String, Boolean>>()

    private fun recordingProvider(answer: (String, Boolean) -> Int?) =
        CustomResourceProvider { name, isDarkMode ->
            calls += name to isDarkMode
            answer(name, isDarkMode)
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        themedContext = ContextThemeWrapper(context, R.style.GiniCaptureTheme)
    }

    @Test
    fun `without a provider the resolver is inactive and returns the resource color`() {
        val resolver = GiniColorResolver(null)

        assertThat(resolver.isActive).isFalse()
        assertThat(resolver.color(context, R.color.gc_dark_05))
            .isEqualTo(context.getColor(R.color.gc_dark_05))
    }

    @Test
    fun `provider color is used for a palette color in light mode`() {
        val resolver = GiniColorResolver(recordingProvider { _, _ -> GREEN })

        assertThat(resolver.isActive).isTrue()
        assertThat(resolver.color(context, R.color.gc_dark_05)).isEqualTo(GREEN)
        assertThat(calls).containsExactly("gc_dark_05" to false)
    }

    @Test
    fun `null from the provider falls back to the resource color`() {
        val resolver = GiniColorResolver(recordingProvider { _, _ -> null })

        assertThat(resolver.color(context, R.color.gc_accent_01))
            .isEqualTo(context.getColor(R.color.gc_accent_01))
    }

    @Test
    fun `a throwing provider falls back to the resource color and does not rethrow`() {
        val resolver = GiniColorResolver(CustomResourceProvider { _, _ -> error("broken provider") })

        assertThat(resolver.color(context, R.color.gc_error_02))
            .isEqualTo(context.getColor(R.color.gc_error_02))
    }

    @Test
    @Config(qualifiers = "night")
    fun `in dark mode the provider is asked with isDarkMode true`() {
        val resolver = GiniColorResolver(recordingProvider { _, isDarkMode -> if (isDarkMode) GREEN else BLUE })

        assertThat(resolver.color(context, R.color.gc_dark_05)).isEqualTo(GREEN)
        assertThat(calls).containsExactly("gc_dark_05" to true)
    }

    @Test
    fun `the provider is asked only once per name and mode, also when it returns null`() {
        val resolver = GiniColorResolver(recordingProvider { name, _ -> if (name == "gc_dark_05") GREEN else null })

        repeat(3) {
            resolver.color(context, R.color.gc_dark_05)
            resolver.color(context, R.color.gc_light_01)
        }

        assertThat(calls).containsExactly("gc_dark_05" to false, "gc_light_01" to false)
    }

    @Test
    fun `colors outside the palette never reach the provider`() {
        val resolver = GiniColorResolver(recordingProvider { _, _ -> GREEN })

        assertThat(resolver.color(context, R.color.gc_accent_06))
            .isEqualTo(context.getColor(R.color.gc_accent_06))
        assertThat(resolver.color(context, R.color.gc_camera_preview_shade))
            .isEqualTo(context.getColor(R.color.gc_camera_preview_shade))
        assertThat(calls).isEmpty()
    }

    @Test
    fun `a theme attribute is traced back to its palette color in light mode`() {
        val resolver = GiniColorResolver(recordingProvider { _, _ -> BLUE })

        assertThat(resolver.colorFromAttr(themedContext, com.google.android.material.R.attr.backgroundColor))
            .isEqualTo(BLUE)
        assertThat(calls).containsExactly("gc_light_02" to false)
    }

    @Test
    @Config(qualifiers = "night")
    fun `a theme attribute is traced back to its night palette color in dark mode`() {
        val resolver = GiniColorResolver(recordingProvider { _, _ -> BLUE })

        assertThat(resolver.colorFromAttr(themedContext, com.google.android.material.R.attr.backgroundColor))
            .isEqualTo(BLUE)
        assertThat(calls).containsExactly("gc_dark_01" to true)
    }

    @Test
    fun `a theme attribute that does not point to a palette color returns null`() {
        val resolver = GiniColorResolver(recordingProvider { _, _ -> BLUE })

        assertThat(resolver.colorFromAttr(themedContext, android.R.attr.colorControlHighlight)).isNull()
        assertThat(calls).isEmpty()
    }

    private companion object {
        const val GREEN = 0xFF00FF00.toInt()
        const val BLUE = 0xFF0000FF.toInt()
    }
}
