package net.gini.android.bank.sdk.exampleapp.core.di

import android.content.Context
import android.text.TextUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.GiniBankAPIBuilder
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.core.api.DocumentMetadata
import org.slf4j.Logger
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GiniCaptureNetworkServiceDebugDisabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GiniCaptureNetworkServiceDebugEnabled


@Module
@InstallIn(SingletonComponent::class)
class GiniExampleModule {

    @Singleton
    @Provides
    fun bindGiniBankAPI(
        @ApplicationContext context: Context, logger: Logger
    ): GiniBankAPI {
        val (clientId, clientSecret) = getClientIdAndClientSecret(context, logger)
        return GiniBankAPIBuilder(context, clientId, clientSecret, "emailDomain")
            .build()
    }

    @Singleton
    @GiniCaptureNetworkServiceDebugDisabled
    @Provides
    fun bindGiniCaptureNetworkServiceDebugDisabled(
        @ApplicationContext context: Context, logger: Logger
    ): GiniCaptureDefaultNetworkService {
        return createGiniCaptureNetworkServiceBuilder(context, logger)
            .build()
    }

    @Singleton
    @GiniCaptureNetworkServiceDebugEnabled
    @Provides
    fun bindGiniCaptureNetworkServiceDebugEnabled(
        @ApplicationContext context: Context, logger: Logger
    ): GiniCaptureDefaultNetworkService {
        return createGiniCaptureNetworkServiceBuilder(context, logger)
            .setDebuggingEnabled(true)
            .build()
    }

    private fun createGiniCaptureNetworkServiceBuilder(
        context: Context,
        logger: Logger
    ): GiniCaptureDefaultNetworkService.Builder {
        val (clientId, clientSecret) = getClientIdAndClientSecret(context, logger)
        return GiniCaptureDefaultNetworkService
            .builder(context)
            .setClientCredentials(
                clientId,
                clientSecret,
                "example.com"
            )
            .setDocumentMetadata(getDocumentMetaData())
    }

    private fun getClientIdAndClientSecret(context: Context, logger: Logger): Pair<String, String> {
        val clientId = context.getString(R.string.gini_api_client_id)
        val clientSecret = context.getString(R.string.gini_api_client_secret)
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(clientSecret)) {
            logger.warn(
                "Missing Gini API client credentials. Either create a local.properties file " + "with clientId and clientSecret properties or pass them in as gradle " + "parameters with -PclientId and -PclientSecret."
            )
        }
        return Pair(clientId, clientSecret)
    }

    private fun getDocumentMetaData(): DocumentMetadata {
        val documentMetadata = DocumentMetadata()
        documentMetadata.setBranchId("GCSExampleAndroid")
        documentMetadata.add("AppFlow", "ScreenAPI")

        return documentMetadata
    }
}
