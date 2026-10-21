package net.gini.android.bank.sdk.capture.digitalinvoice

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.divider.MaterialDivider
import io.mockk.mockk
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.util.wrappedWithGiniCaptureTheme
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that bank-sdk XML screens, themed with [wrappedWithGiniCaptureTheme], use the
 * [CaptureConfiguration.customResourceProvider] colors.
 */
@RunWith(AndroidJUnit4::class)
class DigitalInvoiceHelpRuntimeColorsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        GiniBank.cleanupCapture(context)
    }

    @Test
    fun `digital invoice help screen and its items use provider colors`() {
        configure { name, _ -> PALETTE[name] }
        val inflater = LayoutInflater.from(context.wrappedWithGiniCaptureTheme())

        val screen = inflater.inflate(R.layout.gbs_fragment_digital_invoice_help, null, false)
        val item = inflater.inflate(R.layout.gbs_item_help, null, false)

        assertEquals(LIGHT_02, backgroundColor(screen))
        assertEquals(LIGHT_02, backgroundColor(item))
    }

    @Test
    fun `edit item bottom sheet dividers use the provider color`() {
        configure { name, _ -> PALETTE[name] }

        val sheet = LayoutInflater.from(context.wrappedWithGiniCaptureTheme())
            .inflate(R.layout.gbs_edit_item_bottom_sheet, null, false)

        assertEquals(ACCENT_01, sheet.findViewById<MaterialDivider>(R.id.gbs_article_name_divider).dividerColor)
    }

    @Test
    fun `without a provider the screen keeps its resource colors`() {
        configure(null)

        val screen = LayoutInflater.from(context.wrappedWithGiniCaptureTheme())
            .inflate(R.layout.gbs_fragment_digital_invoice_help, null, false)

        assertEquals(context.getColor(net.gini.android.capture.R.color.gc_light_02), backgroundColor(screen))
    }

    private fun configure(provider: CustomResourceProvider?) {
        GiniBank.setCaptureConfiguration(
            context,
            CaptureConfiguration(networkService = mockk(relaxed = true), customResourceProvider = provider)
        )
    }

    private fun backgroundColor(view: View): Int = (view.background as ColorDrawable).color

    private companion object {
        const val LIGHT_02 = 0xFF00CC33.toInt()
        const val ACCENT_01 = 0xFFAA0011.toInt()
        val PALETTE = mapOf("gc_light_02" to LIGHT_02, "gc_accent_01" to ACCENT_01)
    }
}
