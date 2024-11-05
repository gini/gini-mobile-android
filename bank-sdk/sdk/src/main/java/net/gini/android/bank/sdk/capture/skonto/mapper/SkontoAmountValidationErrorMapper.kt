package net.gini.android.bank.sdk.capture.skonto.mapper

import android.content.res.Resources
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountValidationErrorUseCase
import net.gini.android.capture.Amount

private val maxAmount =
    Amount.parse("${GetSkontoAmountValidationErrorUseCase.SKONTO_AMOUNT_LIMIT}:EUR")

internal fun SkontoScreenState.Ready.SkontoAmountValidationError.toErrorMessage(
    resources: Resources,
    amountFormatter: AmountFormatter,
): String = when (this) {
    is SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount ->
        resources.getString(
            R.string.gbs_skonto_section_discount_field_amount_validation_error_skonto_amount_more_than_full_amount
        )

    SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountLimitExceeded ->
        resources.getString(
            R.string.gbs_skonto_section_discount_field_amount_validation_error_limit_exceeded,
            amountFormatter.format(maxAmount)
        )
}