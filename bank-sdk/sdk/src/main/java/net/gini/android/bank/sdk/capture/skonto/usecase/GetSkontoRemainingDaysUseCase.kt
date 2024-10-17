package net.gini.android.bank.sdk.capture.skonto.usecase

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

internal class GetSkontoRemainingDaysUseCase {

    /**
     * Calculates the Skonto remaining days based on the due date.
     *
     * @return The number of days until the Skonto due date.
     */
    fun execute(dueDate: LocalDate): Int =
        ChronoUnit.DAYS.between(dueDate, LocalDate.now()).absoluteValue.toInt()
}
