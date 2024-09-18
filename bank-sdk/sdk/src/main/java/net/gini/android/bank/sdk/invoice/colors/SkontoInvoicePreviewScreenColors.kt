package net.gini.android.bank.sdk.invoice.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.invoice.colors.section.SkontoInvoicePreviewScreenFooterColors
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives

@Immutable
data class SkontoInvoicePreviewScreenColors(
    val background: Color,
    val topBarColors: GiniTopBarColors,
    val closeButton: CloseButton,
    val footerColors: SkontoInvoicePreviewScreenFooterColors,
) {

    data class CloseButton(
        val contentColor: Color,
        val backgroundColor: Color,
    ) {
        companion object {

            @Composable
            fun colors(
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                contentColor: Color = GiniColorPrimitives().dark02,
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                backgroundColor: Color = GiniColorPrimitives().light01,
            ) = CloseButton(
                contentColor = contentColor,
                backgroundColor = backgroundColor,
            )
        }
    }

    companion object {

        @Composable
        fun colors(
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            background: Color = GiniColorPrimitives().dark01,
            footerColors: SkontoInvoicePreviewScreenFooterColors =
                SkontoInvoicePreviewScreenFooterColors.colors(),
            topBarColors: GiniTopBarColors = GiniTopBarColors.colors(
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                containerColor = GiniColorPrimitives().dark01.copy(alpha = 0.5f),
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                contentColor = GiniColorPrimitives().light01,
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                navigationContentColor = GiniColorPrimitives().light01,
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                actionContentColor = GiniColorPrimitives().light01,
            ),
        ) = SkontoInvoicePreviewScreenColors(
            background = background,
            closeButton = CloseButton.colors(),
            footerColors = footerColors,
            topBarColors = topBarColors,
        )
    }
}
