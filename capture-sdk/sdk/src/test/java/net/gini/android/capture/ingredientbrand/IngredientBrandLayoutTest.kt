package net.gini.android.capture.ingredientbrand

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that every layout variant of the screens covered by the ingredient brand actually
 * includes the badge, and that it starts hidden.
 *
 * The badge is switched on from the client configuration, so a layout variant that forgot the
 * `<include>` would not fail anywhere else: `findViewById` would return null and the screen
 * would crash only for clients that have ingredient branding enabled. Robolectric qualifiers
 * are used to pick each variant, so adding a new one without the badge fails here.
 *
 * It starting out `GONE` is part of the contract, not an incidental default: on the camera
 * screen the badge belongs to the QR-code analysis step, and `CameraFragmentImpl` is the single
 * writer that may reveal it — the live camera must never show it.
 */
@RunWith(RobolectricTestRunner::class)
class IngredientBrandLayoutTest {

    private fun assertBadgeIsIncludedAndHidden(layoutRes: Int) {
        val themedContext = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.GiniCaptureTheme
        )
        val root = LayoutInflater.from(themedContext).inflate(layoutRes, null)

        // Fails with a NullPointerException if the variant forgot the <include>.
        val badge: View = root.findViewById(R.id.gc_powered_by_gini)
        assertThat(badge.visibility).isEqualTo(View.GONE)

        // The accessibility contract has to survive being included into another layout: an
        // <include> can override attributes of the included root, so it is asserted per variant.
        assertThat(badge.contentDescription.toString())
            .isEqualTo(themedContext.getString(R.string.gc_powered_by_gini_content_description))
        assertThat(badge.isScreenReaderFocusable).isTrue()
        assertThat(badge.isFocusable).isFalse()
        listOf(R.id.gc_powered_by_gini_label, R.id.gc_powered_by_gini_logo).forEach { id ->
            assertThat(badge.findViewById<View>(id).importantForAccessibility)
                .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        }
    }

    @Test
    fun `analysis screen includes the badge on a phone`() {
        assertBadgeIsIncludedAndHidden(R.layout.gc_fragment_analysis)
    }

    @Test
    @Config(qualifiers = "sw600dp")
    fun `analysis screen includes the badge on a tablet`() {
        assertBadgeIsIncludedAndHidden(R.layout.gc_fragment_analysis)
    }

    @Test
    @Config(qualifiers = "sw600dp-land")
    fun `analysis screen includes the badge on a tablet in landscape`() {
        assertBadgeIsIncludedAndHidden(R.layout.gc_fragment_analysis)
    }

    @Test
    fun `camera screen includes the badge on a phone`() {
        assertBadgeIsIncludedAndHidden(R.layout.gc_fragment_camera)
    }

    @Test
    @Config(qualifiers = "land")
    fun `camera screen includes the badge on a phone in landscape`() {
        assertBadgeIsIncludedAndHidden(R.layout.gc_fragment_camera)
    }

    @Test
    @Config(qualifiers = "sw600dp")
    fun `camera screen includes the badge on a tablet`() {
        assertBadgeIsIncludedAndHidden(R.layout.gc_fragment_camera)
    }

    @Test
    @Config(qualifiers = "sw600dp-land")
    fun `camera screen includes the badge on a tablet in landscape`() {
        assertBadgeIsIncludedAndHidden(R.layout.gc_fragment_camera)
    }
}
