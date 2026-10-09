package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.snackbar.Snackbar
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the code-level helpers in `RuntimeColors.kt`.
 */
@RunWith(RobolectricTestRunner::class)
class RuntimeColorsTest {

    private val context: Context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.GiniCaptureTheme)

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `giniColor returns the provider color when a provider is set`() {
        configure { name, _ -> if (name == "gc_accent_01") ACCENT else null }

        assertThat(context.giniColor(R.color.gc_accent_01)).isEqualTo(ACCENT)
        assertThat(context.giniColorFromAttr(androidx.appcompat.R.attr.colorPrimary)).isEqualTo(ACCENT)
    }

    @Test
    fun `without a provider the helpers behave like the plain Android calls`() {
        configure(null)

        assertThat(context.giniColor(R.color.gc_accent_01)).isEqualTo(context.getColor(R.color.gc_accent_01))
        assertThat(context.giniColorFromAttr(androidx.appcompat.R.attr.colorPrimary)).isNull()
        val view = View(context).apply { setGiniBackgroundResource(R.drawable.gc_qr_code_warning_background) }
        assertThat((view.background as GradientDrawable).color!!.defaultColor)
            .isEqualTo(context.getColor(R.color.gc_warning_02))
    }

    @Test
    fun `giniDrawable and setGiniBackgroundResource apply provider colors to XML shapes`() {
        configure { name, _ -> if (name == "gc_warning_02") WARNING else null }

        val drawable = context.giniDrawable(R.drawable.gc_qr_code_warning_background) as GradientDrawable
        val view = View(context).apply { setGiniBackgroundResource(R.drawable.gc_qr_code_warning_background) }

        assertThat(drawable.color!!.defaultColor).isEqualTo(WARNING)
        assertThat((view.background as GradientDrawable).color!!.defaultColor).isEqualTo(WARNING)
    }

    @Test
    fun `snackbar colors come from the snackbar styles with provider colors`() {
        configure { name, _ -> SNACKBAR_PALETTE[name] }
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java)
            .also { it.get().setTheme(R.style.GiniCaptureTheme) }
            .setup().get()
        val anchor = FrameLayout(activity).also { activity.setContentView(it) }

        val snackbar = Snackbar.make(anchor, "text", Snackbar.LENGTH_SHORT).applyGiniRuntimeColors(activity)

        // The snackbar layout keeps its background tint privately (not in View.backgroundTintList);
        // the background was verified on a device. The text color is observable here.
        val text = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        assertThat(text.currentTextColor).isEqualTo(LIGHT_02)
    }

    private fun configure(provider: CustomResourceProvider?) {
        val builder = GiniCapture.newInstance(context).setGiniCaptureNetworkService(mockk(relaxed = true))
        provider?.let { builder.setCustomResourceProvider(it) }
        builder.build()
    }

    private companion object {
        const val ACCENT = 0xFFAA0011.toInt()
        const val WARNING = 0xFF00BB22.toInt()
        const val DARK_03 = 0xFF3300CC.toInt() // must not leak into the text color
        const val LIGHT_02 = 0xFF00CC33.toInt()
        val SNACKBAR_PALETTE = mapOf("gc_dark_03" to DARK_03, "gc_light_02" to LIGHT_02)
    }
}
