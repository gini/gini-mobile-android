package net.gini.android.bank.sdk.exampleapp.core

import android.content.Context
import android.text.TextUtils
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.GiniBankAPIBuilder
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService
import net.gini.android.core.api.DocumentMetadata

class DefaultNetworkServicesProvider (internal val context: Context, internal val logger: org.slf4j.Logger) {

    private var clientSecret: String? = null
    private var clientId: String? = null
    var giniBankAPI: GiniBankAPI = bindGiniBankAPI(context, logger)
    var defaultNetworkServiceDebugEnabled: GiniCaptureDefaultNetworkService
    = bindGiniCaptureNetworkServiceDebugEnabled(context, logger)
    var defaultNetworkServiceDebugDisabled: GiniCaptureDefaultNetworkService
    = bindGiniCaptureNetworkServiceDebugDisabled(context, logger)

    fun reinitNetworkServices(clientId: String, clientSecret: String) {
        this.clientId = clientId
        this.clientSecret = clientSecret
        defaultNetworkServiceDebugEnabled = bindGiniCaptureNetworkServiceDebugEnabled(context, logger)
        defaultNetworkServiceDebugDisabled = bindGiniCaptureNetworkServiceDebugDisabled(context, logger)
        giniBankAPI = bindGiniBankAPI(context, logger)
    }

    fun bindGiniCaptureNetworkServiceDebugDisabled(
        context: Context, logger: org.slf4j.Logger
    ): GiniCaptureDefaultNetworkService {
        return createGiniCaptureNetworkServiceBuilder(context, logger)
            .build()
    }

    private fun bindGiniCaptureNetworkServiceDebugEnabled(
        context: Context,
        logger: org.slf4j.Logger,
    ): GiniCaptureDefaultNetworkService {
        return createGiniCaptureNetworkServiceBuilder(context, logger)
            .setDebuggingEnabled(true)
            .build()
    }
    private fun bindGiniBankAPI(
        context: Context, logger: org.slf4j.Logger
    ): GiniBankAPI {
        val clientIdAndSecret = getClientIdAndClientSecret(context, logger)
        return GiniBankAPIBuilder(
            context,
            clientId ?: clientIdAndSecret.first,
            clientSecret ?: clientIdAndSecret.second,
            "emailDomain"
        ).build()
    }
    private fun createGiniCaptureNetworkServiceBuilder(
        context: Context,
        logger: org.slf4j.Logger,
    ): GiniCaptureDefaultNetworkService.Builder {
        val clientIdAndSecret = getClientIdAndClientSecret(context, logger)
        return GiniCaptureDefaultNetworkService
            .builder(context)
            .setClientCredentials(
                clientId ?: clientIdAndSecret.first,
                clientSecret ?: clientIdAndSecret.second,
                "example.com"
            )
            .setDocumentMetadata(getDocumentMetaData())
    }

    private fun getClientIdAndClientSecret(
        context: Context,
        logger: org.slf4j.Logger,
    ): Pair<String, String> {
        val clientId = context.getString(R.string.gini_api_client_id)
        val clientSecret = context.getString(R.string.gini_api_client_secret)
        if (TextUtils.isEmpty(clientId) || TextUtils.isEmpty(clientSecret)) {
            logger.warn(
                "Missing Gini API client credentials. Either create a local.properties file "
                        + "with clientId and clientSecret properties or pass them in as gradle "
                        + "parameters with -PclientId and -PclientSecret."
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
