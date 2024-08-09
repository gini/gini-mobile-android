package net.gini.android.bank.sdk.capture.skonto.help.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.capture.skonto.help.colors.section.SkontoHelpDescriptionSectionColors
import net.gini.android.bank.sdk.capture.skonto.help.colors.section.SkontoHelpFooterSectionColors
import net.gini.android.bank.sdk.capture.skonto.help.colors.section.SkontoHelpItemsSectionColors
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class SkontoHelpScreenColors(
    val backgroundColor: Color,
    val topAppBarColors: GiniTopBarColors,
    val skontoHelpDescriptionSectionColors: SkontoHelpDescriptionSectionColors,
    val skontoHelpItemsSectionColors: SkontoHelpItemsSectionColors,
    val skontoHelpFooterSectionColors: SkontoHelpFooterSectionColors,
) {

    companion object {
        @Composable
        fun colors(
            backgroundColor: Color = GiniTheme.colorScheme.background.primary,
            topAppBarColors: GiniTopBarColors = GiniTopBarColors.colors(),
            skontoHelpDescriptionSectionColors: SkontoHelpDescriptionSectionColors = SkontoHelpDescriptionSectionColors.colors(),
            skontoHelpItemsSectionColors: SkontoHelpItemsSectionColors = SkontoHelpItemsSectionColors.colors(),
            skontoHelpFooterSectionColors: SkontoHelpFooterSectionColors = SkontoHelpFooterSectionColors.colors(),
        ) = SkontoHelpScreenColors(
            backgroundColor = backgroundColor,
            topAppBarColors = topAppBarColors,
            skontoHelpDescriptionSectionColors = skontoHelpDescriptionSectionColors,
            skontoHelpItemsSectionColors = skontoHelpItemsSectionColors,
            skontoHelpFooterSectionColors = SkontoHelpFooterSectionColors.colors(),

        )
    }
}