package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.SkontoScreenState
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoAmountValidator
import org.junit.Test
import java.math.BigDecimal


class SkontoAmountValidatorTest {

    @Test
    fun `skonto amount validation error should be null if skonto amount is less than or equal to full amount`() {
        val useCase = SkontoAmountValidator()
        val skontoAmount = BigDecimal("100.00")
        val fullAmount = BigDecimal("200.00")
        val result = useCase.execute(skontoAmount, fullAmount)
        assert(result == null)
    }

    @Test
    fun `skonto amount validation error should be SKONTO_AMOUNT_MORE_THAN_FULL_AMOUNT if skonto amount is greater than full amount`() {
        val useCase = SkontoAmountValidator()
        val skontoAmount = BigDecimal("300.00")
        val fullAmount = BigDecimal("200.00")
        val result = useCase.execute(skontoAmount, fullAmount)
        assert(result == SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount)
    }

    @Test
    fun `skonto amount validation error should be SKONTO_AMOUNT_LIMIT_EXCEEDED if skonto amount is greater than MAX_AMOUNT`() {
        val useCase = SkontoAmountValidator()
        val skontoAmount = BigDecimal("1000000.00")
        val fullAmount = BigDecimal("1000000.00")
        val result = useCase.execute(skontoAmount, fullAmount)
        assert(result == SkontoScreenState.Ready.SkontoAmountValidationError.SkontoAmountLimitExceeded)
    }
}