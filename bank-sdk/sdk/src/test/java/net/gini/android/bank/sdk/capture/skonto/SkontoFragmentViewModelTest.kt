package net.gini.android.bank.sdk.capture.skonto

import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.gini.android.bank.sdk.MainDispatcherRule
import net.gini.android.bank.sdk.capture.skonto.factory.lines.SkontoInvoicePreviewTextLinesFactory
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDiscountPercentageUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoRemainingDaysUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoAmountValidator
import net.gini.android.bank.sdk.capture.skonto.validation.SkontoFullAmountValidator
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoFragmentViewModel
import net.gini.android.bank.sdk.capture.skonto.viewmodel.SkontoScreenInitialStateFactory
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.FullAmountChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.InfoBannerInteractionIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.InvoiceClickIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.ProceedClickedIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoActiveChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoAmountFieldChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.intent.SkontoDueDateChangeIntent
import net.gini.android.bank.sdk.capture.skonto.viewmodel.subintent.OpenExtractionsScreenSubIntent
import net.gini.android.capture.Amount
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
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

        val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
            getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
            getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
            getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
        )

        val viewModel = SkontoFragmentViewModel(
            data = skontoData,

            skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,

            proceedClickedIntent = mockk(),
            skontoActiveChangeIntent = mockk(),
            keyboardStateChangeIntent = mockk(),
            skontoAmountFieldChangeIntent = mockk(),
            invoiceClickIntent = mockk(),
            fullAmountChangeIntent = mockk(),
            skontoDueDateChangeIntent = mockk(),
            transactionDocDialogDecisionIntent = mockk(),
            infoBannerInteractionIntent = mockk(),
        )

        viewModel.test(this) {
            expectInitialState()
        }
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

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )


            val viewModel = SkontoFragmentViewModel(
                data = skontoData,

                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,

                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.test(this) {
                expectInitialState()
            }

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

            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase =
                mockk(relaxed = true)
            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)
            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk(relaxed = true)

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )

            val infoBannerInteractionIntent = InfoBannerInteractionIntent()

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = infoBannerInteractionIntent,
            )

            viewModel.test(this) {
                runOnCreate()
                containerHost.onInfoBannerClicked()
                expectState {
                    (this as SkontoScreenState.Ready).copy(edgeCaseInfoDialogVisible = true)
                }
            }
        }

    @Test
    fun `when user dismiss info message the edge case dialog should became invisible`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase =
                mockk(relaxed = true)
            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)
            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk(relaxed = true)

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )

            val infoBannerInteractionIntent = InfoBannerInteractionIntent()

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = infoBannerInteractionIntent,
            )

            viewModel.test(this) {
                runOnCreate()
                expectInitialState()
                containerHost.onInfoDialogDismissed()
                expectState {
                    (this as SkontoScreenState.Ready).copy(edgeCaseInfoDialogVisible = false)
                }
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

            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase =
                mockk(relaxed = true)
            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)
            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk(relaxed = true)

            val invoiceClickIntent = InvoiceClickIntent(
                lastAnalyzedDocumentProvider = lastAnalyzedDocumentProvider,
                skontoInvoicePreviewTextLinesFactory = skontoInvoicePreviewTextLinesFactory,
            )

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = invoiceClickIntent,
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.test(this) {
                expectInitialState()
                containerHost.onInvoiceClicked()
                assert(awaitSideEffect() is SkontoScreenSideEffect.OpenInvoiceScreen)
            }
        }

    @Test
    fun `when user swith skonto the final amount should be changed`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true)

            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase =
                mockk(relaxed = true)
            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)
            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk(relaxed = true)

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )

            val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase = mockk() {
                every { execute(any(), any()) } returns BigDecimal.ZERO
            }

            val skontoActiveChangeIntent = SkontoActiveChangeIntent(
                getSkontoDiscountPercentageUseCase = getSkontoDiscountPercentageUseCase,
            )

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = skontoActiveChangeIntent,
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.test(this) {
                expectInitialState()
                runOnCreate()
                viewModel.onSkontoActiveChanged(false)
                expectState {
                    (this as SkontoScreenState.Ready).copy(
                        isSkontoSectionActive = false,
                        totalAmount = fullAmount,
                        skontoPercentage = BigDecimal.ZERO
                    )
                }
                viewModel.onSkontoActiveChanged(true)
                expectState {
                    (this as SkontoScreenState.Ready).copy(
                        isSkontoSectionActive = true,
                        totalAmount = skontoAmount,
                        skontoPercentage = BigDecimal.ZERO
                    )
                }
            }
        }

    @Test
    fun `when user changes skonto amount the discount should be recalculated`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true) {
                every { fullAmountToPay } returns Amount.parse("100:EUR")
                every { skontoAmountToPay } returns Amount.parse("90:EUR")
                every { skontoPercentageDiscounted } returns BigDecimal.ZERO
            }

            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase = mockk {
                every { execute(any(), any()) } returns BigDecimal.ZERO
            }

            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)

            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk { every { execute(any()) } returns true }

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )

            val getSkontoDiscountPercentageUseCase: GetSkontoDiscountPercentageUseCase = mockk {
                every { execute(any(), any()) } returns BigDecimal.ZERO
            }

            val skontoAmountFieldChangeIntent = SkontoAmountFieldChangeIntent(
                skontoAmountValidator = SkontoAmountValidator(),
                getSkontoDiscountPercentageUseCase = getSkontoDiscountPercentageUseCase,
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
            )

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = skontoAmountFieldChangeIntent,
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.test(this) {
                expectInitialState()
                runOnCreate()
                val newSkontoAmount = BigDecimal("95")
                containerHost.onSkontoAmountFieldChanged(newSkontoAmount)
                expectState {
                    (this as SkontoScreenState.Ready).copy(
                        skontoAmount = skontoAmount.copy(value = newSkontoAmount),
                        totalAmount = totalAmount.copy(value = newSkontoAmount),
                    )
                }
            }

            coVerify(exactly = 1) {
                getSkontoDiscountPercentageUseCase.execute(any(), any())
            }
        }

    @Test
    fun `when user changes skonto amount to incorrect value error should be shown`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true) {
                every { fullAmountToPay } returns Amount.parse("100:EUR")
                every { skontoAmountToPay } returns Amount.parse("90:EUR")
            }

            val getSkontoDiscountPercentageUseCase = mockk<GetSkontoDiscountPercentageUseCase> {
                every { execute(any(), any()) } returns mockk()
            }

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = mockk(relaxed = true),
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
            )

            val skontoAmountFieldChangeIntent = SkontoAmountFieldChangeIntent(
                skontoAmountValidator = SkontoAmountValidator(),
                getSkontoDiscountPercentageUseCase = getSkontoDiscountPercentageUseCase,
                getSkontoSavedAmountUseCase = mockk(),
            )

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = skontoAmountFieldChangeIntent,
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.test(this) {
                expectInitialState()
                runOnCreate()
                containerHost.onSkontoAmountFieldChanged(BigDecimal("110"))
                expectState {
                    with(this as SkontoScreenState.Ready) {
                        copy(
                            skontoAmountValidationError = SkontoScreenState
                                .Ready.SkontoAmountValidationError.SkontoAmountMoreThanFullAmount
                        )
                    }
                }
            }
        }

    @Test
    fun `when user changes full amount the skonto amount should be recalculated`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true) {
                every { fullAmountToPay } returns Amount.parse("100:EUR")
                every { skontoAmountToPay } returns Amount.parse("90:EUR")
                every { skontoPercentageDiscounted } returns BigDecimal.ZERO
            }

            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase = mockk {
                every { execute(any(), any()) } returns BigDecimal.ZERO
            }

            val getSkontoAmountUseCase: GetSkontoAmountUseCase = mockk {
                every { execute(any(), any()) } returns BigDecimal.ONE
            }

            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)

            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk { every { execute(any()) } returns true }

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )

            val fullAmountChangeIntent = FullAmountChangeIntent(
                skontoFullAmountValidator = SkontoFullAmountValidator(),
                getSkontoAmountUseCase = getSkontoAmountUseCase,
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
            )

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = fullAmountChangeIntent,
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.test(this) {
                expectInitialState()
                runOnCreate()
                val newFullAmount = BigDecimal("200")
                containerHost.onFullAmountFieldChanged(newFullAmount)
                expectState {
                    (this as SkontoScreenState.Ready).copy(
                        fullAmount = skontoAmount.copy(value = newFullAmount),
                        skontoAmount = skontoAmount.copy(value = BigDecimal.ONE)
                    )
                }
            }

            coVerify(exactly = 1) {
                getSkontoAmountUseCase.execute(any(), any())
            }
        }

    @Test
    fun `when user clicks proceed the extraction screen should be opened`() =
        runTest {

            val skontoData: SkontoData = mockk(relaxed = true)

            val listener = mockk<SkontoFragmentListener>(relaxed = true) {
                every { onPayInvoiceWithSkonto(any(), any()) } just Runs
            }

            val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase =
                mockk(relaxed = true)
            val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase =
                mockk(relaxed = true)
            val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase =
                mockk(relaxed = true)

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = getSkontoSavedAmountUseCase,
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = getSkontoDefaultSelectionStateUseCase,
            )

            val openExtractionsScreenSubIntent = OpenExtractionsScreenSubIntent(
                skontoExtractionsHandler = mockk(relaxed = true),
                lastExtractionsProvider = mockk(relaxed = true),
            )

            val proceedClickedIntent = ProceedClickedIntent(
                openExtractionsScreenSubIntent = openExtractionsScreenSubIntent,
                getTransactionDocShouldBeAutoAttachedUseCase = mockk(relaxed = true),
                getTransactionDocsFeatureEnabledUseCase = mockk(relaxed = true),
                transactionDocDialogConfirmAttachUseCase = mockk(relaxed = true),
            )

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = proceedClickedIntent,
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = mockk(),
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.setListener(listener)

            viewModel.test(this) {
                runOnCreate()
                expectInitialState()
                containerHost.onProceedClicked()
            }

            verify(exactly = 1) {
                listener.onPayInvoiceWithSkonto(any(), any())
            }
        }

    @Test
    fun `when user changes due date to date it should be applied`() =
        runTest {
            val skontoData: SkontoData = mockk(relaxed = true) {
                every { skontoDueDate } returns LocalDate.now()
            }

            val getSkontoEdgeCaseUseCase = mockk<GetSkontoEdgeCaseUseCase> {
                every { execute(any(), any()) } returns mockk(relaxed = true)
            }

            val skontoScreenInitialStateFactory = SkontoScreenInitialStateFactory(
                getSkontoSavedAmountUseCase = mockk(relaxed = true),
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
                getSkontoDefaultSelectionStateUseCase = mockk(relaxed = true),
            )

            val skontoDueDateChangeIntent = SkontoDueDateChangeIntent(
                getSkontoRemainingDaysUseCase = GetSkontoRemainingDaysUseCase(),
                getSkontoEdgeCaseUseCase = getSkontoEdgeCaseUseCase,
            )

            val viewModel = SkontoFragmentViewModel(
                data = skontoData,
                skontoScreenInitialStateFactory = skontoScreenInitialStateFactory,
                proceedClickedIntent = mockk(),
                skontoActiveChangeIntent = mockk(),
                keyboardStateChangeIntent = mockk(),
                skontoAmountFieldChangeIntent = mockk(),
                invoiceClickIntent = mockk(),
                fullAmountChangeIntent = mockk(),
                skontoDueDateChangeIntent = skontoDueDateChangeIntent,
                transactionDocDialogDecisionIntent = mockk(),
                infoBannerInteractionIntent = mockk(),
            )

            viewModel.test(this) {
                expectInitialState()
                runOnCreate()
                val newDueDate = LocalDate.now().plusDays(5)
                containerHost.onSkontoDueDateChanged(newDueDate)
                expectState {
                    (this as SkontoScreenState.Ready).copy(
                        discountDueDate = newDueDate,
                        paymentInDays = 5
                    )
                }
                val pastDueDate = LocalDate.now().minusDays(5)
                containerHost.onSkontoDueDateChanged(pastDueDate)
                expectState {
                    (this as SkontoScreenState.Ready).copy(
                        discountDueDate = pastDueDate,
                        paymentInDays = 5 // always absolute value
                    )
                }
            }
        }
}
