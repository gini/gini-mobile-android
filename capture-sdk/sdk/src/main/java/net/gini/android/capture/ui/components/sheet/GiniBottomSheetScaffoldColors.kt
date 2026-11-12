package net.gini.android.capture.ui.components.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of [GiniBottomSheetScaffold].
 *
 * @property containerColor background of the sheet surface
 */
@Immutable
data class GiniBottomSheetScaffoldColors(
    val containerColor: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.dialogs.container,
        ) = GiniBottomSheetScaffoldColors(
            containerColor = containerColor,
        )
    }
}
