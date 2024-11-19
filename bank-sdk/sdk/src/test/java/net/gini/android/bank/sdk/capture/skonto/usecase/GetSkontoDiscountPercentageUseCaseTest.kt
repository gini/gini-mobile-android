package net.gini.android.bank.sdk.capture.skonto.usecase

import org.junit.Test
import java.math.BigDecimal


class GetSkontoDiscountPercentageUseCaseTest {

    @Test
    fun `skonto discount percentage should be calculated correctly`() {
        val useCase = GetSkontoDiscountPercentageUseCase()
        val skontoAmount = BigDecimal("100.00")
        val fullAmount = BigDecimal("200.00")
        val result = useCase.execute(skontoAmount, fullAmount)
        assert(result == BigDecimal("50.0000"))
    }
}