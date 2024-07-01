package net.gini.android.bank.sdk.capture.skonto.colors

import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.capture.skonto.components.button.filled.GiniButtonColors
import net.gini.android.bank.sdk.capture.skonto.components.switcher.GiniSwitchColors
import net.gini.android.bank.sdk.ui.theme.GiniTheme

@Immutable
data class SkontoScreenColorScheme(
    val backgroundColor: Color,
    val topAppBarColors: TopAppbarColorScheme,
    val invoiceScanSectionColors: InvoiceScanSectionColorScheme,
    val discountSectionColors: SkontoSectionColorScheme,
    val withoutDiscountSectionColors: WithoutDiscountSectionColorScheme,
    val footerSectionColorScheme: FooterSectionColorScheme,
) {

    @Immutable
    data class TopAppbarColorScheme(
        val contentColor: Color,
        val backgroundColor: Color,
    ) {
        companion object {
            @Composable
            fun default() = with(GiniTheme.colorScheme) {
                TopAppbarColorScheme(
                    contentColor = text.primary,
                    backgroundColor = background.bar,
                )
            }
        }
    }

    @Immutable
    data class InvoiceScanSectionColorScheme(
        val cardBackgroundColor: Color,
        val titleTextColor: Color,
        val subtitleTextColor: Color,
        val iconBackgroundColor: Color,
        val iconTint: Color,
        val arrowTint: Color,
    )

    @Immutable
    data class SkontoSectionColorScheme(
        val titleTextColor: Color,
        val switchColors: GiniSwitchColors,
        val cardBackgroundColor: Color,
        val enabledHintTextColor: Color,
        val infoBannerColorScheme: InfoBannerColorScheme,
        val amountFieldColors: TextFieldColors,
        val dueDateTextFieldColor: TextFieldColors,
    ) {

        @Immutable
        data class InfoBannerColorScheme(
            val backgroundColor: Color,
            val textColor: Color,
            val iconTint: Color,
        )
    }

    @Immutable
    data class WithoutDiscountSectionColorScheme(
        val titleTextColor: Color,
        val cardBackgroundColor: Color,
        val amountFieldColors: TextFieldColors,
        val enabledHintTextColor: Color,
    )

    data class FooterSectionColorScheme(
        val cardBackgroundColor: Color,
        val titleTextColor: Color,
        val amountTextColor: Color,
        val discountLabelColorScheme: DiscountLabelColorScheme,
        val continueButtonColors: GiniButtonColors,
    ) {

        data class DiscountLabelColorScheme(
            val backgroundColor: Color,
            val textColor: Color,
        )
    }
}