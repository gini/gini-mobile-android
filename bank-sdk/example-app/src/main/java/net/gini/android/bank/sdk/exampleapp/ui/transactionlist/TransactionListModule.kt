package net.gini.android.bank.sdk.exampleapp.ui.transactionlist

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.gini.android.bank.sdk.exampleapp.ui.transactionlist.intent.InitializeIntent

@Module
@InstallIn(SingletonComponent::class)
internal class TransactionListModule {

    @Provides
    internal fun provideInitializeIntent(): InitializeIntent {
        return InitializeIntent()
    }
}