package net.gini.android.bank.sdk.transactiondocs.ui.extractions.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.components.menu.context.GiniContextMenuColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class TransactionDocsWidgetColors(
    val titleColor: Color,
    val containerColor: Color,
    val documentItemColors: DocumentItemColors,
) {

    @Immutable
    data class DocumentItemColors(
        val documentIconPlaceholderColors: IconPlaceholderColors,
        val textColor: Color,
        val moreIconTint: Color,
        val menuColors: GiniContextMenuColors,
    ) {

        @Immutable
        data class IconPlaceholderColors(
            val iconBackgroundColor: Color,
            val iconTint: Color,
        ) {
            companion object {
                @Composable
                fun colors(
                    iconBackgroundColor: Color = GiniTheme.colorScheme.placeholder.background,
                    iconTint: Color = GiniTheme.colorScheme.placeholder.tint,
                ) = IconPlaceholderColors(
                    iconBackgroundColor = iconBackgroundColor,
                    iconTint = iconTint,
                )
            }
        }

        companion object {
            @Composable
            fun colors(
                documentIconPlaceholderColors: IconPlaceholderColors = IconPlaceholderColors.colors(),
                textColor: Color = GiniTheme.colorScheme.text.primary,
                moreIconTint: Color = GiniTheme.colorScheme.icons.secondary,
                menuColors: GiniContextMenuColors = GiniContextMenuColors.colors(),
            ) = DocumentItemColors(
                documentIconPlaceholderColors = documentIconPlaceholderColors,
                textColor = textColor,
                moreIconTint = moreIconTint,
                menuColors = menuColors,
            )
        }
    }


    companion object {

        @Composable
        fun colors(
            titleColor: Color = GiniTheme.colorScheme.text.primary,
            containerColor: Color = Color.Transparent,
            documentItemColors: DocumentItemColors = DocumentItemColors.colors(),
        ) = TransactionDocsWidgetColors(
            containerColor = containerColor,
            documentItemColors = documentItemColors,
            titleColor = titleColor
        )
    }
}
