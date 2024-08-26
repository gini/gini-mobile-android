package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class DigitalInvoiceSkontoFooterSectionColors(
    val cardBackgroundColor: Color,
) {

    companion object {

        @Composable
        fun colors(
            cardBackgroundColor: Color = GiniTheme.colorScheme.card.container,
        ) = DigitalInvoiceSkontoFooterSectionColors(
            cardBackgroundColor = cardBackgroundColor,
        )
    }
}