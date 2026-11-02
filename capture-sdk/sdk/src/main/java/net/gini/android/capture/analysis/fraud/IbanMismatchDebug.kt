package net.gini.android.capture.analysis.fraud

import androidx.annotation.RestrictTo

/**
 * Development switch for the IBAN mismatch warning, in the spirit of
 * [net.gini.android.capture.GiniCaptureDebug]: while [enabled] the Analysis screen shows the
 * warning with [sampleArgs] after every successful analysis, ahead of the other warnings, so the
 * screen can be exercised without a document that carries the fraud signals.
 *
 * Off by default and not reset by `GiniCapture.cleanup()`. Public because the example app is a
 * separate Gradle module and Kotlin `internal` does not cross module boundaries; integrators
 * have no reason to call it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object IbanMismatchDebug {

    /** When `true` the Analysis screen shows the warning before handing the extractions on. */
    @JvmStatic
    var enabled: Boolean = false

    /** The combined case, because it exercises every part of the screen. */
    internal fun sampleArgs(): IbanMismatchArgs = IbanMismatchArgs(
        type = IbanMismatchType.COMBINED,
        qrIban = "PL12100001110100100012345678",
        printedIban = "DE89370400440532013000",
        sender = "Elektro Brandt GmbH · Germany",
        qrIbanHint = "From Poland, not the sender's country",
    )
}
