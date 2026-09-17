package net.gini.android.capture.ingredientbrand

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies the Gini ingredient brand element — the "Powered by gini" badge — against the approved
 * Figma component "Powered by Gini" (35631:1800).
 *
 * The badge is one vector drawable rather than a composed layout, so what matters is that the
 * drawable is the one attached, that it renders at the component's size, and that it paints the
 * component's colours. The values are asserted as literals: they live in path data precisely so
 * that no colour, dimension, style or string resource stands between an integrator and the badge.
 */
@RunWith(RobolectricTestRunner::class)
// NATIVE so the vector is really rasterised: in the default legacy mode Canvas draws are no-ops
// and every pixel would read back transparent, making the paint assertions below meaningless.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PoweredByGiniLayoutTest {

    private fun inflateBadge(): ImageView {
        val themedContext = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.GiniCaptureTheme
        )
        return LayoutInflater.from(themedContext)
            .inflate(R.layout.gc_powered_by_gini, null) as ImageView
    }

    /** Draws the badge at its intrinsic size so the painted result can be sampled. */
    private fun renderBadge(): Bitmap {
        val drawable = inflateBadge().drawable
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    @Test
    fun `badge is the generated brand vector`() {
        val badge = inflateBadge()

        assertThat(badge.drawable).isNotNull()
        assertThat(org.robolectric.Shadows.shadowOf(badge.drawable).createdFromResId)
            .isEqualTo(R.drawable.gc_powered_by_gini_badge)
    }

    /**
     * 90x24dp is the Figma component's frame. The badge is sized by the drawable rather than by
     * layout dimens, so the intrinsic size is the contract.
     */
    @Test
    fun `renders at the size of the brand lockup`() {
        val badge = inflateBadge()
        val density = badge.resources.displayMetrics.density

        assertThat(badge.drawable.intrinsicWidth).isEqualTo((90 * density).toInt())
        assertThat(badge.drawable.intrinsicHeight).isEqualTo((24 * density).toInt())
    }

    /**
     * The pill has to be opaque white: the badge sits on light analysis screens and over the dark
     * camera preview, and it is what keeps the dark label readable on both.
     */
    @Test
    fun `paints a white pill`() {
        val bitmap = renderBadge()

        // Well inside the pill, clear of the wordmark and the logo.
        val pixel = bitmap.getPixel(bitmap.width / 2, 2)
        assertThat(Color.alpha(pixel)).isEqualTo(255)
        assertThat(pixel).isEqualTo(Color.WHITE)
    }

    /** The corners are rounded, so the very corner pixel falls outside the pill. */
    @Test
    fun `pill corners are rounded`() {
        val bitmap = renderBadge()

        assertThat(Color.alpha(bitmap.getPixel(0, 0))).isEqualTo(0)
    }

    /**
     * The badge must reach TalkBack as one node announced "Powered by Gini". It is a single
     * ImageView, so there are no children to exclude.
     */
    @Test
    fun `is announced as a single element`() {
        val badge = inflateBadge()

        assertThat(badge.contentDescription.toString()).isEqualTo("Powered by Gini")
        assertThat(badge.importantForAccessibility)
            .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
        assertThat(badge.isFocusable).isFalse()
    }
}
