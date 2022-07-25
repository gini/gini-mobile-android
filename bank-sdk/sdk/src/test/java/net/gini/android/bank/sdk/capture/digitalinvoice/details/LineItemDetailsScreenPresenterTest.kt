package net.gini.android.bank.sdk.capture.digitalinvoice.details

import com.google.common.truth.Truth.assertThat
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.digitalinvoice.LineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(JUnitParamsRunner::class)
class LineItemDetailsScreenPresenterTest {

    private val returnReasonsFixture = listOf(
        GiniCaptureReturnReason("1", mapOf("de" to "Foo", "en" to "Foo")),
        GiniCaptureReturnReason("2", mapOf("de" to "Bar", "en" to "Bar"))
    )

    @Test
    fun `shows return reasons dialog when enabled and has a return reasons list`() {
        // Given
        GiniBank.enableReturnReasons = true

        val view: LineItemDetailsScreenContract.View = mockk(relaxed = true)

        val presenter = LineItemDetailsScreenPresenter(
            activity = mockk(),
            view = view,
            selectableLineItem = mockk<SelectableLineItem>().apply {
                every { selected } returns true
                every { copy() } returns this
            },
            returnReasons = returnReasonsFixture
        )

        // When
        presenter.deselectLineItem()

        // Then
        excludeRecords { view.setPresenter(any()) }
        verify { view.showReturnReasonDialog(any(), any()) }
        confirmVerified(view)
    }

    @Test
    @Parameters(method = "skipsReturnReasonDialogValues")
    fun `skips return reasons dialog - (isReturnReasonsEnabled, listOfReturnReasons)`(
        isReturnReasonsEnabled: Boolean,
        listOfReturnReasons: List<GiniCaptureReturnReason>
    ) {
        // Given
        GiniBank.enableReturnReasons = isReturnReasonsEnabled

        val view: LineItemDetailsScreenContract.View = mockk(relaxed = true)

        val presenter = LineItemDetailsScreenPresenter(
            activity = mockk(),
            view = view,
            selectableLineItem = mockk<SelectableLineItem>(relaxed = true).apply {
                every { selected } returns true
                every { copy() } returns this
            },
            returnReasons = listOfReturnReasons
        )

        // When
        presenter.deselectLineItem()

        // Then
        excludeRecords {
            view.setPresenter(any())
            view.disableInput()
            view.showCheckbox(true, 0, true)
            view.disableSaveButton()
        }
        verify(exactly = 0) { view.showReturnReasonDialog(any(), any()) }
        confirmVerified(view)
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
}