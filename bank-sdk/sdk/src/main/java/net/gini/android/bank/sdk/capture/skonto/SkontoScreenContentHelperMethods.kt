package net.gini.android.bank.sdk.capture.skonto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.gini.android.bank.sdk.R

@Composable
internal fun getSkontoRemainingDays(infoPaymentInDays : Int) : String {
    return if (infoPaymentInDays != 0) {
        pluralStringResource(
            id = R.plurals.days,
            count = infoPaymentInDays,
            infoPaymentInDays.toString()
        )
    } else {
        stringResource(id = R.string.days_zero)
    }
}

@Composable
internal fun getInvoicePreviewPaddingTop(): Dp {
    val context = LocalContext.current
    val isTablet =
        remember { context.resources.getBoolean(net.gini.android.capture.R.bool.gc_is_tablet) }

    return if (isTablet) 64.dp else 8.dp
}
