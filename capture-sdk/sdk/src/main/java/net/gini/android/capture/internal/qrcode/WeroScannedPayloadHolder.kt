package net.gini.android.capture.internal.qrcode

import java.util.concurrent.atomic.AtomicReference

/**
 * Single-slot carrier for the raw scanned QR payload of a Wero candidate between
 * `CameraFragmentImpl.handlePaymentQRCodeData` (writer) and the `/analysis`-success callback at
 * `CameraFragmentImpl.analyzeQRCode` (reader). The camera UI serializes QR scans —
 * `isPaymentQRCodeDetectionInProgress()` blocks re-entry until the current popup hides at the
 * end of the round-trip — so a single [AtomicReference] is safe.
 */
internal object WeroScannedPayloadHolder {

    private val reference = AtomicReference<String?>(null)

    fun set(payload: String) {
        reference.set(payload)
    }

    fun consume(): String? = reference.getAndSet(null)
}
