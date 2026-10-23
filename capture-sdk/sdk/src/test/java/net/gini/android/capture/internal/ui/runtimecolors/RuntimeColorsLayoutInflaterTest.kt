package net.gini.android.capture.internal.ui.runtimecolors

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tests that XML inflated through [RuntimeColorsLayoutInflater] uses the provider colors.
 */
@RunWith(RobolectricTestRunner::class)
class RuntimeColorsLayoutInflaterTest {

    private lateinit var activity: AppCompatActivity

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.GiniCaptureTheme)
        activity = controller.create().get()
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `without a provider no runtime colors inflater or context is created`() {
        configure(null)

        assertThat(runtimeColorsThemedInflaterOrNull(activity.layoutInflater, activity, R.style.GiniCaptureTheme)).isNull()
        assertThat(runtimeColorsThemedContextOrNull(activity, R.style.GiniCaptureTheme)).isNull()
    }

    @Test
    fun `layout colors from color resources, theme attributes and views without id use provider colors`() {
        configure { name, _ -> PALETTE[name] }

        val root = inflate(R.layout.gc_fragment_error) as ViewGroup

        assertThat(backgroundColor(root)).isEqualTo(LIGHT_02)
        val stripe = (0 until root.childCount).map { root.getChildAt(it) }.first { it.javaClass == View::class.java }
        assertThat(backgroundColor(stripe)).isEqualTo(ERROR_01)
        assertThat(root.findViewById<TextView>(R.id.gc_error_textview).currentTextColor).isEqualTo(DARK_05)
    }

    @Test
    fun `material buttons use provider colors and keep a translucent disabled state`() {
        configure { name, _ -> PALETTE[name] }

        val root = inflate(R.layout.gc_fragment_error)

        val primary = root.findViewById<MaterialButton>(R.id.gc_button_error_retake_images)
        assertThat(primary.backgroundTintList!!.defaultColor).isEqualTo(ACCENT_01)
        val disabled = primary.backgroundTintList!!.getColorForState(intArrayOf(-android.R.attr.state_enabled), 0)
        assertThat(Color.alpha(disabled)).isLessThan(255)
        val outlined = root.findViewById<MaterialButton>(R.id.gc_button_error_enter_manually)
        assertThat(outlined.strokeColor!!.defaultColor).isEqualTo(LIGHT_03)
    }

    @Test
    fun `views inflated later from the themed context use provider colors`() {
        configure { name, _ -> PALETTE[name] }
        val themedContext = runtimeColorsThemedContextOrNull(activity, R.style.GiniCaptureTheme)!!

        val item = LayoutInflater.from(themedContext).inflate(R.layout.gc_item_help, null, false)

        assertThat(backgroundColor(item)).isEqualTo(LIGHT_01)
    }

    @Test
    fun `view stub content uses provider colors`() {
        configure { name, _ -> PALETTE[name] }
        val root = inflate(R.layout.gc_fragment_camera)

        val stubContent = root.findViewById<ViewStub>(R.id.gc_stub_camera_no_permission).inflate()

        assertThat(backgroundColor(stubContent)).isEqualTo(LIGHT_02)
        assertThat(stubContent.findViewById<TextView>(R.id.gc_text_camera_no_permission).currentTextColor)
            .isEqualTo(DARK_05)
    }

    @Test
    fun `provider null keeps the resource colors`() {
        configure { _, _ -> null }

        val root = inflate(R.layout.gc_fragment_error)

        assertThat(root.findViewById<TextView>(R.id.gc_error_textview).currentTextColor)
            .isEqualTo(activity.getColor(R.color.gc_dark_05))
    }

    private fun configure(provider: CustomResourceProvider?) {
        val builder = GiniCapture.newInstance(activity).setGiniCaptureNetworkService(mockk(relaxed = true))
        provider?.let { builder.setCustomResourceProvider(it) }
        builder.build()
    }

    private fun inflate(layoutRes: Int): View =
        runtimeColorsThemedInflaterOrNull(activity.layoutInflater, activity, R.style.GiniCaptureTheme)!!
            .inflate(layoutRes, null, false)

    private fun backgroundColor(view: View): Int = (view.background as ColorDrawable).color

    private companion object {
        const val ACCENT_01 = 0xFFAA0011.toInt()
        const val LIGHT_01 = 0xFF00BB22.toInt()
        const val LIGHT_02 = 0xFF00CC33.toInt()
        const val LIGHT_03 = 0xFF00DD44.toInt()
        const val DARK_05 = 0xFF3300CC.toInt()
        const val ERROR_01 = 0xFF4400DD.toInt()
        val PALETTE = mapOf(
            "gc_accent_01" to ACCENT_01,
            "gc_light_01" to LIGHT_01,
            "gc_light_02" to LIGHT_02,
            "gc_light_03" to LIGHT_03,
            "gc_dark_05" to DARK_05,
            "gc_error_01" to ERROR_01,
        )
    }
}
