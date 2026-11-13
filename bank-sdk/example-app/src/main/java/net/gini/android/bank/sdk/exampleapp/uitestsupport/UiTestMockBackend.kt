package net.gini.android.bank.sdk.exampleapp.uitestsupport

import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction

/**
 * UI-test support: lets an instrumented test replace the Gini API with canned responses.
 *
 * Ported from the iOS example app's `UITestMockBackend` (gini-mobile-ios PR #1250). It exists for
 * one reason: some acceptance criteria depend on the **backend** client-configuration flags and on
 * extraction shapes we have no document for. Against the real API those cases are either not
 * reproducible from a device (a server flag cannot be flipped by the app) or need a document that
 * happens to extract a particular combination. Both become ordinary tests once the responses are
 * canned.
 *
 * ## How it is wired
 *
 * `ConfigurationViewModel.buildBaseCaptureConfiguration` asks [isArmed] when it builds the
 * `CaptureConfiguration`, and passes [networkService] instead of the default one when a test has
 * armed it. `configureGiniBank()` runs on the photo-payment button click (see
 * `MainActivity.startGiniBankSdk`), not in `onCreate`, so a test can arm this from its body — the
 * same timing the other UI-test configurators rely on.
 *
 * In a normal app run nothing arms it, [scenario] stays `null`, and the app uses the real network
 * service. There is no build flavour or debug flag involved: the class is inert until an
 * instrumented test writes to it.
 *
 * ## Extending
 *
 * Add a case to [UiTestMockScenario] and give it a payload in `UiTestMockNetworkService`. Prefer
 * that over widening an existing scenario, so each test keeps a payload it fully controls.
 */
object UiTestMockBackend {

    /**
     * The scenario whose canned analysis result `analyze` delivers, or `null` when no test has
     * armed the mock — which is every normal app run.
     */
    @Volatile
    var scenario: UiTestMockScenario? = null

    /** The client configuration `getConfiguration` serves — the backend "frontend flags". */
    @Volatile
    var clientConfiguration: UiTestMockClientConfiguration = UiTestMockClientConfiguration()

    /**
     * How long `analyze` waits before it delivers the result. `0` delivers at once, as a mock
     * normally does; a test that must look at the Analysis screen (or the QR overlay) while the
     * analysis runs sets a delay so the screen stays up.
     */
    @Volatile
    var analysisDelayMillis: Long = 0

    /**
     * When `true`, `getConfiguration` fails instead of serving [clientConfiguration], like a
     * `/configurations` request that never reaches the backend. The SDK then keeps the values it
     * saved from the last successful request.
     */
    @Volatile
    var configurationFails: Boolean = false

    /**
     * The compound extractions of the last `sendFeedback` call, or `null` when no feedback was
     * sent since the mock was armed. A copy, so later changes to the SDK's map do not show here.
     *
     * A test reads it to check what the SDK reports back to the backend, for example that the
     * `fraudSignals` compound extraction is never part of the feedback.
     */
    @Volatile
    var lastFeedbackCompoundExtractions: Map<String, GiniCaptureCompoundExtraction>? = null

    /**
     * The specific extractions of the last `sendFeedback` call as name → value, or `null` when no
     * feedback was sent since the mock was armed.
     *
     * Only the values are kept, not the extraction objects: `GiniCaptureExtraction.value` is
     * mutable, so a snapshot of the strings cannot change after it was recorded. A test reads it
     * to check which IBAN and BIC the SDK reports back, for example after an IBAN was chosen on
     * the fraud warning's selection sheet.
     *
     * Written before [lastFeedbackCompoundExtractions] in the same call, so once that one is set,
     * this one is too.
     */
    @Volatile
    var lastFeedbackExtractionValues: Map<String, String>? = null

    val isArmed: Boolean
        get() = scenario != null

    /** Arms the mock. Called from instrumented tests only. */
    fun arm(
        scenario: UiTestMockScenario,
        clientConfiguration: UiTestMockClientConfiguration = UiTestMockClientConfiguration(),
        analysisDelayMillis: Long = 0,
        configurationFails: Boolean = false
    ) {
        this.scenario = scenario
        this.clientConfiguration = clientConfiguration
        this.analysisDelayMillis = analysisDelayMillis
        this.configurationFails = configurationFails
        lastFeedbackExtractionValues = null
        lastFeedbackCompoundExtractions = null
    }

    /**
     * Disarms the mock and restores the defaults. Tests must call this in teardown — the object is
     * process-wide, and a leaked scenario would silently feed canned data to the next test.
     */
    fun disarm() {
        scenario = null
        clientConfiguration = UiTestMockClientConfiguration()
        analysisDelayMillis = 0
        configurationFails = false
        lastFeedbackExtractionValues = null
        lastFeedbackCompoundExtractions = null
    }

