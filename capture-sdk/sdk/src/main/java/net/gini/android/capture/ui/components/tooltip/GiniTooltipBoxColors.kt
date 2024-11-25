package net.gini.android.capture.ui.components.tooltip

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class GiniTooltipBoxColors(
    val containerColor: Color,
    val contentColor: Color,
) {
    companion object {

        @Composable
        fun contentDescriptionColors(
            containerColor: Color = getContentDescriptionContainerColors(),
            contentColor: Color = getContentDescriptionContentColors(),
        ) = GiniTooltipBoxColors(
            containerColor = containerColor,
            contentColor = contentColor,
        )

        @Composable
        private fun getContentDescriptionContainerColors() =
            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.9f) else Color.Black

        @Composable
        private fun getContentDescriptionContentColors() =
            if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.9f) else Color.White
    }
}
