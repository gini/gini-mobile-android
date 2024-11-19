package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import org.junit.Test

class GetSkontoDefaultSelectionStateUseCaseTest {

    @Test
    fun `skonto section should be selected if no edge case detected`() {
        val useCase = GetSkontoDefaultSelectionStateUseCase()
        val result = useCase.execute(null)
        assert(result)
    }

    @Test
    fun `skonto section should not be selected if pay by cash edge case detected`() {
        val useCase = GetSkontoDefaultSelectionStateUseCase()
        val result = useCase.execute(SkontoEdgeCase.PayByCashOnly)
        assert(!result)
    }

    @Test
    fun `skonto section should not be selected if pay by cash today edge case detected`() {
        val useCase = GetSkontoDefaultSelectionStateUseCase()
        val result = useCase.execute(SkontoEdgeCase.PayByCashToday)
        assert(!result)
    }

    @Test
    fun `skonto section should not be selected if skonto expired edge case detected`() {
        val useCase = GetSkontoDefaultSelectionStateUseCase()
        val result = useCase.execute(SkontoEdgeCase.SkontoExpired)
        assert(!result)
    }
}