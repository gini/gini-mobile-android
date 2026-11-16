package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableContainer
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.Xml
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * Applies provider colors to drawables inflated from XML: the `<solid>` and `<stroke>` colors
 * of `<shape>` drawables, also inside `<layer-list>` and `<selector>` drawables. Other drawable
 * types are left unchanged.
 */
internal object RuntimeDrawables {

    /**
     * Applies provider colors to [drawable], which must be the mutated drawable inflated from
     * [drawableRes]. Returns `true` when a color was changed.
     */
    fun apply(
        context: Context,
        resolver: GiniColorResolver,
        drawable: Drawable,
        @DrawableRes drawableRes: Int,
    ): Boolean {
        val node = readTree(context, drawableRes) ?: return false
        return apply(context, resolver, node, drawable)
    }

    private fun apply(context: Context, resolver: GiniColorResolver, node: Node, drawable: Drawable?): Boolean =
        when (node.name) {
            "shape" -> (drawable as? GradientDrawable)
                ?.let { applyShape(context, resolver, node, it) }
                ?: false
            "layer-list" -> (drawable as? LayerDrawable)
                ?.let { layers -> applyItems(context, resolver, node, layers.numberOfLayers, layers::getDrawable) }
                ?: false
            "selector" -> ((drawable as? DrawableContainer)?.constantState as? DrawableContainer.DrawableContainerState)
                ?.let { state -> applyItems(context, resolver, node, state.childCount, state::getChild) }
                ?: false
            else -> false
        }

    private fun applyItems(
        context: Context,
        resolver: GiniColorResolver,
        node: Node,
        childCount: Int,
        childAt: (Int) -> Drawable?,
    ): Boolean {
        var changed = false
        node.children.filter { it.name == "item" }.forEachIndexed { index, item ->
            val child = (if (index < childCount) childAt(index) else null) ?: return@forEachIndexed
            changed = when {
                item.drawableRes != 0 -> apply(context, resolver, child, item.drawableRes)
                else -> item.children.firstOrNull()?.let { apply(context, resolver, it, child) } ?: false
            } || changed
        }
        return changed
    }

    private fun applyShape(
        context: Context,
        resolver: GiniColorResolver,
        node: Node,
        shape: GradientDrawable,
    ): Boolean {
        val solid = node.paletteChild("solid", context, resolver)
        val stroke = node.paletteChild("stroke", context, resolver)
        solid?.let { shape.setColor(resolver.color(context, it.colorRes)) }
        stroke?.let {
            val color = resolver.color(context, it.colorRes)
            if (it.dashWidth == 0f) {
                shape.setStroke(it.strokeWidth, color)
            } else {
                shape.setStroke(it.strokeWidth, color, it.dashWidth, it.dashGap)
            }
        }
        return solid != null || stroke != null
    }

    private fun Node.paletteChild(name: String, context: Context, resolver: GiniColorResolver): Node? =
        children.firstOrNull { it.name == name }?.takeIf { resolver.isPaletteColor(context, it.colorRes) }

    private class Node(
        val name: String,
        val colorRes: Int,
        val drawableRes: Int,
        val strokeWidth: Int,
        val dashWidth: Float,
        val dashGap: Float,
    ) {
        val children = mutableListOf<Node>()
    }

    private fun readTree(context: Context, @DrawableRes drawableRes: Int): Node? {
        val parser = openXml(context.resources, drawableRes) ?: return null
        return try {
            if (!moveToRoot(parser)) null else readNode(context, parser)
        } catch (ignored: XmlPullParserException) {
            null
        } catch (ignored: IOException) {
            null
        } finally {
            parser.close()
        }
    }

    /** Reads the element at the parser position and its children; leaves the parser on its end tag. */
    private fun readNode(context: Context, parser: XmlResourceParser): Node {
        val node = createNode(context, parser)
        val depth = parser.depth
        while (true) {
            val type = parser.next()
            if (type == XmlPullParser.END_DOCUMENT || (type == XmlPullParser.END_TAG && parser.depth <= depth)) break
            if (type == XmlPullParser.START_TAG) node.children += readNode(context, parser)
        }
        return node
    }

    private fun createNode(context: Context, parser: XmlResourceParser): Node {
        val attrs = Xml.asAttributeSet(parser)
        val array = context.obtainStyledAttributes(attrs, NODE_ATTRS)
        try {
            return Node(
                name = parser.name,
                colorRes = array.getResourceId(NODE_ATTRS.indexOf(android.R.attr.color), 0),
                drawableRes = array.getResourceId(NODE_ATTRS.indexOf(android.R.attr.drawable), 0),
                strokeWidth = array.getDimensionPixelSize(NODE_ATTRS.indexOf(android.R.attr.width), 0),
                dashWidth = array.getDimension(NODE_ATTRS.indexOf(android.R.attr.dashWidth), 0f),
                dashGap = array.getDimension(NODE_ATTRS.indexOf(android.R.attr.dashGap), 0f),
            )
        } finally {
            array.recycle()
        }
    }

    // obtainStyledAttributes needs the attribute ids in ascending order.
    @AttrRes
    private val NODE_ATTRS: IntArray = intArrayOf(
        android.R.attr.color,
        android.R.attr.drawable,
        android.R.attr.width,
        android.R.attr.dashWidth,
        android.R.attr.dashGap,
    ).sortedArray()
}
