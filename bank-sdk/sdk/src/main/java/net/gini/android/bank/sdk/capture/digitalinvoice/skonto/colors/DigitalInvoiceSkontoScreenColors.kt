package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoFooterSectionColors
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoInfoDialogColors
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoInvoicePreviewSectionColors
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section.DigitalInvoiceSkontoSectionColors
import net.gini.android.capture.ui.components.picker.date.GiniDatePickerDialogColors
import net.gini.android.capture.ui.components.topbar.GiniTopBarColors
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class DigitalInvoiceSkontoScreenColors(
    val backgroundColor: Color,
    val topAppBarColors: GiniTopBarColors,
    val invoiceScanSectionColors: DigitalInvoiceSkontoInvoicePreviewSectionColors,
    val scontoSectionColors: DigitalInvoiceSkontoSectionColors,
    val datePickerColor: GiniDatePickerDialogColors,
    val infoDialogColors: DigitalInvoiceSkontoInfoDialogColors,
    val footerSectionColors: DigitalInvoiceSkontoFooterSectionColors,
) {

    companion object {
        @Composable
        fun colors(
            backgroundColor: Color = GiniTheme.colorScheme.background.primary,
            topAppBarColors: GiniTopBarColors =
                GiniTopBarColors.colors(),
            skontoInvoiceScanSectionColors: DigitalInvoiceSkontoInvoicePreviewSectionColors =
                DigitalInvoiceSkontoInvoicePreviewSectionColors.colors(),
            discountSectionColors: DigitalInvoiceSkontoSectionColors =
                DigitalInvoiceSkontoSectionColors.colors(),
            datePickerColor: GiniDatePickerDialogColors =
                GiniDatePickerDialogColors.colors(),
            infoDialogColors: DigitalInvoiceSkontoInfoDialogColors =
                DigitalInvoiceSkontoInfoDialogColors.colors(),
            footerSectionColors: DigitalInvoiceSkontoFooterSectionColors =
                DigitalInvoiceSkontoFooterSectionColors.colors(),
        ) = DigitalInvoiceSkontoScreenColors(
            backgroundColor = backgroundColor,
            topAppBarColors = topAppBarColors,
            invoiceScanSectionColors = skontoInvoiceScanSectionColors,
            scontoSectionColors = discountSectionColors,
            datePickerColor = datePickerColor,
            infoDialogColors = infoDialogColors,
            footerSectionColors = footerSectionColors,
        )
    }
}
