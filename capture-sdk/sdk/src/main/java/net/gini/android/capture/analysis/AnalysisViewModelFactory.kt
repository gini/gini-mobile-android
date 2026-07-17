package net.gini.android.capture.analysis

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.gini.android.capture.Document

/**
 * Factory for [AnalysisViewModel].
 *
 * Internal use only.
 */
internal class AnalysisViewModelFactory @JvmOverloads constructor(
    private val application: Application,
    private val document: Document,
    private val documentAnalysisErrorMessage: String?,
    private val isInvoiceSavingEnabled: Boolean,
    private val analysisInteractor: AnalysisInteractor = AnalysisInteractor(application)
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return AnalysisViewModel(
            application,
            document,
            documentAnalysisErrorMessage,
            analysisInteractor,
            isInvoiceSavingEnabled
        ) as T
    }
}
