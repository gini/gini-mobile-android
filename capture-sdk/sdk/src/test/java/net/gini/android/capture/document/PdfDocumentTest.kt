package net.gini.android.capture.document

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.Document
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfDocumentTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `equals itself`() {
        val document = newPdfDocument()

        assertThat(document == document).isTrue()
    }

    @Test
    fun `does not equal another pdf document`() {
        assertThat(newPdfDocument()).isNotEqualTo(newPdfDocument())
    }

    @Test
    fun `loads the filename from its uri`() {
        val document = newPdfDocument()

        document.loadFilename(context)

        assertThat(document.filename).isEqualTo(PDF_FILENAME)
    }

    @Test
    fun `has no filename when it cannot be determined from the uri`() {
        val document = PdfDocument(
            null,
            Uri.fromFile(File(context.cacheDir, "missing.pdf")),
            Document.Source.newExternalSource(),
            Document.ImportMethod.PICKER
        )

        document.loadFilename(context)

        assertThat(document.filename).isNull()
    }

    @Test
    fun `hash code takes the filename into account`() {
        val document = newPdfDocument()
        val hashCodeWithoutFilename = document.hashCode()

        document.loadFilename(context)

        assertThat(document.hashCode()).isNotEqualTo(hashCodeWithoutFilename)
    }

    private fun newPdfDocument(): PdfDocument {
        val file = File(context.cacheDir, PDF_FILENAME).apply { writeText("pdf") }
        return PdfDocument(null, Uri.fromFile(file), Document.Source.newExternalSource(), Document.ImportMethod.PICKER)
    }

    private companion object {
        const val PDF_FILENAME = "invoice.pdf"
    }
}
