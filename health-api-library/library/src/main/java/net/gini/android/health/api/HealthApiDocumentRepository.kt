package net.gini.android.health.api

import android.util.Size
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentMetadata
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
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
import net.gini.android.health.api.response.ConfigurationResponse
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
    private val giniApiType: GiniHealthApiType
) : DocumentRepository<ExtractionsContainer>(documentRemoteSource, giniApiType) {

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
        wrapInResource {
            val imageUri = getPages(documentId)
                .getPageByPageNumber(page)
                .getLargestImageUriSmallerThan(Size(2000, 2000))

            if (imageUri != null) {
                documentRemoteSource.getFile(imageUri.toString())
            } else {
                throw NoSuchElementException("No page image found for page number $page in document $documentId")
            }
        }

    private suspend fun getPages(documentId: String): List<Page> =
        documentRemoteSource.getPages(documentId)
            .toPageList(documentRemoteSource.baseUri)

    suspend fun getPaymentProviders(): Resource<List<PaymentProvider>> {
        return wrapInResource {
            documentRemoteSource.getPaymentProviders()
                .filter { paymentProvider ->
                    paymentProvider.isEnabled()
                }
                .map { paymentProviderResponse ->
                    val icon = documentRemoteSource.getFile(paymentProviderResponse.iconLocation)
                    paymentProviderResponse.toPaymentProvider(icon)
                }
        }
    }

    suspend fun getPaymentProvider(providerId: String): Resource<PaymentProvider> =
        wrapInResource {
            val paymentProviderResponse = documentRemoteSource.getPaymentProvider(providerId)
            val icon = documentRemoteSource.getFile(paymentProviderResponse.iconLocation)
            paymentProviderResponse.toPaymentProvider(icon)
        }

    suspend fun createPaymentRequest(paymentRequestInput: PaymentRequestInput): Resource<String> {
        return wrapInResource {
            documentRemoteSource.createPaymentRequest(paymentRequestInput)
        }
    }

    suspend fun getPaymentRequestDocument(paymentRequestId: String): Resource<ByteArray> =
        wrapInResource {
            documentRemoteSource.getPaymentRequestDocument(paymentRequestId)
        }

    suspend fun deletePaymentRequest(paymentRequestId: String): Resource<Unit> =
        wrapInResource {
            documentRemoteSource.deletePaymentRequest(paymentRequestId)
        }

    suspend fun getPaymentRequestImage(paymentRequestId: String): Resource<ByteArray> =
        wrapInResource {
            documentRemoteSource.getPaymentRequestImage(paymentRequestId)
        }

    suspend fun getConfigurations(): Resource<ConfigurationResponse> =
        wrapInResource {
            documentRemoteSource.getConfigurations()
        }

    suspend fun deleteDocuments(documentIds: List<String>): Resource<Unit> =
        wrapInResource {
            documentRemoteSource.deleteDocuments(documentIds)
        }

    suspend fun deletePaymentRequests(paymentRequestIds: List<String>): Resource<Unit> =
        wrapInResource {
            documentRemoteSource.deletePaymentRequests(paymentRequestIds)
        }
}