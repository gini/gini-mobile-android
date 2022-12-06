package net.gini.android.bank.sdk.network

import android.content.Context
import net.gini.android.core.api.DocumentMetadata
import net.gini.android.capture.network.GiniCaptureDefaultNetworkService

/**
 * Utility method to create a basic Default Network Service.
 * For more control see [GiniCaptureDefaultNetworkService].
 */
fun getDefaultNetworkService(
    context: Context,
    clientId: String,
    clientSecret: String,
    emailDomain: String,
    documentMetadata: DocumentMetadata
): GiniCaptureDefaultNetworkService =
    GiniCaptureDefaultNetworkService.builder(context)
        .setClientCredentials(clientId, clientSecret, emailDomain)
        .setDocumentMetadata(documentMetadata)
        .build()

/**
 * Utility method to create a basic Default Network Api.
 * For more details see [GiniCaptureDefaultNetworkApi].
 */
fun getDefaultNetworkApi(service: GiniCaptureDefaultNetworkService): GiniCaptureDefaultNetworkApi =
    GiniCaptureDefaultNetworkApi.builder()
        .withGiniCaptureDefaultNetworkService(service)
        .build()
