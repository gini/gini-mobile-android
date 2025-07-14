package net.gini.android.capture.ui.components

import androidx.compose.runtime.Composable

/**
 * Implement this interface and pass the composable styles to be used where
 * Gini SDK uses Jetpack Compose.
 *
 */
fun interface GiniComposableStyleProvider {
    @Composable
    fun setGiniComposableStyleProviderConfig(): GiniComposableStyleProviderConfig?
}
