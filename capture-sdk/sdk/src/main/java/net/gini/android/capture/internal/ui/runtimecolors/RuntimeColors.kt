@file:JvmName("RuntimeColors")

package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.R as AppCompatR
import com.google.android.material.R as MaterialR

/*
 * Internal use only. Entry points for applying CustomResourceProvider colors to SDK views.
 * Shared with the Gini Bank SDK, hence public in the internal package.
 */

private fun activeResolver(): GiniColorResolver? = GiniColorResolver.current()?.takeIf { it.isActive }

/**
 * Internal use only.
 *
 * Returns a themed context whose inflater applies the runtime colors, or `null` when no
 * [net.gini.android.capture.ui.theme.colors.CustomResourceProvider] is set. Callers then keep
 * their existing context, so nothing changes without a provider.
 *
 * @suppress
 */
fun runtimeColorsThemedContextOrNull(base: Context, @StyleRes themeRes: Int): Context? =
    activeResolver()?.let { RuntimeColorsContextThemeWrapper(base, themeRes, it) }

/**
 * Internal use only.
 *
 * Returns an inflater for [base] themed with [themeRes] that applies the runtime colors, or
 * `null` when no [net.gini.android.capture.ui.theme.colors.CustomResourceProvider] is set.
 *
 * @suppress
 */
fun runtimeColorsThemedInflaterOrNull(
    inflater: LayoutInflater,
    base: Context,
    @StyleRes themeRes: Int,
): LayoutInflater? =
    activeResolver()?.let { resolver ->
        RuntimeColorsLayoutInflater(inflater, RuntimeColorsContextThemeWrapper(base, themeRes, resolver), resolver)
    }

/**
 * Internal use only.
 *
 * The color for [colorRes]: the provider color for palette colors when a provider is set,
 * otherwise the resource color (same as [Context.getColor]).
 *
 * @suppress
 */
@ColorInt
fun Context.giniColor(@ColorRes colorRes: Int): Int =
    activeResolver()?.color(this, colorRes) ?: getColor(colorRes)

/**
 * Internal use only.
 *
 * The provider color for the theme attribute [attr] when a provider is set and the attribute
 * points to a palette color, otherwise `null` (then use the existing theme lookup).
 *
 * @suppress
 */
@ColorInt
fun Context.giniColorFromAttr(@AttrRes attr: Int): Int? =
    activeResolver()?.colorFromAttr(this, attr)

/**
 * Internal use only.
 *
 * The drawable for [drawableRes] with provider colors applied to its XML shapes when a provider
 * is set, otherwise the plain drawable.
 *
 * @suppress
 */
fun Context.giniDrawable(@DrawableRes drawableRes: Int): Drawable? {
    val drawable = getDrawable(drawableRes)
    val resolver = activeResolver()
    if (drawable == null || resolver == null) return drawable
    return drawable.mutate().also { RuntimeDrawables.apply(this, resolver, it, drawableRes) }
}

/**
 * Internal use only.
 *
 * Same as [View.setBackgroundResource], with provider colors applied to the drawable.
 *
 * @suppress
 */
fun View.setGiniBackgroundResource(@DrawableRes drawableRes: Int) {
    val resolver = activeResolver()
    if (resolver == null) {
        setBackgroundResource(drawableRes)
        return
    }
    background = context.giniDrawable(drawableRes)
}

/**
 * Internal use only.
 *
 * Applies the provider colors to a [Snackbar]. Snackbars attach to the activity's content view,
 * so they are not inflated with the SDK's runtime colors inflater. The colors are read from the
 * `snackbarStyle`, `snackbarTextViewStyle` and `snackbarButtonStyle` of [styledContext], so
 * style overrides keep working. Does nothing without a provider.
 *
 * @suppress
 */
fun Snackbar.applyGiniRuntimeColors(styledContext: Context): Snackbar {
    val resolver = activeResolver() ?: return this
    fun styleColor(@AttrRes styleAttr: Int, @AttrRes colorAttr: Int): Int? {
        val array = styledContext.obtainStyledAttributes(null, intArrayOf(colorAttr), styleAttr, 0)
        val colorRes = try {
            array.getResourceId(0, 0)
        } finally {
            array.recycle()
        }
        return colorRes.takeIf { it != 0 && resolver.isPaletteColor(styledContext, it) }
            ?.let { resolver.color(styledContext, it) }
    }
    styleColor(MaterialR.attr.snackbarStyle, AppCompatR.attr.backgroundTint)?.let { setBackgroundTint(it) }
    styleColor(MaterialR.attr.snackbarTextViewStyle, android.R.attr.textColor)?.let { setTextColor(it) }
    styleColor(MaterialR.attr.snackbarButtonStyle, android.R.attr.textColor)?.let { setActionTextColor(it) }
    return this
}
