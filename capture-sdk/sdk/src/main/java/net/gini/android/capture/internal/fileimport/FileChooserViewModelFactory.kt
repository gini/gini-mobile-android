package net.gini.android.capture.internal.fileimport

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.capture.DocumentImportEnabledFileTypes

/**
 * Factory for [FileChooserViewModel].
 *
 * Internal use only.
 */
internal class FileChooserViewModelFactory(
    private val application: Application,
    private val docImportEnabledFileTypes: DocumentImportEnabledFileTypes?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FileChooserViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return FileChooserViewModel(application, docImportEnabledFileTypes) as T
    }
}
