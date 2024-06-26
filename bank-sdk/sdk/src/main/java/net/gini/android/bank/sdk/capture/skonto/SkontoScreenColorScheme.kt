package net.gini.android.bank.sdk.capture.skonto

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SkontoScreenColorScheme(
    val backgroundColor: Color,
    val topAppBarColors: TopAppbarColorScheme,
    val invoiceScanSectionColors: InvoiceScanSectionColorScheme,
    val discountSectionColors: DiscountSectionColorScheme,
    val withoutDiscountSectionColors: WithoutDiscountSectionColorScheme,
    val footerSectionColorScheme: FooterSectionColorScheme,
) {

    @Immutable
    data class TopAppbarColorScheme(
        val contentColor: Color,
        val backgroundColor: Color,
    )

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
    data class DiscountSectionColorScheme(
        val titleTextColor: Color,
        val switchColors: SwitchColors,
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
        val continueButtonColors: ButtonColors,
    ) {

        data class DiscountLabelColorScheme(
            val backgroundColor: Color,
            val textColor: Color,
        )
    }
}