package net.gini.android.capture.ui.components.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of [GiniInfoCard].
 *
 * @property containerColor background of the card
 */
@Immutable
data class GiniInfoCardColors(
    val containerColor: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = GiniTheme.colorScheme.placeholder.background,
        ) = GiniInfoCardColors(
            containerColor = containerColor,
        )
    }
}
