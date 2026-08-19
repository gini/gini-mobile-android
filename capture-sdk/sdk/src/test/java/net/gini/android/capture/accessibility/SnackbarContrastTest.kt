package net.gini.android.capture.accessibility

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.Locale
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Internal use only
 *
 * Verifies that the snackbar keeps enough contrast between its container and its labels, in light
 * and in dark mode.
 *
 * These assert the contrast *ratio* rather than concrete colour values on purpose: the requirement
 * is WCAG 2.2 AAA, so a future palette change that silently drops below the threshold
 * has to fail here instead of shipping.
 *
 * Each mode is a separate test with its own `@Config`, so a failure in one mode still leaves the
 * other reported rather than hiding it.
 */
@RunWith(RobolectricTestRunner::class)
class SnackbarContrastTest {

    @Test
    @Config(qualifiers = "notnight")
    fun `snackbar action label meets AAA contrast in light mode`() {
        assertActionLabelContrast(night = false)
    }

    @Test
    @Config(qualifiers = "night")
    fun `snackbar action label meets AAA contrast in dark mode`() {
        assertActionLabelContrast(night = true)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `snackbar message text meets AAA contrast in light mode`() {
        assertMessageTextContrast(night = false)
    }

    @Test
    @Config(qualifiers = "night")
    fun `snackbar message text meets AAA contrast in dark mode`() {
        assertMessageTextContrast(night = true)
    }

    // region assertions

    private fun assertActionLabelContrast(night: Boolean) {
        val context = themedContext(night)
        val container = styleColor(context, snackbarStyle, backgroundTintAttr)
        val actionLabel = styleColor(context, snackbarButtonStyle, textColorAttr)

        assertContrastAtLeastAaa(actionLabel, container, "snackbar action label", night)
    }

    private fun assertMessageTextContrast(night: Boolean) {
        val context = themedContext(night)
        val container = styleColor(context, snackbarStyle, backgroundTintAttr)
        val messageText = styleColor(context, snackbarTextViewStyle, textColorAttr)

        assertContrastAtLeastAaa(messageText, container, "snackbar message text", night)
    }

    private fun assertContrastAtLeastAaa(
        foreground: Int,
        background: Int,
        what: String,
        night: Boolean
    ) {
        val ratio = ColorUtils.calculateContrast(foreground, background)
        assertWithMessage(
            "$what in ${modeName(night)} mode: ${hex(foreground)} on ${hex(background)} is only " +
                String.format(Locale.US, "%.2f", ratio) + ":1"
        ).that(ratio).isAtLeast(AAA_NORMAL_TEXT_RATIO)
    }

    // endregion

    // region helpers

    /**
     * Builds the themed context and checks that the `@Config` qualifier actually reached the theme.
     * `colorOnBackground` is one of the attributes `values-night` re-points, so without this guard a
     * qualifier that silently failed to apply would test light mode twice and still pass.
     */
    private fun themedContext(night: Boolean): Context {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.GiniCaptureTheme
        )
        val expected = context.getColor(
            if (night) R.color.gc_light_01 else R.color.gc_dark_02
        )
        val onBackground = TypedValue()
        assertThat(
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnBackground,
                onBackground,
                true
            )
        ).isTrue()
        assertWithMessage("the ${modeName(night)} qualifier was not applied to the theme")
            .that(onBackground.data)
            .isEqualTo(expected)
        return context
    }

    /** Resolves a style pointed at by a theme attribute, so the theme wiring is covered too. */
    private fun styleColor(context: Context, themeAttr: Int, attr: Int): Int {
        val styleRes = context.styleFor(themeAttr)
        val attributes = context.obtainStyledAttributes(styleRes, intArrayOf(attr))
        try {
            val colors = attributes.getColorStateList(0)
            assertThat(colors).isNotNull()
            return colors!!.defaultColor
        } finally {
            attributes.recycle()
        }
    }

    private fun Context.styleFor(themeAttr: Int): Int {
        val value = TypedValue()
        val resolved = theme.resolveAttribute(themeAttr, value, true)
        assertThat(resolved).isTrue()
        return if (value.resourceId != 0) value.resourceId else value.data
    }

    private fun modeName(night: Boolean) = if (night) "dark" else "light"

    private fun hex(color: Int) = String.format(Locale.US, "#%06X", color and 0xFFFFFF)

    // endregion

    private val backgroundTintAttr = com.google.android.material.R.attr.backgroundTint
    private val textColorAttr = android.R.attr.textColor
    private val snackbarStyle = com.google.android.material.R.attr.snackbarStyle
    private val snackbarButtonStyle = com.google.android.material.R.attr.snackbarButtonStyle
    private val snackbarTextViewStyle = com.google.android.material.R.attr.snackbarTextViewStyle

    private companion object {
        /** WCAG 2.2 AAA minimum for normal text. The labels are 14sp, so this is the threshold. */
        const val AAA_NORMAL_TEXT_RATIO = 7.0
    }
}
