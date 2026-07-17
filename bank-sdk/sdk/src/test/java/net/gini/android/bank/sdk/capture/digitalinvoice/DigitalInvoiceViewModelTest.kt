package net.gini.android.bank.sdk.capture.digitalinvoice

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.bank.sdk.capture.util.OncePerInstallEventStore
import net.gini.android.bank.sdk.capture.util.SimpleBusEventStore
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.After
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class DigitalInvoiceViewModelTest {

    private val returnReasonsFixture = listOf(
        GiniCaptureReturnReason("1", mapOf("de" to "Foo", "en" to "Foo")),
        GiniCaptureReturnReason("2", mapOf("de" to "Bar", "en" to "Bar"))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        extractions: Map<String, GiniCaptureSpecificExtraction> = emptyMap(),
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction> = emptyMap(),
        returnReasons: List<GiniCaptureReturnReason> = emptyList(),
        oncePerInstallEventStore: OncePerInstallEventStore = mockk(relaxed = true),
        simpleBusEventStore: SimpleBusEventStore = mockk(relaxed = true) {
            every { observeChange(any()) } returns emptyFlow()
        },
    ) = DigitalInvoiceViewModel(
        extractions = extractions,
        compoundExtractions = compoundExtractions,
        returnReasons = returnReasons,
        skontoData = null,
        savedInstanceBundle = null,
        oncePerInstallEventStore = oncePerInstallEventStore,
        simpleBusEventStore = simpleBusEventStore,
        getSkontoDefaultSelectionStateUseCase = GetSkontoDefaultSelectionStateUseCase(),
        getSkontoEdgeCaseUseCase = GetSkontoEdgeCaseUseCase(),
        getSkontoAmountUseCase = GetSkontoAmountUseCase(),
        getSkontoSavedAmountUseCase = GetSkontoSavedAmountUseCase(),
        skontoInfoBannerTextFactory = mockk(),
    )

    @Test
    fun `shows return reasons dialog when enabled and has a return reasons list`() = runTest {
        // Given
        GiniBank.enableReturnReasons = true

        val viewModel = createViewModel(returnReasons = returnReasonsFixture)

        viewModel.sideEffects.test {
            // When
            viewModel.deselectLineItem(mockk<SelectableLineItem>())

            // Then
            val sideEffect = awaitItem()
            assertThat(sideEffect)
                .isInstanceOf(DigitalInvoiceSideEffect.ShowReturnReasonDialog::class.java)
            assertThat(
                (sideEffect as DigitalInvoiceSideEffect.ShowReturnReasonDialog).reasons
            ).isEqualTo(returnReasonsFixture)
            expectNoEvents()
        }
    }

    @Test
    fun `skips return reasons dialog - (isReturnReasonsEnabled, listOfReturnReasons)`() = runTest {

        skipsReturnReasonDialogValues().forEach { (isReturnReasonsEnabled, listOfReturnReasons) ->
            // Given
            GiniBank.enableReturnReasons = isReturnReasonsEnabled

            val viewModel = createViewModel(returnReasons = listOfReturnReasons)

            viewModel.sideEffects.test {
                // When
                viewModel.deselectLineItem(mockk<SelectableLineItem>())

                // Then
                expectNoEvents()
            }
        }
    }

    private fun skipsReturnReasonDialogValues(): Array<Pair<Boolean, List<GiniCaptureReturnReason>>> =
        arrayOf(
            // isReturnReasonsEnabled, listOfReturnReasons
            true to emptyList(),
            false to returnReasonsFixture,
            false to emptyList()
        )

    private val extractionsWithOtherChargesFixture: Map<String, GiniCaptureSpecificExtraction> =
        mapOf(
            "amountToPay" to GiniCaptureSpecificExtraction(
                "amountToPay",
                "30.27:EUR",
                "amount",
                null,
                emptyList<GiniCaptureExtraction>()
            ),
            "other-charges-addon" to GiniCaptureSpecificExtraction(
                "other-charges-addon",
                "15.00:EUR",
                "amount",
                null,
                emptyList<GiniCaptureExtraction>()
            )
        )
    private val extractionsWithoutOtherChargesFixture: Map<String, GiniCaptureSpecificExtraction> =
        mapOf(
            "amountToPay" to GiniCaptureSpecificExtraction(
                "amountToPay",
                "15.27:EUR",
                "amount",
                null,
                emptyList<GiniCaptureExtraction>()
            )
        )
    private val compoundExtractionsFixture: Map<String, GiniCaptureCompoundExtraction> = mapOf(
        "lineItems" to GiniCaptureCompoundExtraction(
            "lineItems",
            listOf(
                mapOf(
                    "quantity" to GiniCaptureSpecificExtraction(
                        "quantity",
                        "1",
                        "numberic",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "artNumber" to GiniCaptureSpecificExtraction(
                        "artNumber",
                        "5c614a2f5d38b73cb5a2 4341",
                        "text",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "description" to GiniCaptureSpecificExtraction(
                        "description",
                        "Damen Yoga Leggings Fitness Hose Gym Fit- ness Sport bequeme Hose mit Tasche",
                        "text",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "baseGross" to GiniCaptureSpecificExtraction(
                        "baseGross",
                        "4.73:EUR",
                        "amount",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    )
                ),
                mapOf(
                    "quantity" to GiniCaptureSpecificExtraction(
                        "quantity",
                        "1",
                        "numberic",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "artNumber" to GiniCaptureSpecificExtraction(
                        "artNumber",
                        "5dcbb681 78638702870c 27e4",
                        "text",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "description" to GiniCaptureSpecificExtraction(
                        "description",
                        "Lässige Leggings mit Totenkopf- und Blu- men-Print für Damen Yoga Hose Leggings",
                        "text",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "baseGross" to GiniCaptureSpecificExtraction(
                        "baseGross",
                        "5.00:EUR",
                        "amount",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    )
                ),
                mapOf(
                    "quantity" to GiniCaptureSpecificExtraction(
                        "quantity",
                        "1",
                        "numberic",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "artNumber" to GiniCaptureSpecificExtraction(
                        "artNumber",
                        "5dd8de427ad86808ef3d 812f",
                        "text",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "description" to GiniCaptureSpecificExtraction(
                        "description",
                        "Eng anliegende Anti-Cellulite Kompression Slim Yogahose für Damen Sport schnell trocknende Hose mit hohem Bund",
                        "text",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    ),
                    "baseGross" to GiniCaptureSpecificExtraction(
                        "baseGross",
                        "5.54:EUR",
                        "amount",
                        null,
                        emptyList<GiniCaptureExtraction>()
                    )
                )
            )
        )
    )

    @Test
    fun `enables pay button - (extractions, compoundExtractions, deselectedLineItems)`() = runTest {
        enablesPayButtonValues().forEach { (extractions, compoundExtractions, deselectedLineItemIndexes) ->

            GiniBank.enableReturnReasons = false

            val viewModel = createViewModel(
                extractions = extractions,
                compoundExtractions = compoundExtractions
            )

            // When
            viewModel.start()
            deselectedLineItemIndexes.forEach { index ->
                viewModel.deselectLineItem(index)
            }

            // Then
            val footerDetails = viewModel.uiState.value.footerDetails
            assertThat(footerDetails).isNotNull()
            assertThat(footerDetails!!.buttonEnabled).isTrue()
        }
    }

    private fun enablesPayButtonValues(): Array<Triple<
            Map<String, GiniCaptureSpecificExtraction>,
            Map<String, GiniCaptureCompoundExtraction>,
            List<Int>>
            > {
        // extractions, compoundExtractions, deselectedLineItems
        return arrayOf(
            // with other charges and no selected line items
            Triple(
                extractionsWithOtherChargesFixture,
                compoundExtractionsFixture,
                listOf(0, 1, 2)
            ),
            // with other charges and one selected line item
            Triple(
                extractionsWithOtherChargesFixture,
                compoundExtractionsFixture,
                listOf(1, 2)
            ),
            // without other charges and one selected line item
            Triple(
                extractionsWithoutOtherChargesFixture,
                compoundExtractionsFixture,
                listOf(1, 2)
            ),
        )
    }

    @Test
    fun `disables pay button if total price is zero without selected line items`() = runTest {
        // Given
        GiniBank.enableReturnReasons = false

        val viewModel = createViewModel(
            extractions = extractionsWithoutOtherChargesFixture,
            compoundExtractions = compoundExtractionsFixture
        )

        // When
        viewModel.start()
        viewModel.deselectAllLineItems()

        // Then
        val footerDetails = viewModel.uiState.value.footerDetails
        assertThat(footerDetails).isNotNull()
        assertThat(footerDetails!!.buttonEnabled).isFalse()
    }

    @Test
    fun `disables pay button if total price is zero with selected line items`() = runTest {
        // Given
        GiniBank.enableReturnReasons = false

        val viewModel = createViewModel(
            extractions = extractionsWithoutOtherChargesFixture,
            compoundExtractions = compoundExtractionsFixture
        )

        // When
        viewModel.start()
        val lineItems = viewModel.uiState.value.lineItems
        // Leave only first item selected
        lineItems.forEachIndexed { index, selectableLineItem ->
            if (index != 0) {
                viewModel.deselectLineItem(selectableLineItem)
            }
        }
        // Set first item's price to 0
        viewModel.updateLineItem(
            lineItems[0].copy(
                lineItem = lineItems[0].lineItem.copy(
                    rawGrossPrice = "0.00:EUR"
                )
            )
        )

        // Then
        val footerDetails = viewModel.uiState.value.footerDetails
        assertThat(footerDetails).isNotNull()
        assertThat(footerDetails!!.buttonEnabled).isFalse()
    }
}
