package net.gini.android.capture.internal.provider

import java.util.concurrent.atomic.AtomicReference

/**
 * Pins the unsupported QR code warning type for the duration of one capture session.
 *
 * The warning type must not change while a session is running: it is decided once — by the camera
 * screen when the first unsupported QR code warning is shown (see
 * `CameraFragmentExtension.isUnsupportedQRCodeWarningEnabled`) — and every later caller observes
 * that same decision. A configuration change received after the first warning was shown therefore
 * only takes effect in the next session.
 *
 * The session boundary is the lifetime of `GiniCaptureViewModel`, which calls [reset] when it is
 * cleared. This class is a process-wide Koin singleton, so without the reset a pinned value would
 * leak into the next capture session.
 */
internal class UnsupportedQrWarningSessionPin {

    private val pinned = AtomicReference<Boolean?>(null)

    /**
     * Returns the pinned value, computing and pinning it with [compute] if nothing has been
     * pinned yet. Only the first caller's value wins: [compute] may still be invoked by a losing
     * concurrent caller, but its result is discarded and the already-pinned value is returned, so
     * concurrent callers all observe the same pinned result. The trailing `?: candidate` fallback
     * only applies if [reset] runs concurrently with a pin attempt — it cannot occur in practice
     * because [reset] is called at the session boundary, after which no caller pins anymore.
     */
    fun pinIfAbsent(compute: () -> Boolean): Boolean {
        pinned.get()?.let { return it }
        val candidate = compute()
        return if (pinned.compareAndSet(null, candidate)) candidate else pinned.get() ?: candidate
    }

    fun reset() {
        pinned.set(null)
    }
}
