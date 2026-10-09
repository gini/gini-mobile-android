package net.gini.android.bank.sdk.invoice.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.invoice.colors.section.InvoicePreviewScreenFooterColors
import net.gini.android.capture.ui.components.menu.context.GiniContextMenuColors
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.internal.ui.runtimecolors.giniFixedColorPrimitives

@Immutable
data class InvoicePreviewScreenColors(
    val background: Color,
    val topBarColors: GiniTopBarColors,
    val topBarOverflowMenuColors: GiniContextMenuColors,
    val closeButton: CloseButton,
    val footerColors: InvoicePreviewScreenFooterColors,
    val errorMessage: ErrorMessage,
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
                contentColor: Color = giniFixedColorPrimitives().dark02,
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                backgroundColor: Color = giniFixedColorPrimitives().light01,
            ) = CloseButton(
                contentColor = contentColor,
                backgroundColor = backgroundColor,
            )
        }
    }

    data class ErrorMessage(
        val messageColor: Color,
        val errorHint: ErrorHint,
    )

    data class ErrorHint(
        val iconColor: Color,
        val textColor: Color,
        val containerColor: Color,
        val containerStrokeColor: Color,
    )

    companion object {

        @Composable
        fun colors(
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            background: Color = giniFixedColorPrimitives().dark01,
            footerColors: InvoicePreviewScreenFooterColors =
                InvoicePreviewScreenFooterColors.colors(),
            topBarColors: GiniTopBarColors = GiniTopBarColors.colors(
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                containerColor = giniFixedColorPrimitives().dark01.copy(alpha = 0.5f),
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                contentColor = giniFixedColorPrimitives().light01,
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                navigationContentColor = giniFixedColorPrimitives().light01,
                // IMPORTANT! Use GiniColorPrimitives carefully!
                // Using of this class skips adaptation to light/dark modes!
                actionContentColor = giniFixedColorPrimitives().light01,
            ),
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            topBarOverflowMenuColors: GiniContextMenuColors = GiniContextMenuColors.colors(
                containerColor = giniFixedColorPrimitives().dark01,
                borderColor = Color.Transparent,
                itemColors = GiniContextMenuColors.ItemColors(
                    textColor = giniFixedColorPrimitives().light01,
                    iconTint = giniFixedColorPrimitives().light01,
                )
            ),
            // IMPORTANT! Use GiniColorPrimitives carefully!
            // Using of this class skips adaptation to light/dark modes!
            errorMessage: ErrorMessage = ErrorMessage(
                messageColor = giniFixedColorPrimitives().light06,
                errorHint = ErrorHint(
                    iconColor = giniFixedColorPrimitives().error01,
                    textColor = giniFixedColorPrimitives().light01,
                    containerColor = giniFixedColorPrimitives().light01.copy(alpha = 0.15f),
                    containerStrokeColor = giniFixedColorPrimitives().error04.copy(alpha = 0.15f),
                )
            ),
        ) = InvoicePreviewScreenColors(
            background = background,
            closeButton = CloseButton.colors(),
            footerColors = footerColors,
            topBarColors = topBarColors,
            topBarOverflowMenuColors = topBarOverflowMenuColors,
            errorMessage = errorMessage
        )
    }
}
