package net.gini.android.capture.analysis

/**
 * Wrapper for one-shot events exposed through `LiveData`.
 *
 * The content is delivered to the first consumer which calls [getContentIfNotHandled]; subsequent
 * calls (e.g. re-delivery after a configuration change) return `null`.
 *
 * Internal use only.
 */
internal class ConsumableEvent<out T>(private val content: T) {

    var hasBeenHandled: Boolean = false
        private set

    /**
     * Returns the content and marks the event as handled or `null` if it was already handled.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content without marking the event as handled.
     */
    fun peekContent(): T = content
}
