package net.gini.android.bank.screen.ui

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import net.gini.android.bank.screen.R
import net.gini.android.capture.Document
import net.gini.android.capture.camera.CameraActivity
import net.gini.android.capture.camera.CameraFragmentListener.DocumentCheckResultCallback
import net.gini.android.capture.util.IntentHelper
import net.gini.android.capture.util.UriHelper
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Implements callbacks for the Gini Capture SDK's [CameraActivity]. For example to perform
 * checks on imported documents or handle results from QR Codes.
 */
class CameraScreenApiActivity : CameraActivity() {

    override fun onCheckImportedDocument(
        document: Document,
        callback: DocumentCheckResultCallback
    ) {
        // We can apply custom checks here to an imported document and notify the Gini Capture
        // SDK about the result
        // IMPORTANT: do not call super as it will lead to unexpected behaviors

        // As an example we show how to allow only jpegs and pdfs smaller than 5MB
        if (DO_CUSTOM_DOCUMENT_CHECK) {
            // Use the Intent with which the document was imported to access its contents
            // (document.getData() may be null)
            val intent = document.intent
            if (intent == null) {
                callback.documentRejected(getString(net.gini.android.capture.R.string.gc_document_import_error))
                return
            }
            val uri = IntentHelper.getUri(intent)
            if (uri == null) {
                callback.documentRejected(getString(net.gini.android.capture.R.string.gc_document_import_error))
                return
            }
            if (hasMoreThan5MB(uri)) {
                callback.documentRejected(getString(R.string.document_size_too_large))
                return
            }
            // IMPORTANT: always call one of the callback methods
            if (isJpegOrPdf(uri)) {
                callback.documentAccepted()
            } else {
                callback.documentRejected(getString(R.string.unsupported_document_type))
            }
        } else {
            // IMPORTANT: always call one of the callback methods
            callback.documentAccepted()
        }
    }

    private fun hasMoreThan5MB(uri: Uri): Boolean {
        val fileSize = UriHelper.getFileSizeFromUri(uri, this)
        return fileSize > 5 * 1024 * 1024
    }

    private fun isJpegOrPdf(uri: Uri): Boolean {
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val magicBytes = ByteArray(4)
                val read = inputStream.read(magicBytes)
                inputStream.reset()
                return (read != -1
                        && (isJpegWithExif(inputStream, magicBytes, read)
                        || isPDF(uri, magicBytes, read)))
            }
        } catch (e: FileNotFoundException) {
            LOG.error("Could not open document", e)
            return false
        } catch (e: IOException) {
            LOG.error("Could not read document", e)
            return false
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return false
    }

    @Throws(IOException::class)
    private fun isJpegWithExif(
        inputStream: InputStream, magicBytes: ByteArray,
        read: Int
    ): Boolean {
        return isDecodableToBitmap(inputStream) && read == 4 && magicBytes[0] == 0xFF.toByte() && magicBytes[1] == 0xD8.toByte() && magicBytes[2] == 0xFF.toByte() && magicBytes[3] == 0xE1.toByte()
    }

    @Throws(IOException::class)
    private fun isDecodableToBitmap(inputStream: InputStream): Boolean {
        val options = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeStream(inputStream, Rect(), options)
        return bitmap != null
    }

    private fun isPDF(uri: Uri, magicBytes: ByteArray, read: Int): Boolean {
        return isRenderablePDF(uri) && read == 4 && magicBytes[0] == 0x25.toByte() && magicBytes[1] == 0x50.toByte() && magicBytes[2] == 0x44.toByte() && magicBytes[3] == 0x46.toByte()
    }

    private fun isRenderablePDF(uri: Uri): Boolean {
        val fileDescriptor = try {
            contentResolver.openFileDescriptor(uri, "r")
        } catch (e: FileNotFoundException) {
            LOG.error("Pdf not found", e)
            return false
        }
        if (fileDescriptor == null) {
            LOG.error("Pdf not found")
            return false
        }
        try {
            val pdfRenderer = PdfRenderer(fileDescriptor)
            pdfRenderer.close()
            return true
        } catch (e: IOException) {
            LOG.error("Could not read pdf", e)
        }
        return false
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(
            CameraScreenApiActivity::class.java
        )

        // Set to true to allow execution of the custom code check
        private const val DO_CUSTOM_DOCUMENT_CHECK = false
    }
}