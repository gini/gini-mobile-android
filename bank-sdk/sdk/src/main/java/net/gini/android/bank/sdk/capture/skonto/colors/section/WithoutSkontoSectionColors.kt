package net.gini.android.bank.sdk.capture.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.components.textinput.GiniTextInputColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class WithoutSkontoSectionColors(
    val titleTextColor: Color,
    val cardBackgroundColor: Color,
    val amountFieldColors: GiniTextInputColors,
    val enabledHintTextColor: Color,
) {
    companion object {

        @Composable
        fun colors(
            titleTextColor: Color = GiniTheme.colorScheme.text.primary,
            cardBackgroundColor: Color = GiniTheme.colorScheme.card.container,
            amountFieldColors: GiniTextInputColors = GiniTextInputColors.colors(),
            enabledHintTextColor: Color = GiniTheme.colorScheme.text.success,
        ) = WithoutSkontoSectionColors(
            titleTextColor = titleTextColor,
            cardBackgroundColor = cardBackgroundColor,
            amountFieldColors = amountFieldColors,
            enabledHintTextColor = enabledHintTextColor,
        )
    }
}