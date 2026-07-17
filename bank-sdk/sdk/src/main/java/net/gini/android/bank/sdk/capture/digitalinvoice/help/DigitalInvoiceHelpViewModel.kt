package net.gini.android.bank.sdk.capture.digitalinvoice.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class DigitalInvoiceHelpViewModel : ViewModel() {

    val helpItems: List<HelpItem> =
        listOf(HelpItem.DIGITAL_INVOICE, HelpItem.EDIT, HelpItem.SHOP)

    private val _sideEffects = Channel<SideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<SideEffect> = _sideEffects.receiveAsFlow()

    fun onBackClicked() {
        viewModelScope.launch { _sideEffects.send(SideEffect.NavigateBack) }
    }

    internal sealed interface SideEffect {
        object NavigateBack : SideEffect
    }
}
