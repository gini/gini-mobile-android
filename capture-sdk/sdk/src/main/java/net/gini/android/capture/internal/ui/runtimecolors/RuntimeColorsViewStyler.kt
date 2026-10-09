package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.R as MaterialR
import androidx.appcompat.R as AppCompatR

/**
 * Applies the [GiniColorResolver] colors to one inflated view, based on the color attributes it
 * was inflated with: its layout attributes, its `style`, and the widget's default style.
 *
 * A value is only changed when it references a palette color: directly, through a theme
 * attribute, or inside a `<selector>` / XML drawable.
 */
internal object RuntimeColorsViewStyler {

    private const val SNACKBAR_LAYOUT = "com.google.android.material.snackbar.Snackbar\$SnackbarLayout"

    // obtainStyledAttributes needs the attribute ids in ascending order.
    private val ATTRS: IntArray = intArrayOf(
        android.R.attr.textColor,
        android.R.attr.background,
        android.R.attr.textAppearance,
        android.R.attr.backgroundTint,
        android.R.attr.tint,
        AppCompatR.attr.backgroundTint,
        AppCompatR.attr.tint,
        AppCompatR.attr.thumbTint,
        AppCompatR.attr.trackTint,
        MaterialR.attr.strokeColor,
        MaterialR.attr.dividerColor,
        MaterialR.attr.navigationIconTint,
        MaterialR.attr.trackDecorationTint,
    ).sortedArray()

    private fun index(@AttrRes attr: Int) = ATTRS.indexOf(attr)

    fun apply(view: View, attrs: AttributeSet, resolver: GiniColorResolver) {
        // The view's own context also carries android:theme and Material theme overlays.
        val context = view.context
        val array = context.obtainStyledAttributes(attrs, ATTRS, defaultStyleAttr(view), 0)
        try {
            applyTextColor(view, array, context, resolver)
            applyBackground(view, array, context, resolver)
            applyTints(view, array, context, resolver)
            applyTextCursor(view, context, resolver)
        } finally {
            array.recycle()
        }
    }

    private fun applyTextColor(view: View, array: TypedArray, context: Context, resolver: GiniColorResolver) {
        if (view !is TextView) return
        val colorRes = array.getResourceId(index(android.R.attr.textColor), 0)
            .takeIf { it != 0 }
            ?: textAppearanceColor(context, array.getResourceId(index(android.R.attr.textAppearance), 0))
        colorStateList(context, resolver, colorRes)?.let { view.setTextColor(it) }
    }

    private fun textAppearanceColor(context: Context, appearanceRes: Int): Int {
        if (appearanceRes == 0) return 0
        val appearance = context.obtainStyledAttributes(appearanceRes, intArrayOf(android.R.attr.textColor))
        return try {
            appearance.getResourceId(0, 0)
        } finally {
            appearance.recycle()
        }
    }

    private fun applyBackground(view: View, array: TypedArray, context: Context, resolver: GiniColorResolver) {
        val backgroundRes = array.getResourceId(index(android.R.attr.background), 0)
        if (backgroundRes == 0) return
        when {
            resolver.isPaletteColor(context, backgroundRes) ->
                view.setBackgroundColor(resolver.color(context, backgroundRes))
            context.resources.getResourceTypeName(backgroundRes) == "drawable" ->
                view.background?.mutate()?.let { background ->
                    if (RuntimeDrawables.apply(context, resolver, background, backgroundRes)) view.invalidate()
                }
        }
    }

    /** Cursor and selection handles of text fields follow `colorControlActivated` (API 29+ only). */
    private fun applyTextCursor(view: View, context: Context, resolver: GiniColorResolver) {
        if (view !is EditText || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val color = resolver.colorFromAttr(context, AppCompatR.attr.colorControlActivated) ?: return
        view.textCursorDrawable?.mutate()?.let { it.setTint(color); view.textCursorDrawable = it }
        view.textSelectHandle?.mutate()?.let { it.setTint(color); view.setTextSelectHandle(it) }
        view.textSelectHandleLeft?.mutate()?.let { it.setTint(color); view.setTextSelectHandleLeft(it) }
        view.textSelectHandleRight?.mutate()?.let { it.setTint(color); view.setTextSelectHandleRight(it) }
    }

    /** Reads tint attributes of one view as provider-colored color lists. */
    private class Tints(
        private val array: TypedArray,
        private val context: Context,
        private val resolver: GiniColorResolver,
    ) {
        fun of(vararg attrs: Int): ColorStateList? =
            attrs.asSequence()
                .mapNotNull { attr -> colorStateList(context, resolver, array.getResourceId(index(attr), 0)) }
                .firstOrNull()
    }

    private fun applyTints(view: View, array: TypedArray, context: Context, resolver: GiniColorResolver) {
        val tints = Tints(array, context, resolver)
        tints.of(AppCompatR.attr.backgroundTint, android.R.attr.backgroundTint)?.let {
            ViewCompat.setBackgroundTintList(view, it)
        }
        applyWidgetTints(view, tints)
        applySwitchTints(view, tints)
    }

    private fun applyWidgetTints(view: View, tints: Tints) {
        when (view) {
            is MaterialButton -> tints.of(MaterialR.attr.strokeColor)?.let { view.strokeColor = it }
            is MaterialCardView -> tints.of(MaterialR.attr.strokeColor)?.let { view.setStrokeColor(it) }
            is MaterialDivider -> tints.of(MaterialR.attr.dividerColor)?.let { view.dividerColor = it.defaultColor }
            is Toolbar -> tints.of(MaterialR.attr.navigationIconTint)?.let { navigationTint ->
                view.navigationIcon?.mutate()?.setTintList(navigationTint)
            }
            is ImageView -> tints.of(AppCompatR.attr.tint, android.R.attr.tint)?.let {
                ImageViewCompat.setImageTintList(view, it)
            }
        }
    }

    private fun applySwitchTints(view: View, tints: Tints) {
        if (view is SwitchCompat) {
            tints.of(AppCompatR.attr.thumbTint)?.let { view.thumbTintList = it }
            tints.of(AppCompatR.attr.trackTint)?.let { view.trackTintList = it }
        }
        if (view is MaterialSwitch) {
            tints.of(MaterialR.attr.trackDecorationTint)?.let { view.trackDecorationTintList = it }
        }
    }

    /** A palette color as a single color list, a selector rebuilt with provider colors, or `null`. */
    private fun colorStateList(context: Context, resolver: GiniColorResolver, colorRes: Int): ColorStateList? = when {
        colorRes == 0 -> null
        resolver.isPaletteColor(context, colorRes) -> ColorStateList.valueOf(resolver.color(context, colorRes))
        context.resources.getResourceTypeName(colorRes) == "color" ->
            RuntimeColorStateLists.rebuild(context, resolver, colorRes)
        else -> null
    }

    @AttrRes
    private fun defaultStyleAttr(view: View): Int = when {
        view is MaterialButton -> MaterialR.attr.materialButtonStyle
        view is MaterialSwitch -> MaterialR.attr.materialSwitchStyle
        view is SwitchCompat -> AppCompatR.attr.switchStyle
        view is MaterialCardView -> MaterialR.attr.materialCardViewStyle
        view is MaterialDivider -> MaterialR.attr.materialDividerStyle
        view is Toolbar -> AppCompatR.attr.toolbarStyle
        view.javaClass.name == SNACKBAR_LAYOUT -> MaterialR.attr.snackbarStyle
        else -> 0
    }
}
