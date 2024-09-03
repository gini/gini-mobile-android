package net.gini.android.bank.sdk.capture.skonto.factory.text

import android.content.res.Resources
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoDiscountPercentageFormatter
import net.gini.android.bank.sdk.capture.skonto.formatter.SkontoRemainingDaysFormatter
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import java.math.BigDecimal

internal class SkontoInfoBannerTextFactory(
    private val resources: Resources,
    private val skontoDiscountPercentageFormatter: SkontoDiscountPercentageFormatter,
    private val skontoRemainingDaysFormatter: SkontoRemainingDaysFormatter,
) {

    fun create(
        edgeCase: SkontoEdgeCase?,
        discountAmount: BigDecimal,
        remainingDays: Int
    ): String {
        val discountAmountText = skontoDiscountPercentageFormatter.format(discountAmount.toFloat())
        val remainingDaysText = skontoRemainingDaysFormatter.format(remainingDays)

        return when (edgeCase) {
            SkontoEdgeCase.PayByCashOnly ->
                resources.getString(
                    R.string.gbs_skonto_section_discount_info_banner_pay_cash_message,
                    discountAmountText,
                    remainingDaysText
                )

            SkontoEdgeCase.SkontoExpired ->
                resources.getString(
                    R.string.gbs_skonto_section_discount_info_banner_date_expired_message,
                    discountAmountText
                )

            SkontoEdgeCase.SkontoLastDay ->
                resources.getString(
                    R.string.gbs_skonto_section_discount_info_banner_pay_today_message,
                    discountAmountText
                )

            else -> resources.getString(
                R.string.gbs_skonto_section_discount_info_banner_normal_message,
                remainingDaysText,
                discountAmountText
            )
        }
    }
}
