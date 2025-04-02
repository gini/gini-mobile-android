package net.gini.android.bank.sdk.exampleapp.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.gini.android.bank.sdk.exampleapp.data.storage.TransactionListStorage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StorageModule {

    @Singleton
    @Provides
    internal fun provideTransactionListStorage(
        @ApplicationContext context: Context,
    ): TransactionListStorage {
        return TransactionListStorage(context)
    }
}
