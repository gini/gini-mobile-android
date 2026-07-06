package net.gini.android.capture.internal.provider

import java.util.concurrent.atomic.AtomicReference

/**
 * Pins the unsupported QR code warning type for the duration of one capture session.
 *
 * The warning type must not change while a session is running: it is decided once — by whichever
 * caller of [pinIfAbsent] comes first (the configuration observer when the persisted configuration
 * is loaded, or the camera screen when the first unsupported QR code is scanned) — and every later
 * caller observes that same decision. A configuration change received mid-session therefore only
 * takes effect in the next session.
 *
 * The session boundary is the lifetime of `GiniCaptureViewModel`, which calls [reset] when it is
 * cleared. This class is a process-wide Koin singleton, so without the reset a pinned value would
 * leak into the next capture session.
 */
internal class UnsupportedQrWarningSessionPin {

    private val pinned = AtomicReference<Boolean?>(null)

    /**
     * Returns the pinned value, computing and pinning it with [compute] if nothing has been
     * pinned yet. Only the first caller's value wins; concurrent callers all observe the same
     * pinned result.
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
