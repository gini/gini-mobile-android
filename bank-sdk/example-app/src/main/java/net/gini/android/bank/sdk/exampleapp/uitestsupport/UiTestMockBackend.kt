package net.gini.android.bank.sdk.exampleapp.uitestsupport

import net.gini.android.capture.network.GiniCaptureNetworkService

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

    val isArmed: Boolean
        get() = scenario != null

    /** Arms the mock. Called from instrumented tests only. */
    fun arm(
        scenario: UiTestMockScenario,
        clientConfiguration: UiTestMockClientConfiguration = UiTestMockClientConfiguration()
    ) {
        this.scenario = scenario
        this.clientConfiguration = clientConfiguration
    }

    /**
     * Disarms the mock and restores the defaults. Tests must call this in teardown — the object is
     * process-wide, and a leaked scenario would silently feed canned data to the next test.
     */
    fun disarm() {
        scenario = null
        clientConfiguration = UiTestMockClientConfiguration()
    }

    /**
     * The network service serving [scenario]. Only valid while [isArmed]; the caller checks that
     * first.
     */
    fun networkService(): GiniCaptureNetworkService =
        UiTestMockNetworkService(
            scenario = requireNotNull(scenario) { "UiTestMockBackend is not armed" },
            clientConfiguration = clientConfiguration
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
}

/**
 * The backend client-configuration flags the mock serves from `getConfiguration`.
 *
 * These are the server-side gates. The credit note warning needs `creditNoteHintEnabled` here AND
 * the SDK-side flag; `returnAssistantEnabled` / `skontoEnabled` likewise gate their features, so
 * [UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS] needs `returnAssistantEnabled` on. `skontoEnabled`
 * is here so a test can turn Skonto OFF and isolate the Return Assistant branch of
 * `CaptureFlowFragment.processOnFinishedResultSuccessState`.
 */
data class UiTestMockClientConfiguration(
    val creditNoteHintEnabled: Boolean = true,
    val returnAssistantEnabled: Boolean = true,
    val skontoEnabled: Boolean = true,
)
