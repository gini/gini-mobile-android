package net.gini.android.bank.sdk.exampleapp.uitestsupport

import android.os.Handler
import android.os.Looper
import net.gini.android.capture.Document
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.network.AnalysisResult
import net.gini.android.capture.network.Error
import net.gini.android.capture.network.GiniCaptureNetworkCallback
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.network.Result
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.util.CancellationToken
import java.util.UUID

/**
 * Serves the canned responses for one [UiTestMockScenario]. See [UiTestMockBackend] for why this
 * exists and how it is wired in.
 *
 * Upload and delete succeed immediately, `analyze` returns the scenario's payload (after
 * `analysisDelayMillis`, on the main thread like the default network service), and
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
    private val clientConfiguration: UiTestMockClientConfiguration,
    private val analysisDelayMillis: Long = 0,
    private val configurationFails: Boolean = false
) : GiniCaptureNetworkService {

    private val mainHandler = Handler(Looper.getMainLooper())

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
        val deliver = Runnable {
            if (scenario == UiTestMockScenario.ANALYSIS_ERROR) {
                callback.failure(Error(ANALYSIS_ERROR_MESSAGE))
                return@Runnable
            }
            callback.success(
                AnalysisResult(
                    DOCUMENT_ID,
                    DOCUMENT_FILENAME,
                    specificExtractions(),
                    compoundExtractions(),
                    emptyList()
                )
            )
        }
        if (analysisDelayMillis <= 0) {
            deliver.run()
            return NoOpCancellationToken
        }
        // Delivered on the main thread, like GiniCaptureDefaultNetworkService does. Cancelling
        // drops the pending result, so a screen the test closed never receives it.
        mainHandler.postDelayed(deliver, analysisDelayMillis)
        return object : CancellationToken {
            override fun cancel() = mainHandler.removeCallbacks(deliver)
        }
    }

    override fun sendFeedback(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        callback: GiniCaptureNetworkCallback<Void, Error>
    ) {
        // Recorded so a test can check what the SDK reports back, e.g. the chosen IBAN and that
        // `fraudSignals` is never part of the feedback. Copied: the SDK keeps working on its own
        // map. The values first: a test polls the compound map, so once that is set, both are.
        UiTestMockBackend.lastFeedbackExtractionValues = extractions.mapValues { it.value.value }
        UiTestMockBackend.lastFeedbackCompoundExtractions = compoundExtractions.toMap()
        callback.success(null)
    }

    override fun getConfiguration(
        callback: GiniCaptureNetworkCallback<Configuration, Error>
    ): CancellationToken {
        if (configurationFails) {
            callback.failure(Error(CONFIGURATION_ERROR_MESSAGE))
            return NoOpCancellationToken
        }
        callback.success(
            Configuration(
                id = UUID.randomUUID(),
                clientID = CLIENT_ID,
                isUserJourneyAnalyticsEnabled = false,
                isSkontoEnabled = clientConfiguration.skontoEnabled,
                isReturnAssistantEnabled = clientConfiguration.returnAssistantEnabled,
                isTransactionDocsEnabled = false,
                isQrCodeEducationEnabled = clientConfiguration.qrCodeEducationEnabled,
                isInstantPaymentEnabled = false,
                isEInvoiceEnabled = false,
                amplitudeApiKey = "",
                isSavePhotosLocallyEnabled = false,
                isAlreadyPaidHintEnabled = clientConfiguration.alreadyPaidHintEnabled,
                isPaymentDueHintEnabled = clientConfiguration.paymentDueHintEnabled,
                isUnsupportedQRCodeWarningEnabled = false,
                isPaymentScheduleHintEnabled = clientConfiguration.paymentScheduleHintEnabled,
                isCreditNoteHintEnabled = clientConfiguration.creditNoteHintEnabled,
                ingredientBrandScreens = clientConfiguration.ingredientBrandScreens,
                isFraudSignalsEnabled = clientConfiguration.fraudSignalsEnabled
            )
        )
        return NoOpCancellationToken
    }

    override fun deleteGiniUserCredentials() = Unit

    override fun cleanup() = Unit

    // ── payloads ────────────────────────────────────────────────────────────────────────────────

    private fun specificExtractions(): Map<String, GiniCaptureSpecificExtraction> = when {
        scenario == UiTestMockScenario.NO_RESULTS -> emptyMap()
        scenario.isFraudScenario -> fraudInvoiceExtractions()
        else -> invoiceExtractions()
    }

    /** The plain invoice, plus `businessDocType` for the credit note scenarios. */
    private fun invoiceExtractions(): Map<String, GiniCaptureSpecificExtraction> {
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
        UiTestMockScenario.INVOICE,
        UiTestMockScenario.NO_RESULTS,
        UiTestMockScenario.ANALYSIS_ERROR,
        UiTestMockScenario.FRAUD_SIGNALS_MISSING -> emptyMap()

        UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS -> mapOf("lineItems" to lineItems())

        UiTestMockScenario.FRAUD_QR_MISMATCH,
        UiTestMockScenario.FRAUD_SEVERAL_PRINTED,
        UiTestMockScenario.FRAUD_QR_WITHOUT_OTHER_PRINTED -> fraudSignals(listOf(qrSignal()))

        UiTestMockScenario.FRAUD_COUNTRY_MISMATCH -> fraudSignals(listOf(countrySignal()))

        UiTestMockScenario.FRAUD_COMBINED -> fraudSignals(listOf(qrSignal(), countrySignal()))

        UiTestMockScenario.FRAUD_UNKNOWN_CODE -> fraudSignals(listOf(unknownSignal()))
    }

    /**
     * The invoice behind every fraud scenario, shaped like the backend sends it: the `iban`
     * extraction is the QR code IBAN (the IBAN the backend delivers for payment), and its
     * candidates are every IBAN found on the document, the QR code IBAN included. The SDK takes
     * the printed IBANs from those candidates (`IBANMismatchArgsMapper`). `bic` belongs to the QR
     * code IBAN, so choosing a printed IBAN on the selection sheet must clear it.
     */
    private fun fraudInvoiceExtractions(): Map<String, GiniCaptureSpecificExtraction> = linkedMapOf(
        "iban" to GiniCaptureSpecificExtraction(
            "iban",
            UiTestMockFraudData.QR_IBAN,
            "iban",
            null,
            fraudIbanCandidates()
        ),
        "bic" to extraction("bic", UiTestMockFraudData.QR_BIC, "bic"),
        "paymentRecipient" to extraction("paymentRecipient", UiTestMockFraudData.SENDER, "text"),
        "paymentPurpose" to extraction("paymentPurpose", "Backend mock UI test", "text"),
        "amountToPay" to extraction("amountToPay", AMOUNT_TO_PAY, "amount")
    )

    private fun fraudIbanCandidates(): List<GiniCaptureExtraction> {
        val ibans = when (scenario) {
            UiTestMockScenario.FRAUD_QR_WITHOUT_OTHER_PRINTED -> listOf(UiTestMockFraudData.QR_IBAN)
            UiTestMockScenario.FRAUD_SEVERAL_PRINTED -> listOf(
                UiTestMockFraudData.QR_IBAN,
                UiTestMockFraudData.PRINTED_IBAN,
                UiTestMockFraudData.SECOND_PRINTED_IBAN
            )
            else -> listOf(UiTestMockFraudData.QR_IBAN, UiTestMockFraudData.PRINTED_IBAN)
        }
        return ibans.map { GiniCaptureExtraction(it, "iban", null) }
    }

    /** The `fraudSignals` compound extraction, one element map per signal. */
    private fun fraudSignals(
        signals: List<Map<String, GiniCaptureSpecificExtraction>>
    ): Map<String, GiniCaptureCompoundExtraction> =
        mapOf(
            UiTestMockFraudData.FRAUD_SIGNALS_KEY to
                GiniCaptureCompoundExtraction(UiTestMockFraudData.FRAUD_SIGNALS_KEY, signals)
        )

    private fun qrSignal(): Map<String, GiniCaptureSpecificExtraction> = linkedMapOf(
        "signalCode" to extraction("signalCode", "QR_PRINTED_IBAN_MISMATCH", "text"),
        "severity" to extraction("severity", "STOP", "text"),
        "iban" to extraction("iban", UiTestMockFraudData.QR_IBAN, "iban")
    )

    private fun countrySignal(): Map<String, GiniCaptureSpecificExtraction> = linkedMapOf(
        "signalCode" to extraction("signalCode", "IBAN_COUNTRY_MISMATCH", "text"),
        "severity" to extraction("severity", "CHECK", "text"),
        "iban" to extraction("iban", UiTestMockFraudData.QR_IBAN, "iban"),
        "creditorCountry" to extraction(
            "creditorCountry",
            UiTestMockFraudData.CREDITOR_COUNTRY,
            "text"
        )
    )

    /** A complete element whose code no SDK version knows, so only the code makes it unusable. */
    private fun unknownSignal(): Map<String, GiniCaptureSpecificExtraction> = linkedMapOf(
        "signalCode" to extraction("signalCode", "UI_TEST_UNKNOWN_SIGNAL", "text"),
        "severity" to extraction("severity", "STOP", "text"),
        "iban" to extraction("iban", UiTestMockFraudData.QR_IBAN, "iban")
    )

    /**
     * One line item carrying every field `LineItemsValidator.validate` requires: `description`,
     * an int-parsable `quantity`, and a `baseGross` that `parsePriceString` accepts. `artNumber` is
     * not in the validator's list, but it is included so the row renders like a real one.
     */
    private fun lineItems(): GiniCaptureCompoundExtraction =
        GiniCaptureCompoundExtraction(
            "lineItems",
            listOf(lineItem("UI test article", "UITEST-1"))
        )

    private fun lineItem(description: String, artNumber: String) = linkedMapOf(
        "description" to extraction("description", description, "text"),
        "quantity" to extraction("quantity", "1", "number"),
        "baseGross" to extraction("baseGross", LINE_ITEM_GROSS, "amount"),
        "artNumber" to extraction("artNumber", artNumber, "text")
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
        const val ANALYSIS_ERROR_MESSAGE = "UI test mock: analysis failed"
        const val CONFIGURATION_ERROR_MESSAGE = "UI test mock: /configurations failed"
    }
}

