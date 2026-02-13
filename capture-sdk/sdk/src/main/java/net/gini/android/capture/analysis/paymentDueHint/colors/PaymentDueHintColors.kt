package net.gini.android.capture.analysis.paymentDueHint.colors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import net.gini.android.capture.ui.theme.GiniTheme

@Immutable
class PaymentDueHintColors(
    //tip colors
    val tipBackgroundColor: Color,
    val tipContentWarningColor: Color,
    //button
    val buttonTextColor: Color,
    val buttonProgressBarColor: Color,
    val buttonBorderColor: Color,
    val buttonBackgroundColor: Color,
) {

    companion object {
        @Composable
        fun colors(
            tipBackgroundColor: Color = GiniTheme.colorScheme.card.containerWarning,
            tipContentWarningColor: Color = GiniTheme.colorScheme.card.contentWarning,
            buttonTextColor: Color = GiniTheme.colorScheme.progressBarButton.content,
            buttonProgressBarColor: Color = GiniTheme.colorScheme.progressBarButton.progress,
            buttonBorderColor: Color = GiniTheme.colorScheme.progressBarButton.border,
            buttonBackgroundColor: Color = GiniTheme.colorScheme.progressBarButton.container,
        ) = PaymentDueHintColors(
            tipBackgroundColor = tipBackgroundColor,
            tipContentWarningColor = tipContentWarningColor,
            buttonTextColor = buttonTextColor,
            buttonProgressBarColor = buttonProgressBarColor,
            buttonBorderColor = buttonBorderColor,
            buttonBackgroundColor = buttonBackgroundColor
        )
    }
}

