package net.gini.android.capture

import android.content.Context
import android.net.Uri
import net.gini.android.capture.document.GiniCaptureDocument
import net.gini.android.capture.document.ImageMultiPageDocument
import net.gini.android.capture.document.PdfDocument
import net.gini.android.capture.document.XmlDocument
import net.gini.android.capture.internal.util.FileImportValidator
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.util.CancellationToken
import net.gini.android.capture.util.NoOpCancellationToken
import net.gini.android.capture.util.UriHelper

/**
 * Contains methods for preparing to launch the Gini Capture SDK with content Uris received from
 * another app.
 *
 * Mirrors [GiniCaptureFileImport] but takes the document Uris directly instead of unwrapping them
 * from an [android.content.Intent].
 */
internal class GiniCaptureUriImport(private val giniCapture: GiniCapture) {

    fun createDocumentForImportedUris(
        uris: List<Uri>,
        context: Context,
        callback: AsyncCallback<Document, ImportedFileValidationException>
    ): CancellationToken = when {
        !GiniCapture.hasInstance() -> {
            callback.onError(createNoGiniCaptureUriValidationException())
            NoOpCancellationToken()
        }

        uris.isEmpty() -> {
            callback.onError(ImportedFileValidationException("Uri list is empty"))
            NoOpCancellationToken()
        }

        uris.size == 1 && isPdfOrXml(uris[0], context) ->
            importPdfOrXml(uris[0], context, callback)

        else -> importImages(uris, context, callback)
    }

    private fun isPdfOrXml(uri: Uri, context: Context): Boolean =
        UriHelper.hasMimeType(uri, context, MimeType.APPLICATION_PDF.asString()) ||
            UriHelper.hasMimeType(uri, context, MimeType.TEXT_XML.asString()) ||
            UriHelper.hasMimeType(uri, context, MimeType.APPLICATION_XML.asString())

    private fun importPdfOrXml(
        uri: Uri,
        context: Context,
        callback: AsyncCallback<Document, ImportedFileValidationException>
    ): CancellationToken {
        try {
            val document = createDocumentForImportedUri(uri, context)
            document.loadData(
                context,
                object : AsyncCallback<ByteArray, Exception> {
                    override fun onSuccess(result: ByteArray) {
                        callback.onSuccess(document)
                    }

                    override fun onError(exception: Exception) {
                        callback.onError(
                            ImportedFileValidationException(
                                "Could not load data from Uri:" + exception.message
                            )
                        )
                    }

                    override fun onCancelled() {
                        callback.onCancelled()
                    }
                }
            )
        } catch (e: ImportedFileValidationException) {
            callback.onError(e)
        }
        return NoOpCancellationToken()
    }

    @Throws(ImportedFileValidationException::class)
    private fun createDocumentForImportedUri(
        uri: Uri,
        context: Context
    ): GiniCaptureDocument {
        if (!UriHelper.isUriInputStreamAvailable(uri, context)) {
            throw ImportedFileValidationException("InputStream not available for the Uri")
        }
        val fileSizeLimit = if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().importedFileSizeBytesLimit
        } else {
            FileImportValidator.FILE_SIZE_LIMIT
        }
        val fileImportValidator = FileImportValidator(context, fileSizeLimit)
        if (!fileImportValidator.matchesCriteria(uri)) {
            throw ImportedFileValidationException(fileImportValidator.error)
        }
        return if (UriHelper.hasMimeType(uri, context, MimeType.APPLICATION_PDF.asString())) {
            PdfDocument.fromUri(uri, Document.ImportMethod.OPEN_WITH)
                .apply { loadFilename(context) }
        } else {
            XmlDocument.fromUri(uri, Document.ImportMethod.OPEN_WITH)
                .apply { loadFilename(context) }
        }
    }

    @Suppress("SpreadOperator") // AsyncTask.execute is a Java varargs method
    private fun importImages(
        uris: List<Uri>,
        context: Context,
        callback: AsyncCallback<Document, ImportedFileValidationException>
    ): CancellationToken {
        val asyncTask = ImportImageFileUrisAsyncTask(
            context,
            giniCapture,
            Document.Source.newExternalSource(),
            Document.ImportMethod.OPEN_WITH,
            object : AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException> {
                override fun onSuccess(result: ImageMultiPageDocument) {
                    if (!GiniCapture.hasInstance()) {
                        callback.onError(createNoGiniCaptureUriValidationException())
                        return
                    }
                    if (result.documents.isEmpty()) {
                        // Unlike the Intent path, which delivers an empty multi-page document,
                        // surface the "no images" failure to the integrator as an error
                        callback.onError(
                            ImportedFileValidationException("Uris did not contain images")
                        )
                        return
                    }
                    GiniCapture.getInstance().internal()
                        .imageMultiPageDocumentMemoryStore
                        .setMultiPageDocument(result)
                    callback.onSuccess(result)
                }

                override fun onError(exception: ImportedFileValidationException) {
                    callback.onError(exception)
                }

                override fun onCancelled() {
                    callback.onCancelled()
                }
            }
        )
        asyncTask.execute(*uris.toTypedArray())
        return CancellationToken {
            // No-op - mirrors the Intent-based import path
        }
    }

    private fun createNoGiniCaptureUriValidationException(): ImportedFileValidationException =
        ImportedFileValidationException(
            "Cannot import files. GiniCapture instance not available. " +
                "Create it with GiniCapture.newInstance()."
        )
}
