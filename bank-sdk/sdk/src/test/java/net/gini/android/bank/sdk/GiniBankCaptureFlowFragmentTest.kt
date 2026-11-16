package net.gini.android.bank.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.capture.network.GiniCaptureNetworkService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for the capture flow fragment entry points of [GiniBank]:
 * [GiniBank.createCaptureFlowFragment] and [GiniBank.createCaptureFlowFragmentForIntent].
 */
// sdk 33: on newer emulated SDKs PdfRenderer delegates to PdfProcessor, which throws
// NoSuchMethodError under Robolectric and cannot be caught by FileImportValidator
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GiniBankCaptureFlowFragmentTest {

    private lateinit var context: Context
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(MimeTypeMap.getSingleton())
            .addExtensionMimeTypeMapping("pdf", "application/pdf")
    }

    @After
    fun tearDown() {
        GiniBank.cleanupCapture(context)
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    @Test
    fun `createCaptureFlowFragment throws IllegalStateException when capture is not configured`() {
        // When
        val exception = runCatching { GiniBank.createCaptureFlowFragment() }.exceptionOrNull()

        // Then
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception).hasMessageThat().isEqualTo(NOT_CONFIGURED_MESSAGE)
    }

    @Test
    fun `createCaptureFlowFragment returns a CaptureFlowFragment when capture is configured`() {
        // Given
        configureCapture()

        // When
        val fragment = GiniBank.createCaptureFlowFragment()

        // Then
        assertThat(fragment).isNotNull()
    }

    @Test
    fun `createCaptureFlowFragmentForIntent throws IllegalStateException when capture is not configured`() {
        // When
        val exception = runCatching {
            GiniBank.createCaptureFlowFragmentForIntent(context, createPdfIntent()) {}
        }.exceptionOrNull()

        // Then
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception).hasMessageThat().isEqualTo(NOT_CONFIGURED_MESSAGE)
    }

    private fun configureCapture() {
        val mockNetworkService = mockk<GiniCaptureNetworkService>(relaxed = true)
        GiniBank.setCaptureConfiguration(context, CaptureConfiguration(networkService = mockNetworkService))
    }

    private fun createPdfIntent(): Intent =
        Intent(Intent.ACTION_VIEW).setDataAndType(createPdfUri(), "application/pdf")

    private fun createPdfUri(): Uri {
        val file = File.createTempFile("gini-bank-capture-flow-test", ".pdf", context.cacheDir)
        file.writeBytes("%PDF-1.4 test pdf content".toByteArray())
        tempFiles.add(file)
        return Uri.fromFile(file)
    }

    private companion object {
        const val NOT_CONFIGURED_MESSAGE =
            "Capture feature is not configured. Call setCaptureConfiguration before creating the CaptureFlowFragment."
    }
}
