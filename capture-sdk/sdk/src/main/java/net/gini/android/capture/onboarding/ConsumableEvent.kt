package net.gini.android.capture.onboarding

/**
 * Internal use only.
 *
 * Wrapper for one-shot events exposed via [androidx.lifecycle.LiveData]. The content is consumed
 * on the first call to [getContentIfNotHandled] and subsequent calls return `null`, ensuring the
 * event is handled only once (e.g. it won't fire again when observers are re-attached).
 *
 * @suppress
 */
internal class ConsumableEvent<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? =
        if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}
