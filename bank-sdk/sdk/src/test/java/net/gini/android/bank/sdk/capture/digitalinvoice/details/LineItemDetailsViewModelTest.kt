package net.gini.android.bank.sdk.capture.digitalinvoice.details

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import junitparams.JUnitParamsRunner
import junitparams.NamedParameters
import junitparams.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.digitalinvoice.LineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DecimalFormat
import java.util.Locale


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnitParamsRunner::class)
class LineItemDetailsViewModelTest {

    private val returnReasonsFixture = listOf(
        GiniCaptureReturnReason("1", mapOf("de" to "Foo", "en" to "Foo")),
        GiniCaptureReturnReason("2", mapOf("de" to "Bar", "en" to "Bar"))
    )

    private val defaultLocale: Locale = Locale.getDefault()

    private fun selectableLineItemFixture() = SelectableLineItem(
        selected = true,
        reason = null,
        addedByUser = false,
        lineItem = LineItem(
            id = "id1",
            description = "Foo",
            quantity = 1,
            rawGrossPrice = "12.00:EUR",
            origQuantity = 1,
            origRawGrossPrice = "12.00:EUR"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Locale.setDefault(defaultLocale)
        Dispatchers.resetMain()
    }

    @Test
    fun `shows return reasons dialog when enabled and has a return reasons list`() = runTest {
        // Given
        GiniBank.enableReturnReasons = true

        val viewModel = LineItemDetailsViewModel(
            selectableLineItem = selectableLineItemFixture(),
            returnReasons = returnReasonsFixture
        )

        viewModel.sideEffects.test {
            // When
            viewModel.deselectLineItem()

            // Then
            val sideEffect = awaitItem()
            assertThat(sideEffect)
                .isInstanceOf(LineItemDetailsSideEffect.ShowReturnReasonDialog::class.java)
            assertThat(
                (sideEffect as LineItemDetailsSideEffect.ShowReturnReasonDialog).reasons
            ).isEqualTo(returnReasonsFixture)
            expectNoEvents()
        }
    }

    @Test
    @Parameters(method = "skipsReturnReasonDialogValues")
    fun `skips return reasons dialog - (isReturnReasonsEnabled, listOfReturnReasons)`(
        isReturnReasonsEnabled: Boolean,
        listOfReturnReasons: List<GiniCaptureReturnReason>
    ) = runTest {
        // Given
        GiniBank.enableReturnReasons = isReturnReasonsEnabled

        val viewModel = LineItemDetailsViewModel(
            selectableLineItem = selectableLineItemFixture(),
            returnReasons = listOfReturnReasons
        )

        viewModel.sideEffects.test {
            // When
            viewModel.deselectLineItem()

            // Then
            expectNoEvents()
        }
        assertThat(viewModel.selectableLineItem.selected).isFalse()
        assertThat(viewModel.uiState.value.inputEnabled).isFalse()
        assertThat(viewModel.uiState.value.checkboxSelected).isFalse()
    }

    private fun skipsReturnReasonDialogValues(): Array<Any> = arrayOf(
        // isReturnReasonsEnabled, listOfReturnReasons
        arrayOf(true, emptyList<GiniCaptureReturnReason>()),
        arrayOf(false, returnReasonsFixture),
        arrayOf(false, emptyList<GiniCaptureReturnReason>())
    )

    @Test
    @Parameters(
        "-1, 1",
        "0, 1",
        "1, 1",
        "10, 10",
        "99998, 99998",
        "1000000, 99999"
    )
    fun `limits quantity to be between MIN_QUANTITY and MAX_QUANTITY - (quantity, validatedQuantity)`(
        quantity: Int,
        validatedQuantity: Int
    ) {
        // Given
        val viewModel = LineItemDetailsViewModel(
            selectableLineItem = selectableLineItemFixture()
        )

        // When
        viewModel.setQuantity(quantity)

        // Then
        assertThat(viewModel.selectableLineItem.lineItem.quantity).isEqualTo(validatedQuantity)
        assertThat(viewModel.uiState.value.quantity).isEqualTo(validatedQuantity)
    }

    @Test
    @Parameters("0, 1", "100000, 99999")
    fun `updates view if validated quantity is different from entered quantity - (quantity, validatedQuantity)`(
        quantity: Int,
        validatedQuantity: Int
    ) = runTest {
        // Given
        val viewModel = LineItemDetailsViewModel(
            selectableLineItem = selectableLineItemFixture()
        )

        viewModel.sideEffects.test {
            // When
            viewModel.setQuantity(quantity)

            // Then
            val sideEffect = awaitItem()
            assertThat(sideEffect)
                .isInstanceOf(LineItemDetailsSideEffect.UpdateQuantityField::class.java)
            assertThat(
                (sideEffect as LineItemDetailsSideEffect.UpdateQuantityField).quantity
            ).isEqualTo(validatedQuantity)
            expectNoEvents()
        }
    }

    @Test
    @Parameters(named = "paramsForLineItemNameValidation")
    fun `validates the line item name`(name: String, expectedValidationResult: String) {
        // Given
        val viewModel = LineItemDetailsViewModel(
            selectableLineItem = selectableLineItemFixture()
        )

        // When
        val validationResult = viewModel.validateLineItemName(name)

        // Then
        assertThat(validationResult).isEqualTo(expectedValidationResult.toBoolean())
    }

    @NamedParameters("paramsForLineItemNameValidation")
    fun paramsForLineItemNameValidation(): Any = arrayOf(
        arrayOf<Any>("", false),
        arrayOf<Any>(" ", false),
        arrayOf<Any>("  ", false),
        arrayOf<Any>(" a", true),
        arrayOf<Any>("1 ", true),
        arrayOf<Any>("foo", true),
    )

    @Test
    @Parameters(named = "paramsForLineItemGrossPriceValidation")
    fun `validates the line item price`(
        languageTag: String,
        grossPrice: String,
        expectedValidationResult: String
    ) {
        // Given
        Locale.setDefault(Locale.forLanguageTag(languageTag))

        val viewModel = LineItemDetailsViewModel(
            selectableLineItem = selectableLineItemFixture(),
            grossPriceFormat = DecimalFormat(GROSS_PRICE_FORMAT_PATTERN).apply {
                isParseBigDecimal = true
            }
        )

        // When
        val validationResult = viewModel.validateLineItemGrossPrice(grossPrice)

        // Then
        assertThat(validationResult).isEqualTo(expectedValidationResult.toBoolean())
    }

    @NamedParameters("paramsForLineItemGrossPriceValidation")
    fun paramsForLineItemGrossPriceValidation(): Any = arrayOf(
        arrayOf<Any>("en-EN", "", false),
        arrayOf<Any>("en-EN", " ", false),
        arrayOf<Any>("en-EN", "  ", false),
        arrayOf<Any>("en-EN", " a", false),
        arrayOf<Any>("en-EN", " 0", false),
        arrayOf<Any>("en-EN", "0000000", false),
        arrayOf<Any>("en-EN", "0.0", false),
        arrayOf<Any>("en-EN", "0.00", false),
        arrayOf<Any>("en-EN", "0.000000", false),
        arrayOf<Any>("en-EN", "0,000.00", false),
        arrayOf<Any>("en-EN", "000,000.00000", false),
        arrayOf<Any>("en-EN", "0.01", true),
        arrayOf<Any>("en-EN", "1", true),
        arrayOf<Any>("en-EN", "1.0", true),
        arrayOf<Any>("en-EN", "1.00", true),
        arrayOf<Any>("en-EN", "1,111.00", true),
        arrayOf<Any>("en-EN", "1,111,111.00", true),
        arrayOf<Any>("de-DE", "", false),
        arrayOf<Any>("de-DE", " ", false),
        arrayOf<Any>("de-DE", "  ", false),
        arrayOf<Any>("de-DE", " a", false),
        arrayOf<Any>("de-DE", " 0", false),
        arrayOf<Any>("de-DE", "0000000", false),
        arrayOf<Any>("de-DE", "0,0", false),
        arrayOf<Any>("de-DE", "0,00", false),
        arrayOf<Any>("de-DE", "0,000000", false),
        arrayOf<Any>("de-DE", "0.000,00", false),
        arrayOf<Any>("de-DE", "000.000,00000", false),
        arrayOf<Any>("de-DE", "0,01", true),
        arrayOf<Any>("de-DE", "1", true),
        arrayOf<Any>("de-DE", "1,0", true),
        arrayOf<Any>("de-DE", "1,00", true),
        arrayOf<Any>("de-DE", "1.111,00", true),
        arrayOf<Any>("de-DE", "1.111.111,00", true),
    )
}
