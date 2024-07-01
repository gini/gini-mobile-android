package net.gini.android.capture.ui.components.switcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class GiniSwitchColors(
    val checkedTrackColor: Color,
    val checkedThumbColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedThumbColor: Color,
) {
    companion object {

        @Composable
        fun colors(
            checkedTrackColor: Color = GiniTheme.colorScheme.toggles.surfaceFocused,
            checkedThumbColor: Color = Color.White,
            uncheckedTrackColor: Color = GiniTheme.colorScheme.toggles.surfaceUnfocused,
            uncheckedThumbColor: Color = GiniTheme.colorScheme.toggles.thumbUnfocused
        ) = GiniSwitchColors(
            checkedTrackColor = checkedTrackColor,
            checkedThumbColor = checkedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedThumbColor = uncheckedThumbColor,
        )
    }
}