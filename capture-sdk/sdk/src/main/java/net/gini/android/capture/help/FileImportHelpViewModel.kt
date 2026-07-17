package net.gini.android.capture.help

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.gini.android.capture.GiniCapture

/**
 * Internal use only.
 *
 * ViewModel for the file import help screen. Holds the [GiniCapture] configuration
 * state which the screen renders.
 *
 * @suppress
 */
internal class FileImportHelpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        FileImportHelpUiState(
            isBottomNavigationBarEnabled = GiniCapture.hasInstance() &&
                    GiniCapture.getInstance().isBottomNavigationBarEnabled
        )
    )
    val uiState: StateFlow<FileImportHelpUiState> = _uiState.asStateFlow()
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class FileImportHelpUiState(
    val isBottomNavigationBarEnabled: Boolean,
)
