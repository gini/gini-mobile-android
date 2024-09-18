package net.gini.android.bank.sdk.invoice

import net.gini.android.bank.sdk.invoice.image.InvoicePreviewPageImageProcessor
import net.gini.android.capture.GiniCapture
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentLayoutNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewDocumentPagesNetworkService
import net.gini.android.bank.sdk.invoice.network.InvoicePreviewFileNetworkService
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes
import net.gini.android.capture.analysis.LastAnalyzedDocumentIdProvider
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.network.GiniCaptureNetworkService
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
    factory<LastAnalyzedDocumentIdProvider> { getGiniCaptureKoin().get() }

    viewModel { (skontoData: SkontoData, highlights: Array<SkontoInvoiceHighlightBoxes>) ->
        InvoicePreviewViewModel(
            documentId = get<LastAnalyzedDocumentIdProvider>().provide(),
            skontoData = skontoData,
            skontoInvoiceHighlights = highlights.toList(),
            invoicePreviewDocumentPagesNetworkService = get(),
            invoicePreviewDocumentLayoutNetworkService = get(),
            invoicePreviewFileNetworkService = get(),
            invoicePreviewPageImageProcessor = get(),
        )
    }
}
