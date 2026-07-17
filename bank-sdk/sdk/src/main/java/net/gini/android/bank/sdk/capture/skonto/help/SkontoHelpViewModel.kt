package net.gini.android.bank.sdk.capture.skonto.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.view.InjectedViewAdapterInstance

internal class SkontoHelpViewModel : ViewModel() {

    val isBottomNavigationBarEnabled: Boolean =
        GiniCapture.getInstance().isBottomNavigationBarEnabled

    val customBottomNavBarAdapter: InjectedViewAdapterInstance<SkontoHelpNavigationBarBottomAdapter>? =
        GiniBank.skontoHelpNavigationBarBottomAdapterInstance

    val shouldDisallowScreenshots: Boolean =
        GiniCapture.hasInstance() && !GiniCapture.getInstance().allowScreenshots

    private val _sideEffects = Channel<SideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<SideEffect> = _sideEffects.receiveAsFlow()

    fun onBackClicked() {
        viewModelScope.launch { _sideEffects.send(SideEffect.NavigateBack) }
    }

    internal sealed interface SideEffect {
        object NavigateBack : SideEffect
    }
}
