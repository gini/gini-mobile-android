package net.gini.android.bank.sdk.capture.skonto.help.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class SkontoHelpDescriptionSectionColors(
    val backgroundColor: Color,
    val titleTextColor: Color,
    val descriptionTextColor: Color,
) {

    companion object {

        @Composable
        fun colors(
            backgroundColor: Color = GiniTheme.colorScheme.card.container,
            titleTextColor: Color = GiniTheme.colorScheme.text.primary,
            descriptionTextColor: Color = GiniTheme.colorScheme.text.secondary,
        ) = SkontoHelpDescriptionSectionColors(
            backgroundColor = backgroundColor,
            titleTextColor = titleTextColor,
            descriptionTextColor = descriptionTextColor,
        )
    }

}