package net.gini.android.bank.sdk.capture.skonto.mapper

import android.content.res.Resources
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountValidationErrorUseCase
import net.gini.android.capture.Amount

private val maxAmount =
    Amount.parse("${GetSkontoAmountValidationErrorUseCase.SKONTO_AMOUNT_LIMIT}:EUR")

internal fun SkontoScreenState.Ready.FullAmountValidationError.toErrorMessage(
    resources: Resources,
    amountFormatter: AmountFormatter,
): String = when (this) {
    is SkontoScreenState.Ready.FullAmountValidationError.FullAmountLimitExceeded ->
        resources.getString(
            R.string.gbs_skonto_section_without_discount_field_amount_validation_error_limit_exceeded,
            amountFormatter.format(maxAmount)
        )
}