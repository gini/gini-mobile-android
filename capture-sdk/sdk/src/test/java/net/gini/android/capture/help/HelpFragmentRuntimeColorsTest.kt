package net.gini.android.capture.help

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import android.os.Looper

/**
 * Tests that an SDK screen and its list items use the [CustomResourceProvider] colors.
 */
@RunWith(AndroidJUnit4::class)
class HelpFragmentRuntimeColorsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `screen background and list items use provider colors`() {
        configure { name, _ -> PALETTE[name] }

        launchFragmentInContainer<HelpFragment>().use { scenario ->
            shadowOf(Looper.getMainLooper()).idle()
            scenario.onFragment { fragment ->
                val root = fragment.requireView()
                assertThat(backgroundColor(root)).isEqualTo(LIGHT_02)
                val firstItem = root.findViewById<RecyclerView>(R.id.gc_help_items).getChildAt(0)
                assertThat(backgroundColor(firstItem)).isEqualTo(LIGHT_01)
            }
        }
    }

    @Test
    fun `without a provider the screen keeps its resource colors`() {
        configure(null)

        launchFragmentInContainer<HelpFragment>().use { scenario ->
            scenario.onFragment { fragment ->
                assertThat(backgroundColor(fragment.requireView())).isEqualTo(context.getColor(R.color.gc_light_02))
            }
        }
    }

    private fun configure(provider: CustomResourceProvider?) {
        val builder = GiniCapture.newInstance(context).setGiniCaptureNetworkService(mockk(relaxed = true))
        provider?.let { builder.setCustomResourceProvider(it) }
        builder.build()
    }

    private fun backgroundColor(view: View): Int = (view.background as ColorDrawable).color

    private companion object {
        const val LIGHT_01 = 0xFF00BB22.toInt()
        const val LIGHT_02 = 0xFF00CC33.toInt()
        val PALETTE = mapOf("gc_light_01" to LIGHT_01, "gc_light_02" to LIGHT_02)
    }
}
