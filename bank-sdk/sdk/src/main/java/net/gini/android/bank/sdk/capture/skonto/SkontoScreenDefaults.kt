package net.gini.android.bank.sdk.capture.skonto

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import net.gini.android.bank.sdk.capture.skonto.colors.SkontoScreenColorScheme
import net.gini.android.capture.ui.components.button.filled.GiniButtonColors
import net.gini.android.capture.ui.components.switcher.GiniSwitchColors
import net.gini.android.capture.ui.theme.GiniTheme

object SkontoScreenDefaults {

    @Composable
    fun textFieldColors() = with(GiniTheme.colorScheme.textField) {
        TextFieldDefaults.colors()
    }

    @Composable
    fun colors() = with(GiniTheme.colorScheme) {
        SkontoScreenColorScheme(
            backgroundColor = background.background,
            // Top App Bar
            topAppBarColors = SkontoScreenColorScheme.TopAppbarColorScheme(
                contentColor = text.primary,
                backgroundColor = background.bar,
            ),
            // Invoice Scan
            invoiceScanSectionColors = SkontoScreenColorScheme.InvoiceScanSectionColorScheme(
                cardBackgroundColor = background.surface,
                titleTextColor = text.primary,
                subtitleTextColor = text.secondary,
                iconBackgroundColor = icons.surfaceFilled,
                iconTint = icons.trailing,
                arrowTint = icons.trailing,
            ),
            // Discount
            discountSectionColors = SkontoScreenColorScheme.SkontoSectionColorScheme(
                titleTextColor = text.primary,
                cardBackgroundColor = background.surface,
                enabledHintTextColor = text.status,

                infoBannerColorScheme = SkontoScreenColorScheme.SkontoSectionColorScheme.InfoBannerColorScheme(
                    backgroundColor = chips.assistEnabled,
                    textColor = chips.suggestionEnabled,
                    iconTint = chips.suggestionEnabled,
                ),
                amountFieldColors = textFieldColors(),
                dueDateTextFieldColor = textFieldColors(),
                switchColors = GiniSwitchColors.colors(),
            ),
            // Without Discount
            withoutDiscountSectionColors = SkontoScreenColorScheme.WithoutDiscountSectionColorScheme(
                titleTextColor = text.primary,
                cardBackgroundColor = background.surface,
                enabledHintTextColor = text.status,
                amountFieldColors = textFieldColors(),
            ),
            // Footer
            footerSectionColorScheme = SkontoScreenColorScheme.FooterSectionColorScheme(
                titleTextColor = text.primary,
                amountTextColor = text.primary,
                cardBackgroundColor = background.surface,

                discountLabelColorScheme = SkontoScreenColorScheme.FooterSectionColorScheme.DiscountLabelColorScheme(
                    backgroundColor = chips.suggestionEnabled,
                    textColor = chips.textSuggestionEnabled,
                ),
                continueButtonColors = GiniButtonColors(
                    containerColor = button.surfacePrEnabled,
                    contentContent = button.textEnabled
                ),
            )
        )
    }
}