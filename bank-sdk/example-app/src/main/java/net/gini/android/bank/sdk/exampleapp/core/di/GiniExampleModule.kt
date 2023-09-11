package net.gini.android.bank.sdk.exampleapp.core.di

import android.content.Context
import android.text.TextUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.core.api.DocumentMetadata
import org.slf4j.Logger
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GiniCaptureNetworkServiceRelease

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GiniCaptureNetworkServiceDebug


@Module
@InstallIn(SingletonComponent::class)
class GiniExampleModule {

    @Singleton
    @GiniCaptureNetworkServiceRelease
    @Provides
    fun bindGiniCaptureNetworkServiceRelease(
        @ApplicationContext context: Context, logger: Logger
    ): GiniCaptureDefaultNetworkService {
        val (clientId, clientSecret) = getClientIdAndClientSecret(context, logger)
        return GiniCaptureDefaultNetworkService.builder(context).setClientCredentials(
            clientId,
            clientSecret,
            "example.com"
        ).setDocumentMetadata(getDocumentMetaData()).build()
    }

    @Singleton
    @GiniCaptureNetworkServiceDebug
    @Provides
    fun bindGiniCaptureNetworkServiceDebug(
        @ApplicationContext context: Context, logger: Logger
    ): GiniCaptureDefaultNetworkService {
        val (clientId, clientSecret) = getClientIdAndClientSecret(context, logger)
        return GiniCaptureDefaultNetworkService.builder(context).setClientCredentials(
            clientId,
            clientSecret,
            "example.com"
        ).setDocumentMetadata(getDocumentMetaData()).setDebuggingEnabled(true).build()
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
