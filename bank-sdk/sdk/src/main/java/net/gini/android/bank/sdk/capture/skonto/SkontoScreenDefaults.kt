package net.gini.android.bank.sdk.capture.skonto

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.ui.theme.GiniTheme

object SkontoScreenDefaults {

    @Composable
    fun textFieldColors() = with(GiniTheme.colorScheme) {
        TextFieldDefaults.colors(
            unfocusedContainerColor = background.inputUnfocused,
            unfocusedLabelColor = text.secondary,
            unfocusedTrailingIconColor = text.secondary,
            unfocusedTextColor = text.primary,
            unfocusedIndicatorColor = background.divider,

            disabledContainerColor = background.inputUnfocused,
            disabledLabelColor = text.secondary,
            disabledTextColor = text.primary,
            disabledIndicatorColor = Color.Transparent,

            focusedTextColor = text.primary,
            focusedContainerColor = background.inputUnfocused,
            focusedIndicatorColor = background.divider,
            focusedLabelColor = text.secondary,
            focusedTrailingIconColor = text.secondary,
        )
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
                iconBackgroundColor = background.buttonFilled,
                iconTint = icons.standardTertiary,
                arrowTint = icons.standardTertiary,
            ),
            // Discount
            discountSectionColors = SkontoScreenColorScheme.DiscountSectionColorScheme(
                titleTextColor = text.primary,
                cardBackgroundColor = background.surface,
                enabledHintTextColor = text.status,

                infoBannerColorScheme = SkontoScreenColorScheme.DiscountSectionColorScheme.InfoBannerColorScheme(
                    backgroundColor = chips.assistEnabled,
                    textColor = chips.suggestionEnabled,
                    iconTint = chips.suggestionEnabled,
                ),
                amountFieldColors = textFieldColors(),
                dueDateTextFieldColor = textFieldColors(),
                switchColors = SwitchDefaults.colors(
                    checkedTrackColor = background.buttonEnabled,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                    uncheckedTrackColor = background.divider,
                    uncheckedThumbColor = background.surface,
                ),
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
                    textColor = text.chipsSuggestionEnabled,
                ),
                continueButtonColors = ButtonDefaults.buttonColors(
                    containerColor = background.buttonEnabled
                ),
            )
        )
    }
}