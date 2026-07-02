package net.gini.android.bank.sdk.exampleapp.ui.color

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources

/**
 * A [ContextWrapper] installed as the activity's BASE context (via `attachBaseContext`) so that its
 * overridden [getResources] shadows the base resources for the whole context chain.
 *
 * `Context.getColor(id)` / `getColorStateList(id)` are final, but they are implemented as
 * `getResources().getColor(id, theme)` — so overriding [getResources] here is sufficient: every
 * derived context (fragment / Compose `ContextThemeWrapper`) resolves resources down to this base,
 * and therefore reads our [ColorOverrideResources].
 */
class ColorOverrideContextWrapper(
    base: Context,
    private val overrides: Map<Int, Int>,
) : ContextWrapper(base) {

    private val wrappedResources: Resources by lazy {
        ColorOverrideResources(super.getResources(), overrides)
    }

    override fun getResources(): Resources = wrappedResources
}
