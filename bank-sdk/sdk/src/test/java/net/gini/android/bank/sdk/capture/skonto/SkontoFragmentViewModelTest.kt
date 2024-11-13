package net.gini.android.bank.sdk.capture.skonto

import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.gini.android.bank.sdk.MainDispatcherRule
import net.gini.android.bank.sdk.capture.skonto.factory.lines.SkontoInvoicePreviewTextLinesFactory
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoFragmentViewModel
import net.gini.android.capture.Amount
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SkontoFragmentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state should be prepared and fired`() = runTest {

        val skontoData: SkontoData = mockk(relaxed = true)
        val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase =
            mockk(relaxed = true)
        val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
            mockk(relaxed = true)
        val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
            mockk(relaxed = true)

        val viewModel = SkontoFragmentViewModel(
            data = skontoData,
            getTransactionDocsFeatureEnabledUseCase = mockk(),
            getSkontoDiscountPercentageUseCase = mockk(),
            getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
            getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
            getSkontoAmountUseCase = mockk(),
            getSkontoRemainingDaysUseCase = mockk(),
            getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            skontoExtractionsHandler = mockk(),
            lastAnalyzedDocumentProvider = mockk(),
            skontoInvoicePreviewTextLinesFactory = mockk(),
            lastExtractionsProvider = mockk(),
            transactionDocDialogConfirmAttachUseCase = mockk(),
            transactionDocDialogCancelAttachUseCase = mockk(),
            getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
            skontoAmountValidator = mockk(),
            skontoFullAmountValidator = mockk(),
        )

        val flowData = viewModel.stateFlow.first()
        assert(flowData is SkontoScreenState.Ready)
    }

    @Test
    fun `initial state should be prepared based on calculated skonto amount and edge case`() =
        runTest {

            val skontoData: SkontoData = mockk(relaxed = true)
            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase =
                mockk(relaxed = true)
            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)
            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk(relaxed = true)

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = mockk(),
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            val flowData = viewModel.stateFlow.first()
            assert(flowData is SkontoScreenState.Ready)

            coVerify(exactly = 1) {
                getSkontoSavedAmountUseCase.execute(any(), any())
            }
            coVerify(exactly = 1) {
                getSkontoEdgeCaseUseCase.execute(any(), any())
            }
        }

    @Test
    fun `when user clicks on info message the edge case dialog should became visible`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = mockk(),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            with(viewModel.stateFlow.first()) {
                assert(this is SkontoScreenState.Ready)
                require(this is SkontoScreenState.Ready)
            }

            viewModel.onInfoBannerClicked()

            with(viewModel.stateFlow.first()) {
                assert(this is SkontoScreenState.Ready)
                require(this is SkontoScreenState.Ready)
                assert(this.edgeCaseInfoDialogVisible)
            }
        }

    @Test
    fun `when user dismiss info message the edge case dialog should became invisible`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = mockk(),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            viewModel.stateFlow.value = mockk<SkontoScreenState.Ready>(relaxed = true)
                .copy(edgeCaseInfoDialogVisible = true)

            viewModel.onInfoDialogDismissed()

            with(viewModel.stateFlow.first()) {
                assert(this is SkontoScreenState.Ready)
                require(this is SkontoScreenState.Ready)
                assert(!this.edgeCaseInfoDialogVisible)
            }
        }

    @Test
    fun `when user clicks on invoice preview the VM should fire the navigation side effect`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)
            val lastAnalyzedDocumentProvider = mockk<LastAnalyzedDocumentProvider> {
                every { provide() } returns mockk(relaxed = true)
            }
            val skontoInvoicePreviewTextLinesFactory = mockk<SkontoInvoicePreviewTextLinesFactory> {
                every { create(any()) } returns mockk(relaxed = true)
            }
            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = mockk(),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = lastAnalyzedDocumentProvider,
                skontoInvoicePreviewTextLinesFactory = skontoInvoicePreviewTextLinesFactory,
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            viewModel.sideEffectFlow.test {
                viewModel.onInvoiceClicked()
                assert(awaitItem() is SkontoScreenSideEffect.OpenInvoiceScreen)
            }
        }

    @Test
    fun `when user disables skonto the final amount should be changed to full price`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = mockk(relaxed = true),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            viewModel.stateFlow.test {
                skipItems(1) // skip initial state
                viewModel.onSkontoActiveChanged(false)
                with(awaitItem()) {
                    assert(this is SkontoScreenState.Ready)
                    require(this is SkontoScreenState.Ready)
                    assert(!this.isSkontoSectionActive)
                    assert(this.totalAmount == this.fullAmount)
                }
            }
        }

    @Test
    fun `when user enables skonto the final amount should be changed to skonto price`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = mockk(relaxed = true),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            viewModel.stateFlow.test {
                skipItems(1) // skip initial state
                viewModel.onSkontoActiveChanged(true)
                with(awaitItem()) {
                    assert(this is SkontoScreenState.Ready)
                    require(this is SkontoScreenState.Ready)
                    assert(this.isSkontoSectionActive)
                    assert(this.totalAmount == this.skontoAmount)
                }
            }
        }

    @Test
    fun `when user changes skonto amount the discount should be recalculated`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true) {
                every { fullAmountToPay } returns Amount.parse("100:EUR")
            }

            val getSkontoDiscountPercentageUseCase = mockk<GetSkontoDiscountPercentageUseCase>(
                relaxed = true
            ) {
                every { execute(any(), any()) } returns mockk()
            }

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = getSkontoDiscountPercentageUseCase,
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk {
                    coEvery { execute(any(), any()) } returns null
                },
                skontoFullAmountValidator = mockk {
                    coEvery { execute(any()) } returns null
                },
            )

            viewModel.onSkontoAmountFieldChanged(BigDecimal("95"))

            coVerify(exactly = 1) {
                getSkontoDiscountPercentageUseCase.execute(
                    any(), any()
                )
            }
        }

    @Test
    fun `when user changes skonto amount to incorrect value no action should be performed`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true) {
                every { fullAmountToPay } returns Amount.parse("100:EUR")
            }

            val getSkontoDiscountPercentageUseCase = mockk<GetSkontoDiscountPercentageUseCase> {
                every { execute(any(), any()) } returns mockk()
            }


            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = getSkontoDiscountPercentageUseCase,
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(),
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(relaxed = true),
                skontoFullAmountValidator = mockk(),
            )

            viewModel.onSkontoAmountFieldChanged(BigDecimal("110"))

            coVerify(exactly = 0) {
                getSkontoDiscountPercentageUseCase.execute(
                    any(), any()
                )
            }
        }

    @Test
    fun `when user changes full amount the skonto amount should be recalculated`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true) {
                every { skontoAmountToPay } returns Amount.parse("100:EUR")
                every { fullAmountToPay } returns Amount.parse("150:EUR")
            }

            val getSkontoAmountUseCase = mockk<GetSkontoAmountUseCase> {
                every { execute(any(), any()) } returns mockk()
            }

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk(),
                getSkontoDiscountPercentageUseCase = mockk(relaxed = true),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = getSkontoAmountUseCase,
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true) {
                    every { execute(any()) } returns false
                },
                skontoExtractionsHandler = mockk(),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk {
                    coEvery { execute(any(), any()) } returns null
                },
                skontoFullAmountValidator = mockk {
                    coEvery { execute(any()) } returns null
                },
            )

            viewModel.onFullAmountFieldChanged(BigDecimal("200"))

            coVerify(exactly = 1) {
                getSkontoAmountUseCase.execute(any(), any())
            }
        }

    @Test
    fun `when user clicks proceed the extraction screen should be opened`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val getSkontoAmountUseCase = mockk<GetSkontoAmountUseCase> {
                every { execute(any(), any()) } returns mockk()
            }

            val listener = mockk<SkontoFragmentListener>(relaxed = true) {
                every { onPayInvoiceWithSkonto(any(), any()) } just Runs
            }

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk {
                    every { this@mockk.invoke() } returns false
                },
                getSkontoDiscountPercentageUseCase = mockk(relaxed = true),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = getSkontoAmountUseCase,
                getSkontoRemainingDaysUseCase = mockk(),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(relaxed = true),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(relaxed = true),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            viewModel.setListener(listener)

            viewModel.onProceedClicked()

            verify(exactly = 1) {
                listener.onPayInvoiceWithSkonto(any(), any())
            }
        }

    @Test
    fun `when user changes due date to date it should be applied`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                getTransactionDocsFeatureEnabledUseCase = mockk {
                    every { this@mockk.invoke() } returns false
                },
                getSkontoDiscountPercentageUseCase = mockk(relaxed = true),
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoAmountUseCase = mockk(relaxed = true),
                getSkontoRemainingDaysUseCase = mockk(relaxed = true),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
                skontoExtractionsHandler = mockk(relaxed = true),
                lastAnalyzedDocumentProvider = mockk(),
                skontoInvoicePreviewTextLinesFactory = mockk(),
                lastExtractionsProvider = mockk(relaxed = true),
                transactionDocDialogConfirmAttachUseCase = mockk(),
                transactionDocDialogCancelAttachUseCase = mockk(),
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(),
                skontoAmountValidator = mockk(),
                skontoFullAmountValidator = mockk(),
            )

            viewModel.stateFlow.test {
                skipItems(1) // skip initial state
                val futureDueDate = LocalDate.now().plusDays(5)
                viewModel.onSkontoDueDateChanged(futureDueDate)
                with(awaitItem()) {
                    assert(this is SkontoScreenState.Ready)
                    require(this is SkontoScreenState.Ready)
                    assert(this.discountDueDate == futureDueDate)
                }
                val pastDueDate = LocalDate.now().minusDays(5)
                viewModel.onSkontoDueDateChanged(pastDueDate)
                with(awaitItem()) {
                    assert(this is SkontoScreenState.Ready)
                    require(this is SkontoScreenState.Ready)
                    assert(this.discountDueDate == pastDueDate)
                }
            }
        }
}
