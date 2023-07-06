package net.gini.android.capture.screen.core.di

import android.content.Context
import android.text.TextUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.capture.network.GiniCaptureNetworkService
import net.gini.android.capture.screen.R
import net.gini.android.core.api.DocumentMetadata
import org.slf4j.Logger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class GiniCaptureModule {

    @Singleton
    @Provides
    fun bindGiniCaptureNetworkService(
        @ApplicationContext context: Context,
        logger: Logger
    ): GiniCaptureDefaultNetworkService {
        val clientId = context.getString(R.string.gini_api_client_id)
        val clientSecret = context.getString(R.string.gini_api_client_secret)
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(clientSecret)) {
            logger.warn(
                "Missing Gini API client credentials. Either create a local.properties file "
                        + "with clientId and clientSecret properties or pass them in as gradle "
                        + "parameters with -PclientId and -PclientSecret."
            )
        }
        val documentMetadata = DocumentMetadata()
        documentMetadata.setBranchId("GCSExampleAndroid")
        documentMetadata.add("AppFlow", "ScreenAPI")

        return GiniCaptureDefaultNetworkService.builder(context)
            .setClientCredentials(clientId, clientSecret, "example.com")
            .setDocumentMetadata(documentMetadata)
            .build()
    }
}
