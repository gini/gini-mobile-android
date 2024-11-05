package net.gini.android.bank.sdk.capture.skonto.usecase

import org.junit.Test
import java.time.LocalDate


class GetSkontoRemainingDaysUseCaseTest {

    @Test
    fun `Skonto remaining days should be calculated correctly`() {
        val useCase = GetSkontoRemainingDaysUseCase()
        assert(useCase.execute(LocalDate.now().plusDays(10)) == 10)
        assert(useCase.execute(LocalDate.now().plusDays(0)) == 0)
        // Past date should be as absolute value
        assert(useCase.execute(LocalDate.now().minusDays(10)) == 10)
    }

}