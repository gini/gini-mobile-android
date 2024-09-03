package net.gini.android.bank.sdk.capture.skonto.factory.text

import android.content.res.Resources
import androidx.compose.ui.res.stringResource
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoDiscountPercentageFormatter
import net.gini.android.capture.Amount
import java.math.BigDecimal

internal class SkontoDiscountLabelTextFactory(
    private val resources: Resources,
    private val discountPercentageFormatter: SkontoDiscountPercentageFormatter,
) {

    fun create(discountPercentage: BigDecimal): String = resources.getString(
        R.string.gbs_skonto_section_footer_label_discount,
        discountPercentageFormatter.format(discountPercentage.toFloat())
    )
}
