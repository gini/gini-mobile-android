package net.gini.android.bank.sdk.exampleapp.ui.color

import android.content.res.ColorStateList
import android.content.res.Resources

/**
 * A [Resources] wrapper that returns override colors for a set of `gc_*` color resource IDs.
 *
 * Installed as a host activity's base context (see [ColorOverrideContextWrapper]) so that every
 * color read resolves through it — both the SDK's Compose theme (which reads colors via
 * `context.getColor(...)`) and legacy XML-view layouts (which reference `@color/gc_*` directly).
 */
class ColorOverrideResources(
    base: Resources,
    private val overrides: Map<Int, Int>,
) : Resources(base.assets, base.displayMetrics, base.configuration) {

    override fun getColor(id: Int, theme: Theme?): Int =
        overrides[id] ?: super.getColor(id, theme)

    @Deprecated("Deprecated in Java")
    override fun getColor(id: Int): Int =
        overrides[id] ?: super.getColor(id)

    override fun getColorStateList(id: Int, theme: Theme?): ColorStateList =
        overrides[id]?.let { ColorStateList.valueOf(it) } ?: super.getColorStateList(id, theme)

    @Deprecated("Deprecated in Java")
    override fun getColorStateList(id: Int): ColorStateList =
        overrides[id]?.let { ColorStateList.valueOf(it) } ?: super.getColorStateList(id)
}
