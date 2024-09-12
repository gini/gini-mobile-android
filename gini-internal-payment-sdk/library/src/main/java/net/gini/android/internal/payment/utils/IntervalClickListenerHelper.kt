package net.gini.android.internal.payment.utils

/**
 * Internal use only.
 *
 * @suppress
 */
abstract class IntervalClickListenerHelper {
    //Override if needed otherwise, keep the default values
    open var isEnabled: Boolean = true
    open var interval: Long = 500L
    //Override
    protected abstract var enabled: Runnable
}