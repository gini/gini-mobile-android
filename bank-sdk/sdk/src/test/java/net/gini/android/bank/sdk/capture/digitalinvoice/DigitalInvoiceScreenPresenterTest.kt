package net.gini.android.bank.sdk.capture.digitalinvoice

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.di.BankSdkIsolatedKoinContext
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DigitalInvoiceScreenPresenterTest {

    private val returnReasonsFixture = listOf(
        GiniCaptureReturnReason("1", mapOf("de" to "Foo", "en" to "Foo")),
        GiniCaptureReturnReason("2", mapOf("de" to "Bar", "en" to "Bar"))
    )

    @Before
    fun setUp() {
        BankSdkIsolatedKoinContext.init(InstrumentationRegistry.getInstrumentation().context)
    }

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
        presenter.deselectLineItem(mockk<SelectableLineItem>())

        // Then
        excludeRecords { view.setPresenter(any()) }
        verify { view.showReturnReasonDialog(any(), any()) }
        confirmVerified(view)
    }

    @Test
    fun `skips return reasons dialog - (isReturnReasonsEnabled, listOfReturnReasons)`() {

        skipsReturnReasonDialogValues().forEach { (isReturnReasonsEnabled, listOfReturnReasons) ->
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
            presenter.deselectLineItem(mockk<SelectableLineItem>())

            // Then
            excludeRecords {
                view.setPresenter(any())
                view.showLineItems(any(), any())
                view.updateFooterDetails(any())
                view.showAddons(any())
            }
            verify(exactly = 0) { view.showReturnReasonDialog(any(), any()) }
            confirmVerified(view)
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
    fun `enables pay button - (extractions, compoundExtractions, deselectedLineItems)`() {
        enablesPayButtonValues().forEach { (extractions, compoundExtractions, deselectedLineItemIndexes) ->

            GiniBank.enableReturnReasons = false

            val view: DigitalInvoiceScreenContract.View = mockk(relaxed = true)
            val footerDetailsSlot = slot<DigitalInvoiceScreenContract.FooterDetails>()
            every { view.updateFooterDetails(capture(footerDetailsSlot)) } just Runs

            val presenter = DigitalInvoiceScreenPresenter(
                activity = mockk(),
                view = view,
                extractions = extractions,
                compoundExtractions = compoundExtractions,
                savedInstanceBundle = null,
                oncePerInstallEventStore = mockk(relaxed = true),
                simpleBusEventStore = mockk(relaxed = true)
            )

            // When
            presenter.start()
            deselectedLineItemIndexes.forEach { index ->
                presenter.deselectLineItem(index)
            }

            // Then
            excludeRecords {
                view.setPresenter(any())
                view.showLineItems(any(), any())
                view.showAddons(any())
                view.animateListScroll()
                view.showOnboarding()
            }
            verify { view.updateFooterDetails(any()) }
            confirmVerified(view)

            assertThat(footerDetailsSlot.captured.buttonEnabled).isTrue()
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
    fun `disables pay button if total price is zero without selected line items`() {
        // Given
        GiniBank.enableReturnReasons = false

        val view: DigitalInvoiceScreenContract.View = mockk(relaxed = true)
        val footerDetailsSlot = slot<DigitalInvoiceScreenContract.FooterDetails>()
        every { view.updateFooterDetails(capture(footerDetailsSlot)) } just Runs

        val presenter = DigitalInvoiceScreenPresenter(
            activity = mockk(),
            view = view,
            extractions = extractionsWithoutOtherChargesFixture,
            compoundExtractions = compoundExtractionsFixture,
            savedInstanceBundle = null,
            oncePerInstallEventStore = mockk(relaxed = true),
            simpleBusEventStore = mockk(relaxed = true)
        )

        // When
        presenter.start()
        presenter.deselectAllLineItems()

        // Then
        excludeRecords {
            view.setPresenter(any())
            view.showLineItems(any(), any())
            view.showAddons(any())
            view.animateListScroll()
            view.showOnboarding()
        }
        verify { view.updateFooterDetails(any()) }
        confirmVerified(view)

        assertThat(footerDetailsSlot.captured.buttonEnabled).isFalse()
    }

    @Test
    fun `disables pay button if total price is zero with selected line items`() {
        // Given
        GiniBank.enableReturnReasons = false

        val view: DigitalInvoiceScreenContract.View = mockk(relaxed = true)

        val footerDetailsSlot = slot<DigitalInvoiceScreenContract.FooterDetails>()
        every { view.updateFooterDetails(capture(footerDetailsSlot)) } just Runs

        val lineItemsSlot = slot<List<SelectableLineItem>>()
        every { view.showLineItems(capture(lineItemsSlot), any()) } just Runs

        val presenter = DigitalInvoiceScreenPresenter(
            activity = mockk(),
            view = view,
            extractions = extractionsWithoutOtherChargesFixture,
            compoundExtractions = compoundExtractionsFixture,
            savedInstanceBundle = null,
            oncePerInstallEventStore = mockk(relaxed = true),
            simpleBusEventStore = mockk(relaxed = true)
        )

        // When
        presenter.start()
        // Leave only first item selected
        lineItemsSlot.captured.forEachIndexed { index, selectableLineItem ->
            if (index != 0) {
                presenter.deselectLineItem(selectableLineItem)
            }
        }
        // Set first item's price to 0
        presenter.updateLineItem(
            lineItemsSlot.captured[0].copy(
                lineItem = lineItemsSlot.captured[0].lineItem.copy(
                    rawGrossPrice = "0.00:EUR"
                )
            )
        )

        // Then
        excludeRecords {
            view.setPresenter(any())
            view.showLineItems(any(), any())
            view.showAddons(any())
            view.animateListScroll()
            view.showOnboarding()
        }
        verify { view.updateFooterDetails(any()) }
        confirmVerified(view)

        assertThat(footerDetailsSlot.captured.buttonEnabled).isFalse()
    }
}