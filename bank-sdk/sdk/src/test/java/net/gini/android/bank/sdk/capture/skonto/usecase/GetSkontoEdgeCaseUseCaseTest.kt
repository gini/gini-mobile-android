package net.gini.android.bank.sdk.capture.skonto.usecase

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoEdgeCase
import org.junit.Test
import java.time.LocalDate
import io.mockk.mockk


class GetSkontoEdgeCaseUseCaseTest {

    @Test
    fun `skonto last day edge case should be detected in case of today due date`() {
        val useCase = GetSkontoEdgeCaseUseCase()
        val dueDate = LocalDate.now()
        val paymentMethod = SkontoData.SkontoPaymentMethod.Unspecified
        val result = useCase.execute(dueDate, paymentMethod)
        assert(result == SkontoEdgeCase.SkontoLastDay)
    }

    @Test
    fun `skonto pay by cash today edge case should be detected in case of today due date and cash payment method`() {
        val useCase = GetSkontoEdgeCaseUseCase()
        val dueDate = LocalDate.now()
        val paymentMethod = SkontoData.SkontoPaymentMethod.Cash
        val result = useCase.execute(dueDate, paymentMethod)
        assert(result == SkontoEdgeCase.PayByCashToday)
    }

    @Test
    fun `skonto pay by cash only edge case should be detected in case cash only payment method`() {
        val useCase = GetSkontoEdgeCaseUseCase()
        val paymentMethod = SkontoData.SkontoPaymentMethod.Cash
        val result = useCase.execute(mockk(relaxed = true), paymentMethod)
        assert(result == SkontoEdgeCase.PayByCashOnly)
    }

    @Test
    fun `skonto last day edge case should be detected in case of due date tomorrow`() {
        val useCase = GetSkontoEdgeCaseUseCase()
        val dueDate = LocalDate.now()
        val paymentMethod = SkontoData.SkontoPaymentMethod.Unspecified
        val result = useCase.execute(dueDate, paymentMethod)
        assert(result == SkontoEdgeCase.SkontoLastDay)
    }

    @Test
    fun `no edge case should be detected if due date is in the future and payment method is not cash`() {
        val useCase = GetSkontoEdgeCaseUseCase()
        val dueDate = LocalDate.now().plusDays(1)
        val paymentMethod = SkontoData.SkontoPaymentMethod.Unspecified
        val result = useCase.execute(dueDate, paymentMethod)
        assert(result == null)
    }
}