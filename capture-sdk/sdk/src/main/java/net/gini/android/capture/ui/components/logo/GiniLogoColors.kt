package net.gini.android.capture.ui.components.logo

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

data class GiniLogoColors(
    val textColor: Color,
    val logoTint: Color,
) {

    companion object {

        @Composable
        fun colors(
            textColor: Color = GiniTheme.colorScheme.text.secondary,
            logoTint: Color = GiniTheme.colorScheme.logo.tint,
        ) = GiniLogoColors(
            textColor = textColor,
            logoTint = logoTint
        )
    }
}
