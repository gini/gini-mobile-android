package net.gini.android.bank.sdk.capture.skonto.components.button.filled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.ui.theme.GiniTheme

@Immutable
data class GiniButtonColors(
    val containerColor: Color = Color.Unspecified,
    val contentContent: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.button.surfacePrEnabled,
            contentContent: Color = GiniTheme.colorScheme.button.textEnabled,
        ) = GiniButtonColors(
            containerColor = containerColor,
            contentContent = contentContent,
        )
    }
}