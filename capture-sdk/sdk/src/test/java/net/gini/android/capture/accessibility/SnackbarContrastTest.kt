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
import org.robolectric.RuntimeEnvironment

/**
 * Internal use only
 *
 * Verifies that the snackbar keeps enough contrast between its container and its labels, in light
 * and in dark mode.
 *
 * These assert the contrast *ratio* rather than concrete colour values on purpose: the requirement
 * is WCAG 2.2 AAA (see PP-2330), so a future palette change that silently drops below the threshold
 * has to fail here instead of shipping.
 */
@RunWith(RobolectricTestRunner::class)
class SnackbarContrastTest {

    @Test
    fun `snackbar action label meets AAA contrast in both modes`() {
        forEachMode { context ->
            val container = styleColor(context, snackbarStyle, backgroundTintAttr)
            val actionLabel = styleColor(context, snackbarButtonStyle, textColorAttr)

            assertContrastAtLeastAaa(actionLabel, container, "snackbar action label")
        }
    }

    @Test
    fun `snackbar message text meets AAA contrast in both modes`() {
        forEachMode { context ->
            val container = styleColor(context, snackbarStyle, backgroundTintAttr)
            val messageText = styleColor(context, snackbarTextViewStyle, textColorAttr)

            assertContrastAtLeastAaa(messageText, container, "snackbar message text")
        }
    }

    // region helpers

    /**
     * Runs [assertions] once in light mode and once in dark mode. The themed context is rebuilt per
     * mode so that any `values-night` override is picked up.
     */
    private fun forEachMode(assertions: (Context) -> Unit) {
        listOf("notnight", "night").forEach { qualifier ->
            RuntimeEnvironment.setQualifiers("+$qualifier")
            currentMode = qualifier
            val context = ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.GiniCaptureTheme
            )
            assertQualifierApplied(context)
            assertions(context)
        }
    }

    /**
     * Guards the loop itself. `colorOnBackground` is one of the attributes that `values-night`
     * re-points, so if the qualifier switch ever stops working these tests would silently check
     * light mode twice instead of failing.
     */
    private fun assertQualifierApplied(context: Context) {
        val expected = context.getColor(
            if (currentMode == "night") R.color.gc_light_01 else R.color.gc_dark_02
        )
        val onBackground = TypedValue()
        assertThat(
            context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnBackground,
                onBackground,
                true
            )
        ).isTrue()
        assertWithMessage("$currentMode qualifier was not applied to the theme")
            .that(onBackground.data)
            .isEqualTo(expected)
    }

    private fun assertContrastAtLeastAaa(foreground: Int, background: Int, what: String) {
        val ratio = ColorUtils.calculateContrast(foreground, background)
        assertWithMessage(
            "$what in $currentMode mode: ${hex(foreground)} on ${hex(background)} is only " +
                String.format(Locale.US, "%.2f", ratio) + ":1"
        ).that(ratio).isAtLeast(AAA_NORMAL_TEXT_RATIO)
    }

    /** Resolves a style pointed at by a theme attribute, so the theme wiring is covered too. */
    private fun Context.styleFor(themeAttr: Int): Int {
        val value = TypedValue()
        val resolved = theme.resolveAttribute(themeAttr, value, true)
        assertThat(resolved).isTrue()
        return if (value.resourceId != 0) value.resourceId else value.data
    }

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

    private fun hex(color: Int) = String.format(Locale.US, "#%06X", color and 0xFFFFFF)

    // endregion

    private var currentMode = "notnight"

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
