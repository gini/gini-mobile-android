package net.gini.android.capture.ui.components.menu.context.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class GiniContextMenuColors(
    val containerColor: Color,
    val borderColor: Color,
    val itemColors: ItemColors,
) {

    @Immutable
    data class ItemColors(
        val textColor: Color,
        val iconTint: Color,
    ) {
        companion object {
            @Composable
            fun colors(
                textColor: Color = GiniTheme.colorScheme.text.primary,
                iconTint: Color = GiniTheme.colorScheme.text.primary,
            ) = ItemColors(
                textColor = textColor,
                iconTint = iconTint,
            )
        }
    }

    companion object {

        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.contextMenu.container,
            borderColor: Color = GiniTheme.colorScheme.contextMenu.borderColor,
            itemColors: ItemColors = ItemColors.colors(),
        ) = GiniContextMenuColors(
            containerColor = containerColor,
            borderColor = borderColor,
            itemColors = itemColors,
        )
    }
}
