package net.gini.android.capture.help

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import io.mockk.every
import io.mockk.mockk
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.einvoice.GetEInvoiceFeatureEnabledUseCase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupportedFormatsHelpViewModelTest {

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    private fun viewModel(
        isQrCodeDocument: Boolean,
        isEInvoiceEnabled: Boolean = false
    ) = SupportedFormatsHelpViewModel(
        isQrCodeDocument,
        mockk<GetEInvoiceFeatureEnabledUseCase> {
            every { this@mockk.invoke() } returns isEInvoiceEnabled
        }
    )

    private fun itemNames(viewModel: SupportedFormatsHelpViewModel): List<String> =
        viewModel.uiState.value.formatItems.map { it.name }

    @Test
    fun `assembles only the QR code formats for QR code documents`() {
        // When
        val viewModel = viewModel(isQrCodeDocument = true)

        // Then
        assertThat(itemNames(viewModel)).containsExactly(
            "SUPPORTED_FORMATS",
            "QR_BEZAHL",
            "QR_EPS",
            "QR_STUZZA",
            "QR_GIROCODE",
            "QR_GINI_PAYMENT"
        ).inOrder()
    }

    @Test
    fun `QR code formats do not contain e-invoices even when the feature is enabled`() {
        // When
        val viewModel = viewModel(isQrCodeDocument = true, isEInvoiceEnabled = true)

        // Then
        assertThat(itemNames(viewModel)).containsExactly(
            "SUPPORTED_FORMATS",
            "QR_BEZAHL",
            "QR_EPS",
            "QR_STUZZA",
            "QR_GIROCODE",
            "QR_GINI_PAYMENT"
        ).inOrder()
        assertThat(viewModel.uiState.value.isEInvoiceEnabled).isTrue()
    }

    @Test
    fun `assembles the default formats when nothing is configured`() {
        // When
        val viewModel = viewModel(isQrCodeDocument = false)

        // Then
        assertThat(itemNames(viewModel)).containsExactly(
            "SUPPORTED_FORMATS",
            "PRINTED_INVOICES",
            "PHOTOS_OF_MONITORS",
            "UNSUPPORTED_FORMATS",
            "HANDWRITING"
        ).inOrder()
    }

    @Test
    fun `assembles the import and QR code formats when the features are enabled`() {
        // Given
        GiniCapture.newInstance(InstrumentationRegistry.getInstrumentation().targetContext)
            .setGiniCaptureNetworkService(mock())
            .setFileImportEnabled(true)
            .setQRCodeScanningEnabled(true)
            .build()

        // When
        val viewModel = viewModel(isQrCodeDocument = false)

        // Then
        assertThat(itemNames(viewModel)).containsExactly(
            "SUPPORTED_FORMATS",
            "PRINTED_INVOICES",
            "SINGLE_PAGE_AS_JPEG_PNG_GIF",
            "PDF",
            "QR_CODE",
            "PHOTOS_OF_MONITORS",
            "UNSUPPORTED_FORMATS",
            "HANDWRITING"
        ).inOrder()
    }

    @Test
    fun `adds the e-invoice format when the feature is enabled`() {
        // When
        val viewModel = viewModel(isQrCodeDocument = false, isEInvoiceEnabled = true)

        // Then
        assertThat(itemNames(viewModel)).containsExactly(
            "SUPPORTED_FORMATS",
            "PRINTED_INVOICES",
            "PHOTOS_OF_MONITORS",
            "E_INVOICES",
            "UNSUPPORTED_FORMATS",
            "HANDWRITING"
        ).inOrder()
        assertThat(viewModel.uiState.value.isEInvoiceEnabled).isTrue()
    }
}
