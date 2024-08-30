package net.gini.android.bank.sdk.capture

import net.gini.android.bank.sdk.capture.extractions.skonto.SkontoInvoiceHighlightsExtractor
import org.koin.dsl.module

val captureFlowFragmentModule = module {
    single { SkontoInvoiceHighlightsExtractor() }
}
