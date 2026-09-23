package net.gini.android.bank.sdk.exampleapp.uitestsupport

import net.gini.android.capture.Document
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.network.AnalysisResult
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.network.Result
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.util.CancellationToken
import java.util.UUID

/**
 * Serves the canned responses for one [UiTestMockScenario]. See [UiTestMockBackend] for why this
 * exists and how it is wired in.
 *
 * Upload and delete succeed immediately, `analyze` returns the scenario's payload, and
 * `getConfiguration` returns the flags the test asked for. The uploaded bytes are ignored, so the
 * fixture document on screen is irrelevant to the outcome — that is the whole point.
 *
 * The optional interface methods (`getDocumentPages`, `getDocumentLayout`, `getFile`, `sendEvents`)
 * are deliberately left on their interface defaults, which return `null`. The features that use
 * them — the Skonto invoice preview, for instance — wrap the call in `runCatching {}` and degrade,
 * so a screen still opens without them. Do not implement them speculatively; add one only when a
 * test needs it.
 */
internal class UiTestMockNetworkService(
    private val scenario: UiTestMockScenario,
    private val clientConfiguration: UiTestMockClientConfiguration
) : GiniCaptureNetworkService {

    override fun upload(
        document: Document,
        callback: GiniCaptureNetworkCallback<Result, Error>
    ): CancellationToken {
        callback.success(Result(DOCUMENT_ID, DOCUMENT_FILENAME))
        return NoOpCancellationToken
    }

    override fun delete(
        giniApiDocumentId: String,
        callback: GiniCaptureNetworkCallback<Result, Error>
    ): CancellationToken {
        callback.success(Result(giniApiDocumentId, DOCUMENT_FILENAME))
        return NoOpCancellationToken
    }

    override fun analyze(
        giniApiDocumentIdRotationMap: LinkedHashMap<String, Int>,
        callback: GiniCaptureNetworkCallback<AnalysisResult, Error>
    ): CancellationToken {
        callback.success(
            AnalysisResult(
                DOCUMENT_ID,
                DOCUMENT_FILENAME,
                specificExtractions(),
                compoundExtractions(),
                emptyList()
            )
        )
        return NoOpCancellationToken
    }

    override fun sendFeedback(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        callback: GiniCaptureNetworkCallback<Void, Error>
    ) {
        callback.success(null)
    }

    override fun getConfiguration(
        callback: GiniCaptureNetworkCallback<Configuration, Error>
    ): CancellationToken {
        callback.success(
            Configuration(
                id = UUID.randomUUID(),
                clientID = CLIENT_ID,
                isUserJourneyAnalyticsEnabled = false,
                isSkontoEnabled = clientConfiguration.skontoEnabled,
                isReturnAssistantEnabled = clientConfiguration.returnAssistantEnabled,
                isTransactionDocsEnabled = false,
                isQrCodeEducationEnabled = false,
                isInstantPaymentEnabled = false,
                isEInvoiceEnabled = false,
                amplitudeApiKey = "",
                isSavePhotosLocallyEnabled = false,
                isAlreadyPaidHintEnabled = false,
                isPaymentDueHintEnabled = false,
                isUnsupportedQRCodeWarningEnabled = false,
                isPaymentScheduleHintEnabled = false,
                isCreditNoteHintEnabled = clientConfiguration.creditNoteHintEnabled
            )
        )
        return NoOpCancellationToken
    }

    override fun deleteGiniUserCredentials() = Unit

    override fun cleanup() = Unit

    // ── payloads ────────────────────────────────────────────────────────────────────────────────

    private fun specificExtractions(): Map<String, GiniCaptureSpecificExtraction> {
        val extractions = linkedMapOf(
            "iban" to extraction("iban", "DE74700500000000028273", "iban"),
            "paymentRecipient" to extraction("paymentRecipient", "UI Test Recipient GmbH", "text"),
            "paymentPurpose" to extraction("paymentPurpose", "Backend mock UI test", "text"),
            "amountToPay" to extraction("amountToPay", AMOUNT_TO_PAY, "amount")
        )
        if (scenario.isCreditNote) {
            // The exact value the SDK matches on — see BusinessDocType.fromString.
            extractions["businessDocType"] =
                extraction("businessDocType", "CreditNote", "text")
        }
        return extractions
    }

    private fun compoundExtractions(): Map<String, GiniCaptureCompoundExtraction> = when (scenario) {
        UiTestMockScenario.CREDIT_NOTE,
        UiTestMockScenario.INVOICE -> emptyMap()

        UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS -> mapOf("lineItems" to lineItems())
    }

    /**
     * One line item carrying every field `LineItemsValidator.validate` requires: `description`,
     * an int-parsable `quantity`, and a `baseGross` that `parsePriceString` accepts. `artNumber` is
     * not in the validator's list, but it is included so the row renders like a real one.
     */
    private fun lineItems(): GiniCaptureCompoundExtraction =
        GiniCaptureCompoundExtraction(
            "lineItems",
            listOf(
                linkedMapOf(
                    "description" to extraction("description", "UI test article", "text"),
                    "quantity" to extraction("quantity", "1", "number"),
                    "baseGross" to extraction("baseGross", LINE_ITEM_GROSS, "amount"),
                    "artNumber" to extraction("artNumber", "UITEST-1", "text")
                )
            )
        )

    private fun extraction(name: String, value: String, entity: String) =
        GiniCaptureSpecificExtraction(name, value, entity, null, emptyList())

    private object NoOpCancellationToken : CancellationToken {
        override fun cancel() = Unit
    }

    private companion object {
        const val DOCUMENT_ID = "ui-test-mock-document"
        const val DOCUMENT_FILENAME = "ui-test-mock-document.jpg"
        const val CLIENT_ID = "ui-test-mock-client"
        const val AMOUNT_TO_PAY = "42.00:EUR"
        const val LINE_ITEM_GROSS = "42.00:EUR"
    }
}

private val UiTestMockScenario.isCreditNote: Boolean
    get() = when (this) {
        UiTestMockScenario.CREDIT_NOTE,
        UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS -> true

        UiTestMockScenario.INVOICE -> false
    }
