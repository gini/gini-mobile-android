package net.gini.android.capture.network

import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import bolts.Task
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import net.gini.android.bank.api.BankApiDocumentTaskManager
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.capture.Document
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.lang.Thread.sleep
import java.text.DateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

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
        // Mock Gini Bank API documents
        val partialDocument = net.gini.android.core.api.models.Document(
            "id1",
            net.gini.android.core.api.models.Document.ProcessingState.COMPLETED,
            "filename1",
            1,
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
            net.gini.android.core.api.models.Document.SourceClassification.COMPOSITE,
            Uri.EMPTY,
            listOf(),
            listOf()
        )

        // Mock DocumentTaskManager returning the mock documents
        val documentTaskManager = mockk<BankApiDocumentTaskManager>()
        every { documentTaskManager.createPartialDocument(any(), any(), null, null) }
            .returns(
                Task.forResult(
                    partialDocument
                )
            )
        every { documentTaskManager.createCompositeDocument(any<LinkedHashMap<net.gini.android.core.api.models.Document, Int>>(), any()) }
            .returns(
                Task.forResult(
                    compositeDocument
                )
            )
        every { documentTaskManager.pollDocument(any()) } returns Task.forResult(compositeDocument)
        every { documentTaskManager.getAllExtractions(any()) } returns Task.forResult(mockk())

        // Mock GiniBankAPI
        val bankApi = mockk<GiniBankAPI>()
        every { bankApi.documentTaskManager } returns documentTaskManager

        val networkService = GiniCaptureDefaultNetworkService(bankApi, null)

        // Mock Gini Capture SDK document
        val captureDocument = mockk<Document>()
        every { captureDocument.id } returns "id"
        every { captureDocument.data } returns byteArrayOf()
        every { captureDocument.mimeType } returns "image/jpeg"

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