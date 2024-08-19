package net.gini.android.bank.sdk.capture.skonto.invoice

import net.gini.android.bank.sdk.capture.skonto.invoice.image.SkontoPageImageProcessor
import net.gini.android.capture.GiniCapture
import net.gini.android.bank.sdk.capture.skonto.invoice.network.SkontoDocumentLayoutNetworkService
import net.gini.android.bank.sdk.capture.skonto.invoice.network.SkontoDocumentPagesNetworkService
import net.gini.android.bank.sdk.capture.skonto.invoice.network.SkontoFileNetworkService
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes
import net.gini.android.capture.analysis.LastAnalyzedDocumentIdProvider
import net.gini.android.capture.di.getGiniCaptureKoin
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.network.GiniCaptureNetworkService
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val skontoInvoiceScreenModule = module {

    factory<GiniCaptureNetworkService> {
        GiniCapture.getInstance()
            .internal().giniCaptureNetworkService
            ?: error("GiniCaptureNetworkService should be initialized")
    }

    factory<SkontoFileNetworkService> {
        SkontoFileNetworkService(get<GiniCaptureNetworkService>())
    }

    factory<SkontoDocumentLayoutNetworkService> {
        SkontoDocumentLayoutNetworkService(get<GiniCaptureNetworkService>())
    }

    factory<SkontoDocumentPagesNetworkService> {
        SkontoDocumentPagesNetworkService(get<GiniCaptureNetworkService>())
    }

    factory {
        SkontoPageImageProcessor()
    }

    // Bridge between GiniCapture and GiniBank
    factory<LastAnalyzedDocumentIdProvider> { getGiniCaptureKoin().get() }

    viewModel { (highlights: Array<SkontoInvoiceHighlightBoxes>) ->
        SkontoInvoiceFragmentViewModel(
            documentId = get<LastAnalyzedDocumentIdProvider>().provide(),
            skontoInvoiceHighlights = highlights.toList(),
            skontoDocumentPagesNetworkService = get(),
            skontoDocumentLayoutNetworkService = get(),
            skontoFileNetworkService = get(),
            skontoPageImageProcessor = get(),
        )
    }
}
