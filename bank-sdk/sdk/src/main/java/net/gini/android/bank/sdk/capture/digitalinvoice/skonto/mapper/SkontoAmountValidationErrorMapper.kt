package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.mapper

import android.content.res.Resources
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState

internal fun SkontoScreenState.Ready.SkontoAmountValidationError.toErrorMessage(
    resources: Resources,
): String = when (this) {
    is SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount ->
        resources.getString(
            R.string.gbs_skonto_section_discount_field_amount_validation_error_skonto_amount_more_than_full_amount
        )
}
