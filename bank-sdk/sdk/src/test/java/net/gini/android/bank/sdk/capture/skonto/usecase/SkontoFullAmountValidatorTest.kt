package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoFullAmountValidator
import org.junit.Test
import java.math.BigDecimal

class SkontoFullAmountValidatorTest {

    @Test
    fun `full amount validation error should be null if full amount is less than or equal to MAX_AMOUNT`() {
        val useCase = SkontoFullAmountValidator()
        val fullAmount = BigDecimal("100.00")
        val result = useCase.execute(fullAmount)
        assert(result == null)
    }

    @Test
    fun `full amount validation error should be MAX_AMOUNT_EXCEEDED if full amount is greater than MAX_AMOUNT`() {
        val useCase = SkontoFullAmountValidator()
        val fullAmount = BigDecimal("1000000.00")
        val result = useCase.execute(fullAmount)
        assert(result == SkontoScreenState.Ready.FullAmountValidationError.FullAmountLimitExceeded)
    }
}