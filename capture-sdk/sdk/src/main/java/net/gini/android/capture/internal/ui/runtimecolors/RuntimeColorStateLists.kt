package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Color
import android.util.AttributeSet
import android.util.Xml
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Rebuilds `<selector>` color resources with the palette colors replaced by the
 * [GiniColorResolver] colors. States and `alpha` are kept, following the same rules as
 * [ColorStateList] inflation.
 */
internal object RuntimeColorStateLists {

    private val NON_STATE_ATTRS = setOf(
        android.R.attr.color,
        android.R.attr.alpha,
        android.R.attr.lStar,
        androidx.core.R.attr.alpha,
        androidx.core.R.attr.lStar,
    )

    /**
     * Returns [colorStateListRes] rebuilt with provider colors, or `null` when it is not a
     * selector, when none of its items uses a palette color, or when an item uses `lStar`.
     * [context] must carry the theme the selector is resolved against.
     */
    fun rebuild(
        context: Context,
        resolver: GiniColorResolver,
        @ColorRes colorStateListRes: Int,
    ): ColorStateList? {
        val parser = openXml(context.resources, colorStateListRes) ?: return null
        return try {
            readSelector(context, resolver, parser)
        } catch (ignored: XmlPullParserException) {
            null
        } catch (ignored: IOException) {
            null
        } finally {
            parser.close()
        }
    }

    private class Item(val states: IntArray, val color: Int, val fromPalette: Boolean)

    private fun readSelector(
        context: Context,
        resolver: GiniColorResolver,
        parser: XmlResourceParser,
    ): ColorStateList? {
        val items = if (moveToRoot(parser) && parser.name == "selector") readItems(context, resolver, parser) else null
        return items
            ?.takeIf { list -> list.any { it.fromPalette } }
            ?.let { list -> ColorStateList(list.map { it.states }.toTypedArray(), list.map { it.color }.toIntArray()) }
    }

    /** Reads the `<item>` children of the selector, or returns `null` when one can't be rebuilt. */
    private fun readItems(context: Context, resolver: GiniColorResolver, parser: XmlResourceParser): List<Item>? {
        val items = mutableListOf<Item?>()
        forEachChildElement(parser) {
            if (parser.name == "item") items += readItem(context, resolver, parser)
        }
        return items.takeIf { null !in it }?.filterNotNull()
    }

    private fun readItem(context: Context, resolver: GiniColorResolver, parser: XmlResourceParser): Item? {
        val attrs = Xml.asAttributeSet(parser)
        val usesLStar = hasAttr(context, attrs, android.R.attr.lStar) ||
            hasAttr(context, attrs, androidx.core.R.attr.lStar)
        val color = if (usesLStar) null else readItemColor(context, resolver, attrs)
        return color?.let { (value, fromPalette) -> Item(readStates(parser), value, fromPalette) }
    }

    /** Returns the item color (alpha applied) and whether it came from the palette. */
    private fun readItemColor(
        context: Context,
        resolver: GiniColorResolver,
        attrs: AttributeSet,
    ): Pair<Int, Boolean>? {
        val colorArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.color))
        val (baseColor, fromPalette) = try {
            if (!colorArray.hasValue(0)) return null
            val resId = colorArray.getResourceId(0, 0)
            if (resId != 0 && resolver.isPaletteColor(context, resId)) {
                resolver.color(context, resId) to true
            } else {
                colorArray.getColor(0, Color.TRANSPARENT) to false
            }
        } finally {
            colorArray.recycle()
        }
        val alpha = readFloat(context, attrs, android.R.attr.alpha)
            ?: readFloat(context, attrs, androidx.core.R.attr.alpha)
            ?: 1f
        return modulateAlpha(baseColor, alpha) to fromPalette
    }

    private fun readStates(parser: XmlResourceParser): IntArray {
        val states = mutableListOf<Int>()
        for (i in 0 until parser.attributeCount) {
            val nameRes = parser.getAttributeNameResource(i)
            if (nameRes == 0 || nameRes in NON_STATE_ATTRS) continue
            states += if (parser.getAttributeBooleanValue(i, false)) nameRes else -nameRes
        }
        return states.toIntArray()
    }

    private fun readFloat(context: Context, attrs: AttributeSet, @AttrRes attr: Int): Float? {
        val array = context.obtainStyledAttributes(attrs, intArrayOf(attr))
        return try {
            if (array.hasValue(0)) array.getFloat(0, 1f) else null
        } finally {
            array.recycle()
        }
    }

    private fun hasAttr(context: Context, attrs: AttributeSet, @AttrRes attr: Int): Boolean {
        val array = context.obtainStyledAttributes(attrs, intArrayOf(attr))
        return try {
            array.hasValue(0)
        } finally {
            array.recycle()
        }
    }

    @Suppress("MagicNumber")
    private fun modulateAlpha(color: Int, alpha: Float): Int {
        if (alpha == 1f) return color
        val newAlpha = (Color.alpha(color) * alpha).roundToInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (newAlpha shl 24)
    }
}

/** Opens the XML of a file-based resource, or returns `null` for value resources and missing ids. */
internal fun openXml(resources: Resources, resId: Int): XmlResourceParser? =
    try {
        resources.getXml(resId)
    } catch (ignored: Resources.NotFoundException) {
        null
    }

/** Moves [parser] to the root start tag. Returns `false` when there is none. */
internal fun moveToRoot(parser: XmlPullParser): Boolean {
    var type = parser.next()
    while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        type = parser.next()
    }
    return type == XmlPullParser.START_TAG
}

/** Calls [onChild] with the parser on each direct child start tag of the element it is on now. */
internal fun forEachChildElement(parser: XmlPullParser, onChild: () -> Unit) {
    val depth = parser.depth
    var type = parser.next()
    while (type != XmlPullParser.END_DOCUMENT && !(type == XmlPullParser.END_TAG && parser.depth <= depth)) {
        if (type == XmlPullParser.START_TAG && parser.depth == depth + 1) onChild()
        type = parser.next()
    }
}
