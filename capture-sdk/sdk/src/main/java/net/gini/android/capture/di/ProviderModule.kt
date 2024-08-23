package net.gini.android.capture.di

import net.gini.android.capture.analysis.LastAnalyzedDocumentIdProvider
import org.koin.dsl.module

internal val providerModule = module {
    single<LastAnalyzedDocumentIdProvider> { LastAnalyzedDocumentIdProvider() }
}
