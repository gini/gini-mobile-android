package net.gini.android.health.api

import android.util.Size
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentMetadata
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.models.Page
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.models.getPageByPageNumber
import net.gini.android.health.api.models.toPageList
import net.gini.android.health.api.models.toPaymentProvider
import net.gini.android.health.api.util.ImageCompression
import org.json.JSONObject

/**
 * Created by Alpár Szotyori on 14.10.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Internal use only.
 */
class HealthApiDocumentRepository(
    private val documentRemoteSource: HealthApiDocumentRemoteSource,
    sessionManager: SessionManager,
    private val giniApiType: GiniHealthApiType
) : DocumentRepository<ExtractionsContainer>(documentRemoteSource, sessionManager, giniApiType) {

    override suspend fun createPartialDocument(documentData: ByteArray, contentType: String,
                                               filename: String?,
                                               documentType: DocumentManager.DocumentType?,
                                               documentMetadata: DocumentMetadata?
    ): Resource<Document> {
        return super.createPartialDocument(
            ImageCompression.compressIfImageAndExceedsSizeLimit(documentData),
            contentType,
            filename,
            documentType,
            documentMetadata
        )
    }

    override fun createExtractionsContainer(
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
        responseJSON: JSONObject
    ): ExtractionsContainer = ExtractionsContainer(specificExtractions, compoundExtractions)

    suspend fun getPageImage(
        documentId: String,
        page: Int
    ): Resource<ByteArray> =
        withAccessToken { accessToken ->
            wrapInResource {
                val imageUri = getPages(accessToken, documentId)
                    .getPageByPageNumber(page)
                    .getLargestImageUriSmallerThan(Size(2000, 2000))

                if (imageUri != null) {
                    documentRemoteSource.getFile(accessToken, imageUri.toString())
                } else {
                    throw NoSuchElementException("No page image found for page number $page in document $documentId")
                }
            }
        }

    private suspend fun getPages(accessToken: String, documentId: String): List<Page> =
        documentRemoteSource.getPages(accessToken, documentId)
            .toPageList(documentRemoteSource.baseUri)

    suspend fun getPaymentProviders(): Resource<List<PaymentProvider>> {
        return withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPaymentProviders(accessToken)
                    .filter { paymentProvider ->
                        paymentProvider.isEnabled()
                    }
                    .map { paymentProviderResponse ->
                        val icon = documentRemoteSource.getFile(accessToken, paymentProviderResponse.iconLocation)
                        paymentProviderResponse.toPaymentProvider(icon)
                    }
            }
        }
    }

    suspend fun getPaymentProvider(providerId: String): Resource<PaymentProvider> =
        withAccessToken { accessToken ->
            wrapInResource {
                val paymentProviderResponse = documentRemoteSource.getPaymentProvider(accessToken, providerId)
                val icon = documentRemoteSource.getFile(accessToken, paymentProviderResponse.iconLocation)
                paymentProviderResponse.toPaymentProvider(icon)
            }
        }

    suspend fun createPaymentRequest(paymentRequestInput: PaymentRequestInput): Resource<String> {
        return withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.createPaymentRequest(accessToken, paymentRequestInput)
            }
        }
    }

    suspend fun getPaymentRequestDocument(paymentRequestId: String): Resource<ByteArray> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPaymentRequestDocument(accessToken, paymentRequestId)
            }
        }

    suspend fun getPaymentRequestImage(paymentRequestId: String): Resource<ByteArray> =
        withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPaymentRequestImage(accessToken, paymentRequestId)
            }
        }
}