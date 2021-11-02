package net.gini.android.bank.sdk.capture.util

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import org.slf4j.LoggerFactory

internal class OncePerInstallEventStore(context: Context) {
    private val mSharedPreferences: SharedPreferences = context.getSharedPreferences(ONCE_PER_INSTALL_EVENTS, Context.MODE_PRIVATE)

    fun containsEvent(event: OncePerInstallEvent): Boolean = mSharedPreferences.contains(event.name)

    fun saveEvent(event: OncePerInstallEvent) {
        mSharedPreferences.edit {
            putBoolean(event.name, true)
        }
        LOG.debug("Saved event {}", event.name)
    }

    @VisibleForTesting
    fun clearEvent(event: OncePerInstallEvent) {
        mSharedPreferences.edit {
            remove(event.name)
        }
        LOG.debug("Cleared event {}", event.name)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OncePerInstallEventStore::class.java)
        private const val ONCE_PER_INSTALL_EVENTS = "GBS_ONCE_PER_INSTALL_EVENTS"
    }
}

internal enum class OncePerInstallEvent {
    SHOW_DIGITAL_INVOICE_ONBOARDING,
    SCROLL_DIGITAL_INVOICE
}
