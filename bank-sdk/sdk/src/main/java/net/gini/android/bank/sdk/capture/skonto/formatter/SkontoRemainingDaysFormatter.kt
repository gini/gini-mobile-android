package net.gini.android.bank.sdk.capture.skonto.formatter

import android.content.res.Resources
import net.gini.android.bank.sdk.R

class SkontoRemainingDaysFormatter(
    private val resources: Resources,
) {

    fun format(remainingDays: Int): String {
        return if (remainingDays != 0) {
            resources.getQuantityString(
                R.plurals.days,
                remainingDays,
                remainingDays.toString()
            )
        } else {
            resources.getString(R.string.days_zero)
        }
    }
}