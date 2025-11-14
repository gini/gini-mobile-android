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
import net.gini.android.capture.Document
import net.gini.android.capture.internal.network.Configuration
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.core.api.Resource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.Date
import net.gini.android.bank.api.models.Configuration as BankConfig

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
            isPaymentDueHintEnabled = true
        )

        // Add more stubs if mapBankConfigurationToConfiguration uses other properties
        coEvery { documentManager.getConfigurations() } returns Resource.Success(bankConfig)
        val bankApi = mockk<GiniBankAPI>()
        every { bankApi.documentManager } returns documentManager

        val service = GiniCaptureDefaultNetworkService(bankApi, null, mockk())
        var called = false

        service.getConfiguration(object : GiniCaptureNetworkCallback<Configuration, Error> {
            override fun success(result: Configuration) {
                called = true
                assertThat(result).isNotNull()
            }

            override fun failure(error: Error) {
                error("Should not fail")
            }

            override fun cancelled() {
                error("Should not cancel")
            }
        })
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(called).isTrue()
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
}