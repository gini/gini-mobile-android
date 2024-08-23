package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.components.switcher.GiniSwitchColors
import net.gini.android.capture.ui.components.textinput.GiniTextInputColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class DigitalInvoiceSkontoSectionColors(
    val titleTextColor: Color,
    val switchColors: GiniSwitchColors,
    val cardBackgroundColor: Color,
    val enabledHintTextColor: Color,
    val successInfoBannerColors: InfoBannerColors,
    val warningInfoBannerColors: InfoBannerColors,
    val errorInfoBannerColors: InfoBannerColors,
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
            successInfoBannerColors: InfoBannerColors = InfoBannerColors.success(),
            warningInfoBannerColors: InfoBannerColors = InfoBannerColors.warning(),
            errorInfoBannerColors: InfoBannerColors = InfoBannerColors.error(),
            amountFieldColors: GiniTextInputColors = GiniTextInputColors.colors(),
            dueDateTextFieldColor: GiniTextInputColors = GiniTextInputColors.colors(),
        ) = DigitalInvoiceSkontoSectionColors(
            titleTextColor = titleTextColor,
            switchColors = switchColors,
            cardBackgroundColor = cardBackgroundColor,
            enabledHintTextColor = enabledHintTextColor,
            successInfoBannerColors = successInfoBannerColors,
            warningInfoBannerColors = warningInfoBannerColors,
            errorInfoBannerColors = errorInfoBannerColors,
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
            fun success(
                backgroundColor: Color = GiniTheme.colorScheme.card.containerSuccess,
                textColor: Color = GiniTheme.colorScheme.card.contentSuccess,
                iconTint: Color = GiniTheme.colorScheme.card.contentSuccess,
            ) = InfoBannerColors(
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
            )

            @Composable
            fun warning(
                backgroundColor: Color = GiniTheme.colorScheme.card.containerWarning,
                textColor: Color = GiniTheme.colorScheme.card.contentWarning,
                iconTint: Color = GiniTheme.colorScheme.card.contentWarning,
            ) = InfoBannerColors(
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
            )

            @Composable
            fun error(
                backgroundColor: Color = GiniTheme.colorScheme.card.containerError,
                textColor: Color = GiniTheme.colorScheme.card.contentError,
                iconTint: Color = GiniTheme.colorScheme.card.contentError,
            ) = InfoBannerColors(
                backgroundColor = backgroundColor,
                textColor = textColor,
                iconTint = iconTint,
            )
        }
    }
}