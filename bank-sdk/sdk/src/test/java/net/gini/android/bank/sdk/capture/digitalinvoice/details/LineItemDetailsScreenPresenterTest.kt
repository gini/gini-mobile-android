package net.gini.android.bank.sdk.capture.digitalinvoice.details

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import net.gini.android.bank.sdk.GiniBank
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
}