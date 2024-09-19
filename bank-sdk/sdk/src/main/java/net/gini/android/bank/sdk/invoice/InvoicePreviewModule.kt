package net.gini.android.bank.sdk.invoice

import net.gini.android.bank.sdk.invoice.image.InvoicePreviewPageImageProcessor
import net.gini.android.capture.GiniCapture
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentLayoutNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentPagesNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewFileNetworkService
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.network.model.GiniCaptureBox
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val invoicePreviewScreenModule = module {

    factory<GiniCaptureNetworkService> {
        GiniCapture.getInstance()
            .internal().giniCaptureNetworkService
            ?: error("GiniCaptureNetworkService should be initialized")
    }

    factory<InvoicePreviewFileNetworkService> {
        InvoicePreviewFileNetworkService(get<GiniCaptureNetworkService>())
    }

    factory<InvoicePreviewDocumentLayoutNetworkService> {
        InvoicePreviewDocumentLayoutNetworkService(get<GiniCaptureNetworkService>())
    }

    factory<InvoicePreviewDocumentPagesNetworkService> {
        InvoicePreviewDocumentPagesNetworkService(get<GiniCaptureNetworkService>())
    }

    factory {
        InvoicePreviewPageImageProcessor()
    }

    // Bridge between GiniCapture and GiniBank
    factory<LastAnalyzedDocumentProvider> { getGiniCaptureKoin().get() }

    viewModel { (screenTitle: String,
                    documentId: String,
                    infoTextLines: Array<String>,
                    highlights: Array<GiniCaptureBox>) ->
        InvoicePreviewViewModel(
            screenTitle = screenTitle,
            documentId = documentId,
            infoTextLines = infoTextLines.toList(),
            highlightBoxes = highlights.toList(),
            invoicePreviewDocumentPagesNetworkService = get(),
            invoicePreviewDocumentLayoutNetworkService = get(),
            invoicePreviewFileNetworkService = get(),
            invoicePreviewPageImageProcessor = get(),
        )
    }
}
