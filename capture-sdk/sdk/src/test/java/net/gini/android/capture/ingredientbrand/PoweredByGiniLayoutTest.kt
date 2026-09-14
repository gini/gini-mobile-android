package net.gini.android.capture.ingredientbrand

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the Gini ingredient brand element — the "Powered by gini" badge shown on the
 * Analysis screen — against the approved Figma component "Powered by Gini" (35631:1800).
 */
@RunWith(RobolectricTestRunner::class)
class PoweredByGiniLayoutTest {

    private fun inflateBadge(): View {
        val themedContext = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.GiniCaptureTheme
        )
        return LayoutInflater.from(themedContext)
            .inflate(R.layout.gc_powered_by_gini, null)
    }

    @Test
    fun `renders the powered by label`() {
        val label = inflateBadge().findViewById<TextView>(R.id.gc_powered_by_gini_label)

        assertThat(label.text.toString())
            .isEqualTo(label.context.getString(R.string.gc_powered_by_gini_label))
    }

    /**
     * The label sits on a pill that is white in both themes, so its colour must not follow
     * ?attr/colorOnBackground — it is pinned to the Figma token color/text/secondary.
     */
    @Test
    fun `label uses the fixed brand label colour and size`() {
        val badge = inflateBadge()
        val label = badge.findViewById<TextView>(R.id.gc_powered_by_gini_label)

        assertThat(label.currentTextColor)
            .isEqualTo(badge.context.getColor(R.color.gc_powered_by_gini_label))
        assertThat(label.textSize)
            .isEqualTo(badge.resources.displayMetrics.scaledDensity * 10f)
        assertThat(label.letterSpacing).isEqualTo(-0.05f)
    }

    @Test
    fun `has the pill background`() {
        assertThat(inflateBadge().background).isNotNull()
    }

    @Test
    fun `renders the gini logo at the size of the brand lockup`() {
        val badge = inflateBadge()
        val logo = badge.findViewById<ImageView>(R.id.gc_powered_by_gini_logo)

        assertThat(logo.drawable).isNotNull()
        assertThat(logo.layoutParams.width)
            .isEqualTo(badge.resources.getDimensionPixelSize(R.dimen.gc_powered_by_gini_logo_width))
        assertThat(logo.layoutParams.height)
            .isEqualTo(badge.resources.getDimensionPixelSize(R.dimen.gc_powered_by_gini_logo_height))
    }

    /**
     * The logo carries the Gini brand colour itself, so it must not be re-tinted by the theme.
     */
    @Test
    fun `does not tint the gini logo`() {
        val logo = inflateBadge().findViewById<ImageView>(R.id.gc_powered_by_gini_logo)

        assertThat(logo.imageTintList).isNull()
    }

    /**
     * The badge must reach TalkBack as one node: the container carries the description and is
     * screen-reader focusable, while neither child contributes anything of its own.
     *
     * This asserts the view-level contract rather than the produced
     * [android.view.accessibility.AccessibilityNodeInfo], because Robolectric returns an
     * unpopulated node for a detached view — `getContentDescription()` on it is null even after
     * `onInitializeAccessibilityNodeInfo`. Verifying the merged node itself needs an
     * instrumented test; see the spec's "Not tested" section.
     */
    @Test
    fun `is announced as a single element`() {
        val badge = inflateBadge()

        assertThat(badge.contentDescription.toString())
            .isEqualTo(badge.context.getString(R.string.gc_powered_by_gini_content_description))
        assertThat(badge.importantForAccessibility)
            .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
        assertThat(badge.isScreenReaderFocusable).isTrue()
        assertThat(badge.isFocusable).isFalse()
        listOf(R.id.gc_powered_by_gini_label, R.id.gc_powered_by_gini_logo).forEach { id ->
            assertThat(badge.findViewById<View>(id).importantForAccessibility)
                .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        }
    }
}
