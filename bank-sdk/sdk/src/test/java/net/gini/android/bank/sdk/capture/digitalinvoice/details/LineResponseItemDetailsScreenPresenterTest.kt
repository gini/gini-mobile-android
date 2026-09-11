package net.gini.android.bank.sdk.capture.digitalinvoice.details

import com.google.common.truth.Truth.assertThat
import io.mockk.confirmVerified
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import junitparams.JUnitParamsRunner
import junitparams.NamedParameters
import junitparams.Parameters
import net.gini.android.bank.sdk.capture.digitalinvoice.LineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DecimalFormat
import java.util.*


@RunWith(JUnitParamsRunner::class)
class LineResponseItemDetailsScreenPresenterTest {

    private val defaultLocale: Locale = Locale.getDefault()

    fun teardown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `deselects the line item and disables input`() {
        // Given
        val view: LineItemDetailsScreenContract.View = mockk(relaxed = true)

        val selectableLineItem = SelectableLineItem(
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

        val presenter = LineItemDetailsScreenPresenter(
            activity = mockk(),
            view = view,
            selectableLineItem = selectableLineItem
        )

        // When
        presenter.deselectLineItem()

        // Then
        assertThat(presenter.selectableLineItem.selected).isFalse()
        excludeRecords {
            view.setPresenter(any())
            view.showCheckbox(any(), any(), any())
            view.disableSaveButton()
            view.enableSaveButton()
        }
        verify(exactly = 1) { view.disableInput() }
        confirmVerified(view)
    }

    @Test
    @Parameters(
        "-1, 1",
        "0, 1",
        "1, 1",
        "10, 10",
        "99998, 99998",
        "1000000, 99999"
    )
    fun `limits quantity to be between MIN_QUANTITY and MAX_QUANTITY - (quantity, validatedQuantity)`(quantity: Int, validatedQuantity: Int) {
        // Given
        val view: LineItemDetailsScreenContract.View = mockk(relaxed = true)

        val selectableLineItem = SelectableLineItem(
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

        val presenter = LineItemDetailsScreenPresenter(
            activity = mockk(),
            view = view,
            selectableLineItem = selectableLineItem
        )

        // When
        presenter.setQuantity(quantity)

        // Then
        assertThat(presenter.selectableLineItem.lineItem.quantity).isEqualTo(validatedQuantity)
    }

    @Test
    @Parameters("0, 1", "100000, 99999")
    fun `updates view if validated quantity is different from entered quantity - (quantity, validatedQuantity)`(quantity: Int, validatedQuantity: Int) {
        // Given
        val view: LineItemDetailsScreenContract.View = mockk(relaxed = true)

        val selectableLineItem = SelectableLineItem(
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

        val presenter = LineItemDetailsScreenPresenter(
            activity = mockk(),
            view = view,
            selectableLineItem = selectableLineItem
        )

        // When
        presenter.setQuantity(quantity)

        // Then
        excludeRecords {
            view.setPresenter(any())
            view.disableInput()
            view.showCheckbox(any(), any(), any())
            view.showTotalGrossPrice(any(), any())
            view.disableSaveButton()
            view.enableSaveButton()
        }
        verify(exactly = 1) { view.showQuantity(eq(validatedQuantity)) }
        confirmVerified(view)
    }

    @Test
    @Parameters(named = "paramsForLineItemNameValidation")
    fun `validates the line item name`(name: String, expectedValidationResult: String) {
        // Given
        val view: LineItemDetailsScreenContract.View = mockk(relaxed = true)

        val presenter = LineItemDetailsScreenPresenter(
            activity = mockk(),
            view = view,
            selectableLineItem = mockk(relaxed = true)
        )

        // When
        val validationResult = presenter.validateLineItemName(name)

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
    fun `validates the line item price`(languageTag: String, grossPrice: String, expectedValidationResult: String) {
        // Given
        Locale.setDefault(Locale.forLanguageTag(languageTag))

        val view: LineItemDetailsScreenContract.View = mockk(relaxed = true)

        val presenter = LineItemDetailsScreenPresenter(
            activity = mockk(),
            view = view,
            selectableLineItem = mockk(relaxed = true),
            grossPriceFormat = DecimalFormat(GROSS_PRICE_FORMAT_PATTERN).apply { isParseBigDecimal = true }
        )

        // When
        val validationResult = presenter.validateLineItemGrossPrice(grossPrice)

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