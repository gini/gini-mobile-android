package net.gini.android.bank.sdk.capture.skonto.usecase

import org.junit.Test
import java.math.BigDecimal


class GetSkontoSavedAmountUseCaseTest {

    @Test
    fun `skonto saved amount should be calculated correctly`() {
        val useCase = GetSkontoSavedAmountUseCase()
        val skontoAmount = BigDecimal("100.00")
        val fullAmount = BigDecimal("200.00")
        val savedAmount = useCase.execute(skontoAmount, fullAmount)
        assert(savedAmount == BigDecimal("100.00"))
    }

    @Test
    fun `skonto saved amount should be calculated correctly in case of negative skonto amount`() {
        val useCase = GetSkontoSavedAmountUseCase()
        val skontoAmount = BigDecimal("200.00")
        val fullAmount = BigDecimal("100.00")
        val savedAmount = useCase.execute(skontoAmount, fullAmount)
        assert(savedAmount == BigDecimal.ZERO)
    }
}