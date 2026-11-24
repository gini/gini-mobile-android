package net.gini.android.capture.network

import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import net.gini.android.bank.api.BankApiDocumentManager
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.capture.Document
import net.gini.android.capture.error.ErrorType
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.core.api.Resource
import net.gini.android.core.api.requests.ApiException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Date
import net.gini.android.bank.api.models.Configuration as BankConfig
import net.gini.android.core.api.models.Document as ApiDocument

/**
 * Created by Alpár Szotyori on 25.02.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class GiniCaptureDefaultNetworkServiceTest {

    @Test
    fun `allows retrieving the analyzed Gini Bank API document after analysis`() {
        // Given
        UserAnalytics.initialize(getApplicationContext())
        // Mock Gini Bank API documents
        val partialDocument = net.gini.android.core.api.models.Document(
            "id1",
            net.gini.android.core.api.models.Document.ProcessingState.COMPLETED,
            "filename1",
            1,
            Date(),
            Date(),
            net.gini.android.core.api.models.Document.SourceClassification.SCANNED,
            Uri.EMPTY,
            listOf(),
            listOf()
        )

        val compositeDocument = net.gini.android.core.api.models.Document(
            "id2",
            net.gini.android.core.api.models.Document.ProcessingState.COMPLETED,
            "filename2",
            1,
            Date(),
            Date(),
            net.gini.android.core.api.models.Document.SourceClassification.COMPOSITE,
            Uri.EMPTY,
            listOf(),
            listOf()
        )

        // Mock DocumentTaskManager returning the mock documents
        val documentManager = mockk<BankApiDocumentManager>()
        coEvery {
            documentManager.createPartialDocument(
                any(),
                any(),
                null,
                null,
                any()
            )
        } returns Resource.Success(partialDocument)
        coEvery {
            documentManager.createCompositeDocument(
                any<LinkedHashMap<net.gini.android.core.api.models.Document, Int>>(),
                any(),
                any()
            )
        } returns Resource.Success(compositeDocument)
        coEvery { documentManager.pollDocument(any()) } returns Resource.Success(compositeDocument)
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(
            mockk()
        )

        // Mock GiniBankAPI
        val bankApi = mockk<GiniBankAPI>()
        every { bankApi.documentManager } returns documentManager

        val networkService = GiniCaptureDefaultNetworkService(
            bankApi,
            null,
            getApplicationContext()
        )

        // Mock Gini Capture SDK document
        val captureDocument = mockk<Document>()
        every { captureDocument.id } returns "id"
        every { captureDocument.data } returns byteArrayOf()
        every { captureDocument.mimeType } returns "image/jpeg"
        every { captureDocument.generateUploadMetadata(getApplicationContext()) } returns ""

        // When
        networkService.upload(captureDocument, mockk(relaxed = true))

        // Wait for queued runnables to execute
        shadowOf(Looper.getMainLooper()).idle()

        networkService.analyze(linkedMapOf(partialDocument.id to 0), mockk(relaxed = true))

        // Wait for queued runnables to execute
        shadowOf(Looper.getMainLooper()).idle()

        // Then
        assertThat(networkService.analyzedGiniApiDocument).isEqualTo(compositeDocument)
    }

    @Test
    fun `getConfiguration returns success and maps configuration`() {
        val documentManager = mockk<BankApiDocumentManager>()

        val bankConfig = BankConfig(
            "test-client-id",
            isUserJourneyAnalyticsEnabled = true,
            isSkontoEnabled = true,
            isReturnAssistantEnabled = true,
            amplitudeApiKey = "amplitude",
            isTransactionDocsEnabled = true,
            isInstantPaymentEnabled = true,
            isEInvoiceEnabled = true,
            isQrCodeEducationEnabled = true,
            isSavePhotosLocallyEnabled = true,
            isAlreadyPaidHintEnabled = true,
            isPaymentDueHintEnabled = true,
            isPaymentScheduleHintEnabled = true,
            isCreditNoteHintEnabled = true,
            ingredientBrandScreens = listOf("Analysis"),
            isFraudSignalsEnabled = true,
        )

        // Add more stubs if mapBankConfigurationToConfiguration uses other properties
        coEvery { documentManager.getConfigurations() } returns Resource.Success(bankConfig)
        val bankApi = mockk<GiniBankAPI>()
        every { bankApi.documentManager } returns documentManager

        val service = GiniCaptureDefaultNetworkService(bankApi, null, mockk())

        // The callback is invoked from inside launchCancellable's coroutine, which swallows
        // anything thrown in it — an assertion that fails there is reported to the uncaught
        // handler and the test still passes. So the callback only records what it was given and
        // every assertion runs on this thread once the coroutine has completed.
        var captured: Configuration? = null
        var failed: Error? = null
        var wasCancelled = false

        service.getConfiguration(object : GiniCaptureNetworkCallback<Configuration, Error> {
            override fun success(result: Configuration) {
                captured = result
            }

            override fun failure(error: Error) {
                failed = error
            }

            override fun cancelled() {
                wasCancelled = true
            }
        })
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(failed).isNull()
        assertThat(wasCancelled).isFalse()
        assertThat(captured).isNotNull()
        assertThat(captured!!.isPaymentScheduleHintEnabled).isTrue()
        // Guards the ingredientBrandScreens mapping: drop that line from
        // mapBankConfigurationToConfiguration and this assertion fails, instead of the default
        // network silently discarding the backend flag.
        assertThat(captured!!.ingredientBrandScreens).containsExactly("Analysis")
        assertThat(captured!!.isFraudSignalsEnabled).isTrue()
    }

    @Test
    fun `getConfiguration returns error and calls failure`() {
        val documentManager = mockk<BankApiDocumentManager>(relaxed = true)
        coEvery { documentManager.getConfigurations() } returns Resource.Error<BankConfig>(message = "fail")
        val bankApi = mockk<GiniBankAPI>(relaxed = true) {
            every { this@mockk.documentManager } returns documentManager
        }
        val service = GiniCaptureDefaultNetworkService(bankApi, null, mockk())
        var called = false

        service.getConfiguration(object : GiniCaptureNetworkCallback<Configuration, Error> {
            override fun success(result: Configuration) {
                error("Should not succeed")
            }

            override fun failure(error: Error) {
                called = true
                assertThat(error).isNotNull()
            }

            override fun cancelled() {
                error("Should not cancel")
            }
        })
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(called).isTrue()
    }

    @Test
    fun `getConfiguration returns cancelled and calls cancelled`() {
        val documentManager = mockk<BankApiDocumentManager>(relaxed = true)
        coEvery { documentManager.getConfigurations() } returns Resource.Cancelled()
        val bankApi = mockk<GiniBankAPI>(relaxed = true) {
            every { this@mockk.documentManager } returns documentManager
        }
        val service = GiniCaptureDefaultNetworkService(bankApi, null, mockk())
        var called = false

        service.getConfiguration(object : GiniCaptureNetworkCallback<Configuration, Error> {
            override fun success(result: Configuration) {
                error("Should not succeed")
            }

            override fun failure(error: Error) {
                error("Should not fail")
            }

            override fun cancelled() {
                called = true
            }
        })
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(called).isTrue()
    }

    @Test
    fun `analyze passes the exception to the failure callback when creating the composite document fails`() {
        // Given
        val exception = UnknownHostException("Unable to resolve host")

        // When
        val error = analyzeAndGetFailure(
            compositeDocumentResource = Resource.Error(message = exception.message, exception = exception)
        )

        // Then
        assertThat(error.cause).isSameInstanceAs(exception)
        assertThat(error.statusCode).isNull()
    }

    @Test
    fun `analyze passes the exception to the failure callback when getting the extractions fails`() {
        // Given
        val exception = UnknownHostException("Unable to resolve host")

        // When
        val error = analyzeAndGetFailure(
            extractionsResource = Resource.Error(message = exception.message, exception = exception)
        )

        // Then
        assertThat(error.cause).isSameInstanceAs(exception)
        assertThat(error.statusCode).isNull()
    }

    @Test
    fun `analyze failure maps to the no connection error when the session request failed offline`() {
        // Given
        // The shape core-api-library produces when its session interceptor can't reach the host
        val sessionFailure = ApiException(
            message = "Session request failed",
            cause = UnknownHostException("Unable to resolve host")
        )

        // When
        val error = analyzeAndGetFailure(
            compositeDocumentResource = Resource.Error.fromApiException(sessionFailure)
        )

        // Then
        assertThat(ErrorType.typeFromError(error)).isEqualTo(ErrorType.NO_CONNECTION)
    }

    @Test
    fun `analyze failure maps to the upload error when the request timed out`() {
        // Given
        val exception = SocketTimeoutException("timeout")

        // When
        val error = analyzeAndGetFailure(
            compositeDocumentResource = Resource.Error(message = exception.message, exception = exception)
        )

        // Then
        // Same mapping as for an upload timeout. Showing the no connection error for timeouts
        // instead is an open product question - update this test when that is decided.
        assertThat(ErrorType.typeFromError(error)).isEqualTo(ErrorType.UPLOAD)
    }

    @Test
    fun `analyze passes the response status code and headers to the failure callback`() {
        // Given
        val headers = mapOf("Retry-After" to listOf("120"))

        // When
        val error = analyzeAndGetFailure(
            compositeDocumentResource = Resource.Error(
                message = "Service unavailable",
                responseStatusCode = 503,
                responseHeaders = headers
            )
        )

        // Then
        assertThat(error.statusCode).isEqualTo(503)
        assertThat(error.headers).isEqualTo(headers)
    }

    /**
     * Uploads a page successfully, then runs the analysis with the given Gini Bank API
     * responses and returns the error passed to the failure callback.
     */
    private fun analyzeAndGetFailure(
        compositeDocumentResource: Resource<ApiDocument> = Resource.Success(
            createApiDocument("id2", ApiDocument.SourceClassification.COMPOSITE)
        ),
        extractionsResource: Resource<ExtractionsContainer> = Resource.Success(mockk())
    ): Error {
        UserAnalytics.initialize(getApplicationContext())
        val partialDocument = createApiDocument("id1", ApiDocument.SourceClassification.SCANNED)

        val documentManager = mockk<BankApiDocumentManager>()
        coEvery {
            documentManager.createPartialDocument(any(), any(), null, null, any())
        } returns Resource.Success(partialDocument)
        coEvery {
            documentManager.createCompositeDocument(any<LinkedHashMap<ApiDocument, Int>>(), any(), any())
        } returns compositeDocumentResource
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns extractionsResource

        val bankApi = mockk<GiniBankAPI>()
        every { bankApi.documentManager } returns documentManager

        val networkService = GiniCaptureDefaultNetworkService(bankApi, null, getApplicationContext())

        val captureDocument = mockk<Document>()
        every { captureDocument.id } returns "id"
        every { captureDocument.data } returns byteArrayOf()
        every { captureDocument.mimeType } returns "image/jpeg"
        every { captureDocument.generateUploadMetadata(getApplicationContext()) } returns ""

        networkService.upload(captureDocument, mockk(relaxed = true))
        shadowOf(Looper.getMainLooper()).idle()

        var failure: Error? = null
        networkService.analyze(
            linkedMapOf(partialDocument.id to 0),
            object : GiniCaptureNetworkCallback<AnalysisResult, Error> {
                override fun success(result: AnalysisResult) {
                    error("Should not succeed")
                }

                override fun failure(error: Error) {
                    failure = error
                }

                override fun cancelled() {
                    error("Should not cancel")
                }
            }
        )
        shadowOf(Looper.getMainLooper()).idle()

        return checkNotNull(failure) { "The failure callback was not called" }
    }

    private fun createApiDocument(id: String, sourceClassification: ApiDocument.SourceClassification) =
        ApiDocument(
            id,
            ApiDocument.ProcessingState.COMPLETED,
            "filename-$id",
            1,
            Date(),
            Date(),
            sourceClassification,
            Uri.EMPTY,
            listOf(),
            listOf()
        )
}
