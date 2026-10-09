package net.gini.android.capture.internal.ui.runtimecolors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives

/**
 * Internal use only.
 *
 * The palette for screens that use the default palette on purpose, without light/dark mode
 * adaptation (for example the always-dark invoice preview). With a
 * [net.gini.android.capture.ui.theme.colors.CustomResourceProvider] it returns the provider's
 * light-mode colors, falling back to the defaults; without one it returns exactly
 * [GiniColorPrimitives] defaults, as before.
 *
 * @suppress
 */
@Composable
fun giniFixedColorPrimitives(): GiniColorPrimitives {
    val context = LocalContext.current
    val resolver = GiniColorResolver.current()?.takeIf { it.isActive }
    return remember(resolver) {
        resolver?.primitives(context, isDarkMode = false, base = GiniColorPrimitives()) ?: GiniColorPrimitives()
    }
}
