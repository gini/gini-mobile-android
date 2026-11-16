package net.gini.android.bank.sdk.invoice.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.internal.ui.runtimecolors.giniFixedColorPrimitives

data class InvoicePreviewScreenFooterColors(
    val contentColor: Color,
    val backgroundColor: Color,
) {
    companion object {
        @Composable
        fun colors(
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            contentColor: Color = giniFixedColorPrimitives().light01,
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            backgroundColor: Color = giniFixedColorPrimitives().dark01.copy(alpha = 0.5f),
        ) = InvoicePreviewScreenFooterColors(
            contentColor = contentColor,
            backgroundColor = backgroundColor,
        )
    }
}
