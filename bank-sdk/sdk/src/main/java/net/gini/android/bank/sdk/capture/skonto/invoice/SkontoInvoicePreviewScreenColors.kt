package net.gini.android.bank.sdk.capture.skonto.invoice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.colors.GiniColorPrimitives

@Immutable
data class SkontoInvoicePreviewScreenColors(
    val background: Color,
    val closeButton: CloseButton,
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
            // IMPORTANT! Use GiniColorPrimitives carefully! Using of this class skips adaptation to light/dark modes!
            background: Color = GiniColorPrimitives().dark01,
        ) = SkontoInvoicePreviewScreenColors(
            background = background,
            closeButton = CloseButton.colors(),
        )
    }
}
