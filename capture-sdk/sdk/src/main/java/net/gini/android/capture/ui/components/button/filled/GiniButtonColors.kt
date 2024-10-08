package net.gini.android.capture.ui.components.button.filled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class GiniButtonColors(
    val containerColor: Color = Color.Unspecified,
    val contentContent: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.button.container,
            contentContent: Color = GiniTheme.colorScheme.button.content,
        ) = GiniButtonColors(
            containerColor = containerColor,
            contentContent = contentContent,
        )
    }
}