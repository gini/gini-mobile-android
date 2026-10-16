package net.gini.android.capture.internal.ui.runtimecolors

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper

/**
 * A themed context whose [LayoutInflater] applies the [GiniColorResolver] colors, so that views
 * inflated later with `LayoutInflater.from(view.context)` (list items, view stubs, snackbars)
 * get the runtime colors too.
 */
internal class RuntimeColorsContextThemeWrapper(
    base: Context,
    @StyleRes themeRes: Int,
    private val resolver: GiniColorResolver,
) : ContextThemeWrapper(base, themeRes) {

    private var inflater: LayoutInflater? = null

    override fun getSystemService(name: String): Any? {
        if (name == Context.LAYOUT_INFLATER_SERVICE) {
            return inflater ?: RuntimeColorsLayoutInflater(LayoutInflater.from(baseContext), this, resolver)
                .also { inflater = it }
        }
        return super.getSystemService(name)
    }
}
