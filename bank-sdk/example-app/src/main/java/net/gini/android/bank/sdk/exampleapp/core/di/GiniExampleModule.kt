package net.gini.android.bank.sdk.exampleapp.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.gini.android.bank.sdk.exampleapp.core.DefaultNetworkServicesProvider
import org.slf4j.Logger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class GiniExampleModule {

    @Singleton
    @Provides
    fun bindDefaultNetworkServicesProvider(
        @ApplicationContext context: Context,
        logger: Logger
    ): DefaultNetworkServicesProvider {
        return DefaultNetworkServicesProvider(context, logger)
    }
}
