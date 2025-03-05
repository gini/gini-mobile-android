package net.gini.android.capture.network

import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import net.gini.android.bank.api.BankApiDocumentManager
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.capture.Document
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import net.gini.android.core.api.Resource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.*

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
        coEvery { documentManager.createPartialDocument(any(), any(), null, null, any()) } returns Resource.Success(partialDocument)
        coEvery { documentManager.createCompositeDocument(any<LinkedHashMap<net.gini.android.core.api.models.Document, Int>>(), any()) } returns Resource.Success(compositeDocument)
        coEvery { documentManager.pollDocument(any()) } returns Resource.Success(compositeDocument)
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(mockk())

        // Mock GiniBankAPI
        val bankApi = mockk<GiniBankAPI>()
        every { bankApi.documentManager } returns documentManager

        val networkService = GiniCaptureDefaultNetworkService(bankApi, null, ApplicationProvider.getApplicationContext())

        // Mock Gini Capture SDK document
        val captureDocument = mockk<Document>()
        every { captureDocument.id } returns "id"
        every { captureDocument.data } returns byteArrayOf()
        every { captureDocument.mimeType } returns "image/jpeg"
        every { captureDocument.generateUploadMetadata(ApplicationProvider.getApplicationContext()) } returns ""

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
}