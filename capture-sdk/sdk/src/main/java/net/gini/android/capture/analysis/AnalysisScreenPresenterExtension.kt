package net.gini.android.capture.analysis

import net.gini.android.capture.analysis.transactiondoc.AttachedToTransactionDocumentProvider
import net.gini.android.capture.di.getGiniCaptureKoin

internal open class AnalysisScreenPresenterExtension {

    val lastAnalyzedDocumentProvider: LastAnalyzedDocumentProvider
            by getGiniCaptureKoin().inject()

    val attachDocToTransactionDialogProvider: AttachedToTransactionDocumentProvider
            by getGiniCaptureKoin().inject()

}
