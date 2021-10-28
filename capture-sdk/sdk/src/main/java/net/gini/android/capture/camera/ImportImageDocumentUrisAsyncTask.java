package net.gini.android.capture.camera;

import android.content.Context;
import android.content.Intent;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.ImportedFileValidationException;
import net.gini.android.capture.R;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.GiniCaptureDocumentError;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.internal.fileimport.AbstractImportImageUrisAsyncTask;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 23.03.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

class ImportImageDocumentUrisAsyncTask extends AbstractImportImageUrisAsyncTask {

    private ImportedFileValidationException mException;

    ImportImageDocumentUrisAsyncTask(@NonNull final Context context,
            @NonNull final Intent intent, @NonNull final GiniCapture giniCapture,
            @NonNull final Document.Source source,
            @NonNull final Document.ImportMethod importMethod,
            @NonNull final AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException>
                    callback) {
        super(context, intent, giniCapture, source, importMethod, callback);
    }

    @Override
    protected void onHaltingError(@NonNull final ImportedFileValidationException exception) {
        mException = exception;
    }

    @Override
    protected void onPostExecute(final ImageMultiPageDocument multiPageDocument) {
        if (multiPageDocument != null) {
            getCallback().onSuccess(multiPageDocument);
        } else if (mException != null) {
            getCallback().onError(mException);
        } else {
            getCallback().onCancelled();
        }
    }

    @Override
    protected void onError(@NonNull final ImageMultiPageDocument multiPageDocument,
            @NonNull final ImportedFileValidationException exception) {
        addMultiPageDocumentError(getContext().getString(
                R.string.gc_document_import_invalid_document), multiPageDocument);
    }

    @Override
    protected boolean shouldHaltOnError(@NonNull final ImageMultiPageDocument multiPageDocument,
            @NonNull final ImportedFileValidationException exception) {
        addMultiPageDocumentError(getContext().getString(
                R.string.gc_document_import_invalid_document), multiPageDocument);
        return false;
    }

    private void addMultiPageDocumentError(@NonNull final String string,
            @NonNull final ImageMultiPageDocument multiPageDocument) {
        final ImageDocument document = DocumentFactory.newEmptyImageDocument(getSource(),
                getImportMethod());
        multiPageDocument.addDocument(document);
        final GiniCaptureDocumentError documentError = new GiniCaptureDocumentError(string,
                GiniCaptureDocumentError.ErrorCode.FILE_VALIDATION_FAILED);
        multiPageDocument.setErrorForDocument(document, documentError);
    }
}
