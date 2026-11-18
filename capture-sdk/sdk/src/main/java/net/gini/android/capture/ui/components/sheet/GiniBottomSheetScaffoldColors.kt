package net.gini.android.capture.ui.components.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.colors.GiniSheetColors

/**
 * Colors of [GiniBottomSheetScaffold].
 *
 * @property containerColor background of the sheet surface
 * @property scrimColor the dim layer behind the sheet
 */
@Immutable
data class GiniBottomSheetScaffoldColors(
    val containerColor: Color = Color.Unspecified,
    val scrimColor: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = GiniSheetColors.container,
            scrimColor: Color = GiniSheetColors.scrim,
        ) = GiniBottomSheetScaffoldColors(
            containerColor = containerColor,
            scrimColor = scrimColor,
        )
    }
}

