package net.gini.android.bank.sdk.capture.skonto.help.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class SkontoHelpFooterSectionColors(
    val backgroundColor: Color,
    val textColor: Color,
) {

    companion object {

        @Composable
        fun colors(
            backgroundColor: Color = GiniTheme.colorScheme.card.container,
            textColor: Color = GiniTheme.colorScheme.text.secondary,
        ) = SkontoHelpFooterSectionColors(
            backgroundColor = backgroundColor,
            textColor = textColor,
        )
    }

}
