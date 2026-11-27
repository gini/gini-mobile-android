package net.gini.android.capture.ui.components.expandable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.LocalGiniExtendedColors

/**
 * Colors of [GiniExpandableInfoRow].
 *
 * @property containerColor background of the row
 * @property iconTint tint of the leading icon
 * @property titleColor color of the title
 * @property bodyColor color of the expanded body text
 * @property chevronTint tint of the expand/collapse chevron
 */
@Immutable
data class GiniExpandableInfoRowColors(
    val containerColor: Color = Color.Unspecified,
    val iconTint: Color = Color.Unspecified,
    val titleColor: Color = Color.Unspecified,
    val bodyColor: Color = Color.Unspecified,
    val chevronTint: Color = Color.Unspecified,
) {

    companion object {
        @Composable
        fun colors(
            containerColor: Color = LocalGiniExtendedColors.current.infoContainer,
            iconTint: Color = GiniTheme.colorScheme.text.accent,
            titleColor: Color = GiniTheme.colorScheme.text.primary,
            bodyColor: Color = GiniTheme.colorScheme.text.secondary,
            chevronTint: Color = GiniTheme.colorScheme.text.primary,
        ) = GiniExpandableInfoRowColors(
            containerColor = containerColor,
            iconTint = iconTint,
            titleColor = titleColor,
            bodyColor = bodyColor,
            chevronTint = chevronTint,
        )
    }
}