    /**
     * The network service serving [scenario]. Only valid while [isArmed]; the caller checks that
     * first.
     */
    fun networkService(): GiniCaptureNetworkService =
        UiTestMockNetworkService(
            scenario = requireNotNull(scenario) { "UiTestMockBackend is not armed" },
            clientConfiguration = clientConfiguration,
            analysisDelayMillis = analysisDelayMillis,
            configurationFails = configurationFails
        )
}

/**
 * What the mocked backend claims the uploaded document is.
 *
 * The names say what the *extractions* contain, not what the image on screen shows — the mock
 * ignores the uploaded bytes entirely, so any fixture document can drive any scenario.
 */
enum class UiTestMockScenario {

    /** A credit note: payment fields plus `businessDocType = CreditNote`. No compound extractions. */
    CREDIT_NOTE,

    /**
     * A credit note that *also* carries `lineItems`. Without the credit note warning this would
     * open the Return Assistant, which is what makes it the interesting case: the warning path
     * strips the compound extractions.
     */
    CREDIT_NOTE_WITH_LINE_ITEMS,

    /** An ordinary invoice: payment fields, no `businessDocType`. */
    INVOICE,

    /** The analysis succeeds but finds nothing, so the SDK opens its No Results screen. */
    NO_RESULTS,

    /** The analysis request fails, so the SDK opens its Error screen. */
    ANALYSIS_ERROR,

    /**
     * An invoice whose `fraudSignals` compound extraction holds only a `QR_PRINTED_IBAN_MISMATCH`
     * signal (severity STOP). The `iban` extraction is the QR code IBAN, and its candidates add
     * one other printed IBAN, so the SDK shows the QR-vs-printed IBAN mismatch warning when the
     * `fraudSignalsEnabled` flag is on.
     */
    FRAUD_QR_MISMATCH,

    /**
     * An invoice whose `fraudSignals` holds only an `IBAN_COUNTRY_MISMATCH` signal (severity
     * CHECK, creditor country DE) for a Slovak IBAN, so the SDK shows the country mismatch
     * warning.
     */
    FRAUD_COUNTRY_MISMATCH,

    /** Both signals of [FRAUD_QR_MISMATCH] and [FRAUD_COUNTRY_MISMATCH]: the combined warning. */
    FRAUD_COMBINED,

    /**
     * `fraudSignals` with one element whose `signalCode` this SDK version does not know. The SDK
     * skips unknown codes, so no warning is shown and the extractions are handed on.
     */
    FRAUD_UNKNOWN_CODE,

    /**
     * Like [FRAUD_QR_MISMATCH], but the `iban` candidates hold two printed IBANs besides the QR
     * code IBAN, so the warning and the selection sheet list three IBANs.
     */
    FRAUD_SEVERAL_PRINTED,

    /**
     * A `QR_PRINTED_IBAN_MISMATCH` signal while the `iban` candidates hold only the QR code IBAN.
     * With no other printed IBAN the SDK cannot explain the mismatch, so it drops the signal and
     * shows no warning.
     */
    FRAUD_QR_WITHOUT_OTHER_PRINTED,

    /**
     * The same invoice extractions as the fraud scenarios (QR code IBAN, printed candidate, BIC,
     * sender), but no `fraudSignals` compound extraction at all. No warning.
     */
    FRAUD_SIGNALS_MISSING,
}

/**
 * The backend client-configuration flags the mock serves from `getConfiguration`.
 *
 * These are the server-side gates. The credit note warning needs `creditNoteHintEnabled` here AND
 * the SDK-side flag; `returnAssistantEnabled` / `skontoEnabled` likewise gate their features, so
 * [UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS] needs `returnAssistantEnabled` on. `skontoEnabled`
 * is here so a test can turn Skonto OFF and isolate the Return Assistant branch of
 * `CaptureFlowFragment.processOnFinishedResultSuccessState`.
 *
 * `ingredientBrandScreens` switches the Gini ingredient brand (the Powered by Gini badge and the
 * Gini loading indicator); empty means off. `qrCodeEducationEnabled` switches both the invoice
 * education on the Analysis screen and the QR code education on the camera screen. Both default to
 * off, so a test that does not ask for them sees neither.
 *
 * `fraudSignalsEnabled` gates the fraud prevention (IBAN mismatch) warning: with it off the SDK
 * ignores the `fraudSignals` compound extraction. `alreadyPaidHintEnabled`,
 * `paymentDueHintEnabled` and `paymentScheduleHintEnabled` are the server-side gates of the other
 * Analysis screen hints. All four default to off, so existing tests see no change.
 */
data class UiTestMockClientConfiguration(
    val creditNoteHintEnabled: Boolean = true,
    val returnAssistantEnabled: Boolean = true,
    val skontoEnabled: Boolean = true,
    val ingredientBrandScreens: Set<String> = emptySet(),
    val qrCodeEducationEnabled: Boolean = false,
    val fraudSignalsEnabled: Boolean = false,
    val alreadyPaidHintEnabled: Boolean = false,
    val paymentDueHintEnabled: Boolean = false,
    val paymentScheduleHintEnabled: Boolean = false,
)
