package net.gini.android.capture.ui.components.header

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Colors of [GiniHeaderBlock].
 *
 * @property iconContainerColor background of the rounded square behind the icon
 * @property iconTint tint applied to the icon
 * @property titleColor color of the title
 * @property descriptionColor color of the description
 */
@Immutable
data class GiniHeaderBlockColors(
    val iconContainerColor: Color = Color.Unspecified,
    val iconTint: Color = Color.Unspecified,
    val titleColor: Color = Color.Unspecified,
    val descriptionColor: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            iconContainerColor: Color = GiniTheme.colorScheme.placeholder.background,
            iconTint: Color = GiniTheme.colorScheme.text.accent,
            titleColor: Color = GiniTheme.colorScheme.text.primary,
            descriptionColor: Color = GiniTheme.colorScheme.text.secondary,
        ) = GiniHeaderBlockColors(
            iconContainerColor = iconContainerColor,
            iconTint = iconTint,
            titleColor = titleColor,
            descriptionColor = descriptionColor,
        )
    }
}
