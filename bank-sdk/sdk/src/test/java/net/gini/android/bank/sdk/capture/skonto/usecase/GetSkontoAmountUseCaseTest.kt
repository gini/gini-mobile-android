package net.gini.android.bank.sdk.capture.skonto.usecase

import org.junit.Test
import java.math.BigDecimal


class GetSkontoAmountUseCaseTest {

    @Test
    fun `skonto amount should be calculated correctly`() {
        val useCase = GetSkontoAmountUseCase()
        val fullAmount = BigDecimal("100.00")
        val discount = BigDecimal("10.00")
        val result = useCase.execute(fullAmount, discount)
        assert(result == BigDecimal("90.00"))
    }

}