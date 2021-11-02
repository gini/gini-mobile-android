package net.gini.android.bank.sdk.capture.util

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory

internal class SimpleBusEventStore(context: Context) {
    private val mSharedPreferences: SharedPreferences =
        context.getSharedPreferences(APP_SIMPLE_EVENTS, Context.MODE_PRIVATE)

    fun saveEvent(event: BusEvent, value: Boolean = true) {
        mSharedPreferences.edit {
            putBoolean(event.name, value)
        }
        LOG.debug("Saved event {}", event.name)
    }


    fun observeChange(event: BusEvent): Flow<Boolean> = mSharedPreferences.observeKey(event.name, false)

    @VisibleForTesting
    fun clearEvent(event: BusEvent) {
        mSharedPreferences.edit {
            remove(event.name)
        }
        LOG.debug("Cleared event {}", event.name)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SimpleBusEventStore::class.java)
        private const val APP_SIMPLE_EVENTS = "GOB_SIMPLE_EVENTS"
    }

}

internal enum class BusEvent {
    DISMISS_ONBOARDING_FRAGMENT
}
