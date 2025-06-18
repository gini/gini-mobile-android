package net.gini.android.capture;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.internal.util.DeviceHelper;
import net.gini.android.capture.internal.util.FileImportValidator;
import net.gini.android.capture.internal.util.MimeType;
import net.gini.android.capture.util.CancellationToken;
import net.gini.android.capture.util.IntentHelper;
import net.gini.android.capture.util.NoOpCancellationToken;
import net.gini.android.capture.util.UriHelper;

import java.util.List;

import static net.gini.android.capture.internal.util.FileImportValidator.FILE_SIZE_LIMIT;

/**
 * This class contains methods for preparing launching the Gini Capture SDK with a file received
 * from another app.
 */
public final class GiniCaptureFileImport {

    @NonNull
    private final GiniCapture mGiniCapture;

    @NonNull
    private static GiniCaptureDocument createDocumentForImportedFile(@NonNull final Intent intent,
                                                                     @NonNull final Context context) throws ImportedFileValidationException {
        final Uri uri = IntentHelper.getUri(intent);
        if (uri == null) {
            throw new ImportedFileValidationException("Intent data did not contain a Uri");
        }
        if (!UriHelper.isUriInputStreamAvailable(uri, context)) {
            throw new ImportedFileValidationException(
                    "InputStream not available for Intent's data Uri");
        }
        final int fileSizeLimit;
        if (GiniCapture.hasInstance()) {
            fileSizeLimit = GiniCapture.getInstance().getImportedFileSizeBytesLimit();
        } else {
            fileSizeLimit = FILE_SIZE_LIMIT;
        }
        final FileImportValidator fileImportValidator = new FileImportValidator(context, fileSizeLimit);
        if (fileImportValidator.matchesCriteria(intent, uri)) {
            return DocumentFactory.newDocumentFromIntent(intent, context,
                    DeviceHelper.getDeviceOrientation(context), DeviceHelper.getDeviceType(context),
                    Document.ImportMethod.OPEN_WITH);
        } else {
            throw new ImportedFileValidationException(fileImportValidator.getError());
        }
    }

    GiniCaptureFileImport(@NonNull final GiniCapture giniCapture) {
        mGiniCapture = giniCapture;
    }


    CancellationToken createDocumentForImportedFiles(@NonNull final Intent intent,
            @NonNull final Context context,
            @NonNull final AsyncCallback<Document, ImportedFileValidationException> callback) {
        if (!GiniCapture.hasInstance()) {
            callback.onError(createNoGiniCaptureFileValidationException());
            return new NoOpCancellationToken();
        }
        final List<Uri> uris = IntentHelper.getUris(intent);
        if (uris == null) {
            callback.onError(
                    new ImportedFileValidationException("Intent data did not contain Uris"));
            return new NoOpCancellationToken();
        }
        if (uris.size() == 1 && (
                UriHelper.hasMimeType(uris.get(0), context, MimeType.APPLICATION_PDF.asString()) ||
                UriHelper.hasMimeType(uris.get(0), context, MimeType.TEXT_XML.asString()) ||
                UriHelper.hasMimeType(uris.get(0), context, MimeType.APPLICATION_XML.asString())
        )) {
            try {
                final GiniCaptureDocument document = createDocumentForImportedFile(intent,
                        context);
                document.loadData(context, new AsyncCallback<byte[], Exception>() {
                    @Override
                    public void onSuccess(byte[] result) {
                        callback.onSuccess(document);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(new ImportedFileValidationException("Could not load data from Uri:" + exception.getMessage()));
                    }

                    @Override
                    public void onCancelled() {
                        callback.onCancelled();
                    }
                });

            } catch (final ImportedFileValidationException e) {
                callback.onError(e);
            }
            return new NoOpCancellationToken();
        } else {
            final ImportImageFileUrisAsyncTask asyncTask = new ImportImageFileUrisAsyncTask(context,
                    intent,
                    mGiniCapture, Document.Source.newExternalSource(),
                    Document.ImportMethod.OPEN_WITH,
                    new AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException>() {
                        @Override
                        public void onSuccess(final ImageMultiPageDocument result) {
                            if (!GiniCapture.hasInstance()) {
                                callback.onError(createNoGiniCaptureFileValidationException());
                                return;
                            }
                            GiniCapture.getInstance().internal()
                                    .getImageMultiPageDocumentMemoryStore()
                                    .setMultiPageDocument(result);
                            callback.onSuccess(result);
                        }

                        @Override
                        public void onError(final ImportedFileValidationException exception) {
                            callback.onError(exception);
                        }

                        @Override
                        public void onCancelled() {
                            callback.onCancelled();
                        }
                    });
            asyncTask.execute(uris.toArray(new Uri[uris.size()]));
            return new CancellationToken() {
                @Override
                public void cancel() {
                }
            };
        }
    }

    @NonNull
    private static ImportedFileValidationException createNoGiniCaptureFileValidationException() {
        return new ImportedFileValidationException(
                "Cannot import files. GiniCapture instance not available. Create it with GiniCapture.newInstance().");
    }

}
