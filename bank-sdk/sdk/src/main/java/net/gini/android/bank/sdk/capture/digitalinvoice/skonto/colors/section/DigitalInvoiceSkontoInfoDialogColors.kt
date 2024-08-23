package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.colors.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
data class DigitalInvoiceSkontoInfoDialogColors(
    val cardBackgroundColor: Color,
    val textColor: Color,
    val buttonTextColor: Color,
) {

    companion object {

        @Composable
        fun colors(
            cardBackgroundColor: Color = GiniTheme.colorScheme.dialogs.container,
            textColor: Color = GiniTheme.colorScheme.dialogs.text,
            buttonTextColor: Color = GiniTheme.colorScheme.dialogs.labelText,
        ) = DigitalInvoiceSkontoInfoDialogColors(
            cardBackgroundColor = cardBackgroundColor,
            textColor = textColor,
            buttonTextColor = buttonTextColor,
        )
    }

}