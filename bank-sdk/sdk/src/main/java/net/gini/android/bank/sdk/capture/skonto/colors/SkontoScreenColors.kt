package net.gini.android.bank.sdk.capture.skonto.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoFooterSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoInvoiceScanSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.SkontoSectionColors
import net.gini.android.bank.sdk.capture.skonto.colors.section.WithoutSkontoSectionColors
import net.gini.android.capture.ui.components.picker.date.GiniDatePickerDialogColors
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class SkontoScreenColors(
    val backgroundColor: Color,
    val topAppBarColors: GiniTopBarColors,
    val invoiceScanSectionColors: SkontoInvoiceScanSectionColors,
    val skontoSectionColors: SkontoSectionColors,
    val withoutSkontoSectionColors: WithoutSkontoSectionColors,
    val footerSectionColors: SkontoFooterSectionColors,
    val datePickerColor: GiniDatePickerDialogColors,
) {

    companion object {
        @Composable
        fun colors(
            backgroundColor: Color = GiniTheme.colorScheme.background.background,
            topAppBarColors: GiniTopBarColors = GiniTopBarColors.colors(),
            skontoInvoiceScanSectionColors: SkontoInvoiceScanSectionColors = SkontoInvoiceScanSectionColors.colors(),
            discountSectionColors: SkontoSectionColors = SkontoSectionColors.colors(),
            withoutSkontoSectionColors: WithoutSkontoSectionColors = WithoutSkontoSectionColors.colors(),
            skontoFooterSectionColors: SkontoFooterSectionColors = SkontoFooterSectionColors.colors(),
            datePickerColor: GiniDatePickerDialogColors = GiniDatePickerDialogColors.colors()
        ) = SkontoScreenColors(
            backgroundColor = backgroundColor,
            topAppBarColors = topAppBarColors,
            invoiceScanSectionColors = skontoInvoiceScanSectionColors,
            skontoSectionColors = discountSectionColors,
            withoutSkontoSectionColors = withoutSkontoSectionColors,
            footerSectionColors = skontoFooterSectionColors,
            datePickerColor = datePickerColor,
        )
    }
}