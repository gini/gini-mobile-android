package net.gini.android.bank.sdk.capture.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class SkontoInvoiceScanSectionColors(
    val cardBackgroundColor: Color,
    val titleTextColor: Color,
    val subtitleTextColor: Color,
    val iconBackgroundColor: Color,
    val iconTint: Color,
    val arrowTint: Color,
) {
    companion object {

        @Composable
        fun colors(
            cardBackgroundColor: Color = GiniTheme.colorScheme.background.surface,
            titleTextColor: Color = GiniTheme.colorScheme.text.primary,
            subtitleTextColor: Color = GiniTheme.colorScheme.text.secondary,
            iconBackgroundColor: Color = GiniTheme.colorScheme.icons.surfaceFilled,
            iconTint: Color = GiniTheme.colorScheme.icons.trailing,
            arrowTint: Color = GiniTheme.colorScheme.icons.trailing,
        ) = SkontoInvoiceScanSectionColors(
            cardBackgroundColor = cardBackgroundColor,
            titleTextColor = titleTextColor,
            subtitleTextColor = subtitleTextColor,
            iconBackgroundColor = iconBackgroundColor,
            iconTint = iconTint,
            arrowTint = arrowTint,
        )
    }
}