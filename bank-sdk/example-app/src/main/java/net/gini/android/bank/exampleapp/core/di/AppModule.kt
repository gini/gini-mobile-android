package net.gini.android.bank.exampleapp.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.gini.android.bank.exampleapp.ExampleApp
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Singleton
    @Provides
    fun bindScreenApiExampleAppLogger(): Logger {
        return LoggerFactory.getLogger(ExampleApp::class.java)
    }
}
