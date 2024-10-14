package net.gini.android.bank.sdk.capture.skonto.usecase

import org.junit.Test
import java.time.LocalDate


class GetSkontoRemainingDaysUseCaseTest {

    @Test
    fun `Skonto remaining days should be calculated correctly`() {
        val useCase = GetSkontoRemainingDaysUseCase()
        val dueDate = LocalDate.now().plusDays(10)
        val remainingDays = useCase.execute(dueDate)
        assert(remainingDays == 10)
    }

}