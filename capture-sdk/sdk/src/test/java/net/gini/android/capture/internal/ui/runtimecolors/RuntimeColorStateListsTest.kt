package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.graphics.Color
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [RuntimeColorStateLists]: selectors are rebuilt with provider colors, keeping
 * their states and alpha.
 */
@RunWith(RobolectricTestRunner::class)
class RuntimeColorStateListsTest {

    private val context: Context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.GiniCaptureTheme)

    private val provider = CustomResourceProvider { name, _ -> PALETTE[name] }

    @Test
    fun `switch track selector uses provider colors for checked and unchecked`() {
        val colors = RuntimeColorStateLists.rebuild(context, GiniColorResolver(provider), R.color.gc_switch_track_color)!!

        assertThat(colors.getColorForState(intArrayOf(android.R.attr.state_checked), 0)).isEqualTo(ACCENT)
        assertThat(colors.getColorForState(intArrayOf(-android.R.attr.state_checked), 0)).isEqualTo(LIGHT)
    }

    @Test
    fun `material button background keeps its disabled state and alpha`() {
        val buttonContext = ContextThemeWrapper(context, com.google.android.material.R.style.ThemeOverlay_Material3_Button)

        val colors = RuntimeColorStateLists.rebuild(
            buttonContext,
            GiniColorResolver(provider),
            com.google.android.material.R.color.m3_button_background_color_selector,
        )!!

        assertThat(colors.defaultColor).isEqualTo(ACCENT)
        val disabled = colors.getColorForState(intArrayOf(-android.R.attr.state_enabled), 0)
        assertThat(disabled and 0x00FFFFFF).isEqualTo(DARK_02 and 0x00FFFFFF)
        assertThat(Color.alpha(disabled)).isLessThan(255)
    }

    @Test
    fun `without provider values the rebuilt selector equals the original`() {
        val resolver = GiniColorResolver { _, _ -> null }
        val original = context.getColorStateList(R.color.gc_switch_thumb_color)

        val colors = RuntimeColorStateLists.rebuild(context, resolver, R.color.gc_switch_thumb_color)!!

        listOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)).forEach { state ->
            assertThat(colors.getColorForState(state, 0)).isEqualTo(original.getColorForState(state, 0))
        }
    }

    @Test
    fun `a plain color resource is not a selector and returns null`() {
        assertThat(RuntimeColorStateLists.rebuild(context, GiniColorResolver(provider), R.color.gc_accent_01)).isNull()
    }

    private companion object {
        const val ACCENT = 0xFFAA0011.toInt()
        const val LIGHT = 0xFF00BB22.toInt()
        const val DARK_02 = 0xFF3300CC.toInt()
        val PALETTE = mapOf("gc_accent_01" to ACCENT, "gc_light_01" to LIGHT, "gc_dark_02" to DARK_02)
    }
}
