package net.gini.android.capture.analysis.warning

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Internal use only.
 *
 * ViewModel for the warning bottom sheet. Resolves the texts to show for the given
 * [WarningType] and decides how the cancel and proceed actions should be handled.
 *
 * @suppress
 */
internal class WarningBottomSheetViewModel(warningType: WarningType?) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WarningUiState(
            titleRes = warningType?.titleRes,
            descriptionRes = warningType?.descriptionRes,
        )
    )
    val uiState: StateFlow<WarningUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<WarningSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<WarningSideEffect> = _sideEffects.receiveAsFlow()

    fun onCancelClicked() {
        sendSideEffect(WarningSideEffect.CancelAndDismiss)
    }

    fun onProceedClicked() {
        sendSideEffect(WarningSideEffect.ProceedAndDismiss)
    }

    private fun sendSideEffect(sideEffect: WarningSideEffect) {
        viewModelScope.launch { _sideEffects.send(sideEffect) }
    }

    class Factory(private val warningType: WarningType?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WarningBottomSheetViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return WarningBottomSheetViewModel(warningType) as T
        }
    }
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class WarningUiState(
    @StringRes val titleRes: Int?,
    @StringRes val descriptionRes: Int?,
)

/**
 * Internal use only.
 *
 * @suppress
 */
internal sealed interface WarningSideEffect {
    data object CancelAndDismiss : WarningSideEffect
    data object ProceedAndDismiss : WarningSideEffect
}
