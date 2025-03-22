package net.gini.android.capture.ui.components.button.outlined

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class GiniOutlinedButtonColors(
    val container: Color = Color.Unspecified,
    val content: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.buttonOutlined.container,
            contentContent: Color = GiniTheme.colorScheme.buttonOutlined.content,
        ) = GiniOutlinedButtonColors(
            container = containerColor,
            content = contentContent,
        )
    }
}
