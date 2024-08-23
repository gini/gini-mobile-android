package net.gini.android.capture.analysis

import net.gini.android.capture.di.getGiniCaptureKoin

open class AnalysisScreenPresenterExtension {

    val lastAnalyzedDocumentIdProvider: LastAnalyzedDocumentIdProvider by
    getGiniCaptureKoin().inject()

}
