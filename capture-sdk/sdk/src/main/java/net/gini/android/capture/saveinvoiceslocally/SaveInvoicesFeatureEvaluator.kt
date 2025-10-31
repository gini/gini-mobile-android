package net.gini.android.capture.saveinvoiceslocally

import android.view.View
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.internal.util.FeatureConfiguration.isSavingInvoicesLocallyEnabled

/**
 * Internal use only.
 * Helper class of the Save Invoices Locally feature to determine whether the Save Invoices Locally
 * view should be shown, and what to pass to the 'AnalysisFragment' to determine whether the
 * invoices should be saved locally or not.
 */

internal object SaveInvoicesFeatureEvaluator {

    private val getSaveInvoicesLocallyFeatureEnabledUseCase:
            GetSaveInvoicesLocallyFeatureEnabledUseCase
        get() = getGiniCaptureKoin().get()


    /**
     * These conditions must be met to show the Save Invoices Locally view:
     * 1. The Gini Capture SDK instance must be initialized.
     * 2. The Save Invoices Locally feature must be enabled in the Gini Capture SDK.
     * 3. The Save Invoices Locally 'feature flag' must be enabled.
     * 4. There must be at least one valid document in the multi-page document that
     *    was not imported via the picker or "Open with".
     * */

    fun shouldShowSaveInvoicesLocallyView(): Boolean {
        if (!GiniCapture.hasInstance()) return false

        val documents = GiniCapture.getInstance()
            .internal()
            .imageMultiPageDocumentMemoryStore
            .multiPageDocument
            ?.documents
            .orEmpty()

        val hasValidDocs = documents.any { doc ->
            doc.uri != null &&
                    doc.importMethod !in listOf(
                Document.ImportMethod.PICKER,
                Document.ImportMethod.OPEN_WITH
            )
        }

        return isSavingInvoicesLocallyEnabled() &&
                getSaveInvoicesLocallyFeatureEnabledUseCase.invoke() &&
                hasValidDocs
    }

    /**
     * Helper method to evaluate whether the invoices should be saved locally in
     * 'AnalysisFragment' or not.
     * - If the view in 'MultiPageReviewFragment' is visible which is determined by
     * [SaveInvoicesFeatureEvaluator.shouldShowSaveInvoicesLocallyView]
     * or the switch is turned on by the user only then the invoices should be saved locally.
     * */

    fun shouldSaveInvoicesLocally(
        isViewVisible: Int,
        isSwitchOn: Boolean
    ): Boolean = (isViewVisible == View.VISIBLE) && isSwitchOn

}
