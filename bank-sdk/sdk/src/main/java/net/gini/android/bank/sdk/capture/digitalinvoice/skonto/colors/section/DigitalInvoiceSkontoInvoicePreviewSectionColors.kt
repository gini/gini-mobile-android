package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class DigitalInvoiceSkontoInvoicePreviewSectionColors(
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
            cardBackgroundColor: Color = GiniTheme.colorScheme.card.container,
            titleTextColor: Color = GiniTheme.colorScheme.text.primary,
            subtitleTextColor: Color = GiniTheme.colorScheme.text.secondary,
            iconBackgroundColor: Color = GiniTheme.colorScheme.placeholder.background,
            iconTint: Color = GiniTheme.colorScheme.placeholder.tint,
            arrowTint: Color = GiniTheme.colorScheme.icons.secondary,
        ) = DigitalInvoiceSkontoInvoicePreviewSectionColors(
            cardBackgroundColor = cardBackgroundColor,
            titleTextColor = titleTextColor,
            subtitleTextColor = subtitleTextColor,
            iconBackgroundColor = iconBackgroundColor,
            iconTint = iconTint,
            arrowTint = arrowTint,
        )
    }
}