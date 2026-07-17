package net.gini.android.capture.error

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.capture.Document

/**
 * Factory for [ErrorViewModel].
 *
 * Internal use only.
 */
internal class ErrorViewModelFactory(
    private val application: Application,
    private val document: Document?,
    private val errorType: ErrorType?,
    private val customError: String?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ErrorViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ErrorViewModel(application, document, errorType, customError) as T
    }
}
