package net.gini.android.bank.sdk.capture.skonto.viewmodel.intent

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoRemainingDaysUseCase
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenContainerHost
import java.time.LocalDate

internal class SkontoDueDateChangeIntent(
    private val getSkontoRemainingDaysUseCase: GetSkontoRemainingDaysUseCase,
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
) {

    fun SkontoScreenContainerHost.run(newDate: LocalDate) = intent {
        val state = state as? SkontoScreenState.Ready ?: return@intent
        val newPayInDays = getSkontoRemainingDaysUseCase.execute(newDate)
        reduce {
            state.copy(
                discountDueDate = newDate,
                paymentInDays = newPayInDays,
                skontoEdgeCase = getSkontoEdgeCaseUseCase.execute(
                    dueDate = newDate,
                    paymentMethod = state.paymentMethod
                )
            )
        }
    }
}
