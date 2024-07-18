package net.gini.android.bank.sdk.capture.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.components.switcher.GiniSwitchColors
import net.gini.android.capture.ui.components.textinput.GiniTextInputColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class SkontoSectionColors(
    val titleTextColor: Color,
    val switchColors: GiniSwitchColors,
    val cardBackgroundColor: Color,
    val enabledHintTextColor: Color,
    val infoBannerColors: InfoBannerColors,
    val amountFieldColors: GiniTextInputColors,
    val dueDateTextFieldColor: GiniTextInputColors,
) {

    companion object {

        @Composable
        fun colors(
            titleTextColor: Color = GiniTheme.colorScheme.text.primary,
            switchColors: GiniSwitchColors = GiniSwitchColors.colors(),
            cardBackgroundColor: Color = GiniTheme.colorScheme.card.container,
            enabledHintTextColor: Color = GiniTheme.colorScheme.text.success,
            infoBannerColors: InfoBannerColors = InfoBannerColors.colors(),
            amountFieldColors: GiniTextInputColors = GiniTextInputColors.colors(),
            dueDateTextFieldColor: GiniTextInputColors = GiniTextInputColors.colors(),
        ) = SkontoSectionColors(
            titleTextColor = titleTextColor,
            switchColors = switchColors,
            cardBackgroundColor = cardBackgroundColor,
            enabledHintTextColor = enabledHintTextColor,
            infoBannerColors = infoBannerColors,
            amountFieldColors = amountFieldColors,
            dueDateTextFieldColor = dueDateTextFieldColor
        )
    }

    @Immutable
    data class InfoBannerColors(
        val backgroundColor: Color,
        val textColor: Color,
        val iconTint: Color,
    ) {
        companion object {
            @Composable
            fun colors(
                backgroundColor: Color = GiniTheme.colorScheme.card.containerSuccess,
                textColor: Color = GiniTheme.colorScheme.card.contentSuccess,
                iconTint: Color = GiniTheme.colorScheme.card.contentSuccess,
            ) = InfoBannerColors(
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
            )
        }
    }
}