package net.gini.android.bank.sdk.capture.digitalinvoice

import io.mockk.confirmVerified
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.util.OncePerInstallEventStore
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(JUnitParamsRunner::class)
class DigitalInvoiceScreenPresenterTest {

    private val returnReasonsFixture = listOf(
        GiniCaptureReturnReason("1", mapOf("de" to "Foo", "en" to "Foo")),
        GiniCaptureReturnReason("2", mapOf("de" to "Bar", "en" to "Bar"))
    )

    @Test
    fun `shows return reasons dialog when enabled and has a return reasons list`() {
        // Given
        GiniBank.enableReturnReasons = true

        val view: DigitalInvoiceScreenContract.View = mockk(relaxed = true)

        val presenter = DigitalInvoiceScreenPresenter(
            activity = mockk(),
            view = view,
            returnReasons = returnReasonsFixture,
            savedInstanceBundle = null,
            oncePerInstallEventStore = mockk(),
            simpleBusEventStore = mockk()
        )

        // When
        presenter.deselectLineItem(mockk())

        // Then
        excludeRecords { view.setPresenter(any()) }
        verify { view.showReturnReasonDialog(any(), any()) }
        confirmVerified(view)
    }

    @Test
    @Parameters(method = "skipsReturnReasonDialogValues")
    fun `skips return reasons dialog - (isReturnReasonsEnabled, listOfReturnReasons)`(isReturnReasonsEnabled: Boolean, listOfReturnReasons: List<GiniCaptureReturnReason>) {
        // Given
        GiniBank.enableReturnReasons = isReturnReasonsEnabled

        val view: DigitalInvoiceScreenContract.View = mockk(relaxed = true)

        val presenter = DigitalInvoiceScreenPresenter(
            activity = mockk(),
            view = view,
            returnReasons = listOfReturnReasons,
            savedInstanceBundle = null,
            oncePerInstallEventStore = mockk(relaxed = true),
            simpleBusEventStore = mockk()
        )

        // When
        presenter.deselectLineItem(mockk())

        // Then
        excludeRecords {
            view.setPresenter(any())
            view.showLineItems(any(),any())
            view.updateFooterDetails(any())
            view.showAddons(any())
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