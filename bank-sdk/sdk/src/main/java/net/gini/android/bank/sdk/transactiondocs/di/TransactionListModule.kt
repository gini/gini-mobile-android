package net.gini.android.bank.sdk.transactiondocs.di

import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.transactiondocs.internal.GiniBankTransactionDocs
import org.koin.dsl.module

internal val transactionListModule = module {
    factory<GiniBankTransactionDocs?> { GiniBank.giniBankTransactionDocs }
}
