package net.gini.android.bank.sdk.capture.skonto.invoice.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives

data class SkontoInvoicePreviewScreenFooterColors(
    val contentColor: Color,
    val backgroundColor: Color,
) {
    companion object {
        @Composable
        fun colors(
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            contentColor: Color = GiniColorPrimitives().light01,
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            backgroundColor: Color = GiniColorPrimitives().dark01.copy(alpha = 0.5f),
        ) = SkontoInvoicePreviewScreenFooterColors(
            contentColor = contentColor,
            backgroundColor = backgroundColor,
        )
    }
}
