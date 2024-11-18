package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.viewmodel.SkontoContainerHost
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoRemainingDaysUseCase
import java.time.LocalDate

internal class SkontoDueDateChangeIntent(
    private val getSkontoRemainingDaysUseCase: GetSkontoRemainingDaysUseCase,
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
) {

    fun SkontoContainerHost.run(newDate: LocalDate) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        val newPayInDays = getSkontoRemainingDaysUseCase.execute(newDate)
        reduce {
            state.copy(
                discountDueDate = newDate,
                paymentInDays = newPayInDays,
                edgeCase = getSkontoEdgeCaseUseCase.execute(
                    dueDate = newDate,
                    paymentMethod = state.paymentMethod
                )
            )
        }
    }
}