/**
 * The values of the fraud scenarios. Public so the instrumented tests assert against the same
 * constants the mock serves, instead of a second copy that could drift.
 */
object UiTestMockFraudData {
    /** The compound extraction key the SDK reads, see `FraudSignalsExtractions` (internal). */
    const val FRAUD_SIGNALS_KEY = "fraudSignals"

    /** The IBAN in the payment QR code; also the `iban` extraction and the signals' `iban`. */
    const val QR_IBAN = "SK2811000000002620154106"

    /** The IBAN printed on the document. */
    const val PRINTED_IBAN = "ES0800497139179024888238"

    /** A second printed IBAN, for [UiTestMockScenario.FRAUD_SEVERAL_PRINTED]. */
    const val SECOND_PRINTED_IBAN = "AT611904300234573201"

    /** The BIC of [QR_IBAN]. */
    const val QR_BIC = "TATRSKBX"

    /** The `paymentRecipient`, shown as the sender on the country and combined warnings. */
    const val SENDER = "Martin Braun Backmittel und Essenzen KG"

    /** The sender's country in the `IBAN_COUNTRY_MISMATCH` signal. */
    const val CREDITOR_COUNTRY = "DE"
}

private val UiTestMockScenario.isCreditNote: Boolean
    get() = when (this) {
        UiTestMockScenario.CREDIT_NOTE,
        UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS -> true

        UiTestMockScenario.INVOICE,
        UiTestMockScenario.NO_RESULTS,
        UiTestMockScenario.ANALYSIS_ERROR,
        UiTestMockScenario.FRAUD_QR_MISMATCH,
        UiTestMockScenario.FRAUD_COUNTRY_MISMATCH,
        UiTestMockScenario.FRAUD_COMBINED,
        UiTestMockScenario.FRAUD_UNKNOWN_CODE,
        UiTestMockScenario.FRAUD_SEVERAL_PRINTED,
        UiTestMockScenario.FRAUD_QR_WITHOUT_OTHER_PRINTED,
        UiTestMockScenario.FRAUD_SIGNALS_MISSING -> false
    }

/** Whether the scenario serves the fraud invoice of `fraudInvoiceExtractions`. */
private val UiTestMockScenario.isFraudScenario: Boolean
    get() = when (this) {
        UiTestMockScenario.FRAUD_QR_MISMATCH,
        UiTestMockScenario.FRAUD_COUNTRY_MISMATCH,
        UiTestMockScenario.FRAUD_COMBINED,
        UiTestMockScenario.FRAUD_UNKNOWN_CODE,
        UiTestMockScenario.FRAUD_SEVERAL_PRINTED,
        UiTestMockScenario.FRAUD_QR_WITHOUT_OTHER_PRINTED,
        UiTestMockScenario.FRAUD_SIGNALS_MISSING -> true

        UiTestMockScenario.CREDIT_NOTE,
        UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS,
        UiTestMockScenario.INVOICE,
        UiTestMockScenario.NO_RESULTS,
        UiTestMockScenario.ANALYSIS_ERROR -> false
    }
