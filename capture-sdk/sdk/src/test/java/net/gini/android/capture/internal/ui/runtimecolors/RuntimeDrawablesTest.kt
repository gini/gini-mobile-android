package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [RuntimeDrawables]: shape colors inside XML drawables use provider colors.
 */
@RunWith(RobolectricTestRunner::class)
class RuntimeDrawablesTest {

    private val context: Context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.GiniCaptureTheme)

    private val resolver = GiniColorResolver(CustomResourceProvider { name, _ -> PALETTE[name] })

    @Test
    fun `shape solid color uses the provider color`() {
        val drawable = context.getDrawable(R.drawable.gc_qr_code_warning_background)!!.mutate() as GradientDrawable

        val changed = RuntimeDrawables.apply(context, resolver, drawable, R.drawable.gc_qr_code_warning_background)

        assertThat(changed).isTrue()
        assertThat(drawable.color!!.defaultColor).isEqualTo(WARNING_02)
    }

    @Test
    fun `shape solid from a theme attribute uses the provider color`() {
        val drawable = context.getDrawable(R.drawable.gc_photo_thumbnail_badge_background)!!.mutate() as GradientDrawable

        RuntimeDrawables.apply(context, resolver, drawable, R.drawable.gc_photo_thumbnail_badge_background)

        assertThat(drawable.color!!.defaultColor).isEqualTo(ACCENT)
    }

    @Test
    fun `layer-list shapes use the provider color`() {
        val drawable = context.getDrawable(R.drawable.gc_bg_warning_circle)!!.mutate() as LayerDrawable

        RuntimeDrawables.apply(context, resolver, drawable, R.drawable.gc_bg_warning_circle)

        assertThat((drawable.getDrawable(0) as GradientDrawable).color!!.defaultColor).isEqualTo(WARNING_04)
    }

    @Test
    fun `a vector drawable is not changed`() {
        val drawable = context.getDrawable(R.drawable.gc_add_page)!!.mutate()

        assertThat(RuntimeDrawables.apply(context, resolver, drawable, R.drawable.gc_add_page)).isFalse()
    }

    private companion object {
        const val ACCENT = 0xFFAA0011.toInt()
        const val WARNING_02 = 0xFF00BB22.toInt()
        const val WARNING_04 = 0xFF3300CC.toInt()
        val PALETTE = mapOf("gc_accent_01" to ACCENT, "gc_warning_02" to WARNING_02, "gc_warning_04" to WARNING_04)
    }
}
