package net.gini.android.bank.sdk.capture.digitalinvoice.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.capture.util.BusEvent
import net.gini.android.bank.sdk.capture.util.OncePerInstallEvent
import net.gini.android.bank.sdk.capture.util.OncePerInstallEventStore
import net.gini.android.bank.sdk.capture.util.SimpleBusEventStore

internal class DigitalInvoiceOnboardingViewModel(
    private val oncePerInstallEventStore: OncePerInstallEventStore,
    private val simpleBusEventStore: SimpleBusEventStore,
) : ViewModel() {

    private val _sideEffects = Channel<SideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<SideEffect> = _sideEffects.receiveAsFlow()

    fun dismissOnboarding(doNotShowAnymore: Boolean) {
        if (doNotShowAnymore) {
            oncePerInstallEventStore.saveEvent(OncePerInstallEvent.SHOW_DIGITAL_INVOICE_ONBOARDING)
        }
        simpleBusEventStore.saveEvent(BusEvent.DISMISS_ONBOARDING_FRAGMENT)
        viewModelScope.launch { _sideEffects.send(SideEffect.Close) }
    }

    internal sealed interface SideEffect {
        object Close : SideEffect
    }
}
