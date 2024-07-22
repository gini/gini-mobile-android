package net.gini.android.capture.ui.components.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class GiniTopBarColors(
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
    val navigationContentColor: Color = Color.Unspecified,
    val actionContentColor: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.topAppBar.container,
            contentColor: Color = GiniTheme.colorScheme.text.primary,
            navigationContentColor: Color = GiniTheme.colorScheme.topAppBar.icon.navigation,
            actionContentColor: Color = GiniTheme.colorScheme.topAppBar.icon.action,
        ) = GiniTopBarColors(
            containerColor = containerColor,
            contentColor = contentColor,
            navigationContentColor = navigationContentColor,
            actionContentColor = actionContentColor,
        )
    }
}