package net.gini.android.bank.sdk.capture.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.components.button.filled.GiniButtonColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class SkontoFooterSectionColors(
    val cardBackgroundColor: Color,
    val titleTextColor: Color,
    val amountTextColor: Color,
    val discountLabelColorScheme: DiscountLabelColorScheme,
    val continueButtonColors: GiniButtonColors,
) {

    companion object {

        @Composable
        fun colors(
            cardBackgroundColor: Color = GiniTheme.colorScheme.background.surface,
            titleTextColor: Color = GiniTheme.colorScheme.text.primary,
            amountTextColor: Color = GiniTheme.colorScheme.text.primary,
            discountLabelColorScheme: DiscountLabelColorScheme = DiscountLabelColorScheme.colors(),
            continueButtonColors: GiniButtonColors = GiniButtonColors.colors(),
        ) = SkontoFooterSectionColors(
            cardBackgroundColor = cardBackgroundColor,
            titleTextColor = titleTextColor,
            amountTextColor = amountTextColor,
            discountLabelColorScheme = discountLabelColorScheme,
            continueButtonColors = continueButtonColors,
        )
    }

    @Immutable
    data class DiscountLabelColorScheme(
        val backgroundColor: Color,
        val textColor: Color,
    ) {
        companion object {

            @Composable
            fun colors(
                backgroundColor: Color = GiniTheme.colorScheme.chips.suggestionEnabled,
                textColor: Color = GiniTheme.colorScheme.chips.textSuggestionEnabled,
            ) = DiscountLabelColorScheme(
                backgroundColor = backgroundColor,
                textColor = textColor,
            )
        }
    }
}