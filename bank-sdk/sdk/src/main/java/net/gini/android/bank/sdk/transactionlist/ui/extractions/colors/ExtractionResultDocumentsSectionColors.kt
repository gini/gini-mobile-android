package net.gini.android.bank.sdk.transactionlist.ui.extractions.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class ExtractionResultDocumentsSectionColors(
    val documentItemColors: DocumentItemColors,
) {

    @Immutable
    data class DocumentItemColors(
        val documentIconPlaceholderColors: IconPlaceholderColors,
        val textColor: Color,
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
            ) = DocumentItemColors(
                documentIconPlaceholderColors = documentIconPlaceholderColors,
                textColor = textColor,
            )
        }
    }


    companion object {

        @Composable
        fun colors(
            documentItemColors: DocumentItemColors = DocumentItemColors.colors(),
        ) = ExtractionResultDocumentsSectionColors(
            documentItemColors = documentItemColors,
        )
    }
}
