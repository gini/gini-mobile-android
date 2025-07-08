package net.gini.android.capture.ui.components

import net.gini.android.capture.ui.components.button.filled.GiniPrimaryButtonStyleConfig

/**
 * <p>
 *     Use this class to override different styles of widgets provided by the Gini SDK.
 *     You can override the text in the strings.xml file!
 * </p>
 */
data class GiniComposableStyleProviderConfig(
    val primaryButtonStyle: GiniPrimaryButtonStyleConfig? = null
)
