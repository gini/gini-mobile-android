package net.gini.android.health.api

import android.net.Uri
import android.util.Size
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.Resource.Companion.wrapInResource
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.Extraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.models.Page
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.models.getPageByPageNumber
import net.gini.android.health.api.models.toPageList
import net.gini.android.health.api.models.toPaymentProvider
import net.gini.android.health.api.response.AppVersionResponse
import net.gini.android.health.api.response.Colors
import net.gini.android.health.api.response.PaymentProviderResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
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

    // TODO remove mock payment provider when backend ready
    suspend fun getPaymentProviders(): Resource<List<PaymentProvider>> {
        return withAccessToken { accessToken ->
            wrapInResource {
                documentRemoteSource.getPaymentProviders(accessToken).toMutableList().apply { add(0, PaymentProviderResponse(
                    id = "com.gini.android.fake.notSupported",
                    name = "Open With Tester",
                    gpcSupportedPlatforms = listOf(),
                    minAppVersion = AppVersionResponse(
                        android = "1.0.0"
                    ),
                    colors = Colors(
                        background = "D9B965",
                        text = "FFFFFF"
                    ),
                    iconLocation = "https://health-api.gini.net/paymentProviders/f7d06ee0-51fd-11ec-8216-97f0937beb16/icon",
                    playStoreUrl = "https://play.google.com/store/apps/details?id=net.gini.android.fake",
                    packageNameAndroid = "",
                    openWithSupportedPlatforms = listOf()
                ))

                    add(0, PaymentProviderResponse(
                        id = "com.gini.android.fake.supported",
                        name = "GPC Supported Tester",
                        gpcSupportedPlatforms = listOf("android"),
                        minAppVersion = AppVersionResponse(
                            android = "1.0.0"
                        ),
                        colors = Colors(
                            background = "D9B965",
                            text = "FFFFFF"
                        ),
                        iconLocation = "https://health-api.gini.net/paymentProviders/f7d06ee0-51fd-11ec-8216-97f0937beb16/icon",
                        playStoreUrl = "https://play.google.com/store/apps/details?id=net.gini.android.fake",
                        packageNameAndroid = "",
                        openWithSupportedPlatforms = listOf("android")
                    ))

                    add(0, PaymentProviderResponse(
                        id = "com.gini.android.fake.openWith",
                        name = "Open With Tester Supported",
                        gpcSupportedPlatforms = listOf(),
                        minAppVersion = AppVersionResponse(
                            android = "1.0.0"
                        ),
                        colors = Colors(
                            background = "D9B965",
                            text = "FFFFFF"
                        ),
                        iconLocation = "https://health-api.gini.net/paymentProviders/f7d06ee0-51fd-11ec-8216-97f0937beb16/icon",
                        playStoreUrl = "https://play.google.com/store/apps/details?id=net.gini.android.fake",
                        packageNameAndroid = "",
                        openWithSupportedPlatforms = listOf("android")
                    ))
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
}