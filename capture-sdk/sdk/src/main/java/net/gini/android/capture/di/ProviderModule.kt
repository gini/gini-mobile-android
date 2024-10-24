package net.gini.android.capture.di

import kotlinx.coroutines.Dispatchers
import net.gini.android.capture.analysis.LastAnalyzedDocumentProvider
import net.gini.android.capture.provider.LastExtractionsProvider
import org.koin.dsl.module

internal val providerModule = module {
    single<LastAnalyzedDocumentProvider> {
        LastAnalyzedDocumentProvider(
            backgroundDispatcher = Dispatchers.IO
        )
    }
    single {
        LastExtractionsProvider()
    }
}
