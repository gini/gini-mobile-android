package net.gini.android.capture;

import android.content.Context;
import android.content.Intent;

import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.internal.fileimport.AbstractImportImageUrisAsyncTask;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 22.05.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

class ImportImageFileUrisAsyncTask extends AbstractImportImageUrisAsyncTask {

    private ImportedFileValidationException mException;

    protected ImportImageFileUrisAsyncTask(@NonNull final Context context,
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
    protected void onError(@NonNull final ImageMultiPageDocument multiPageDocument,
            @NonNull final ImportedFileValidationException exception) {
        mException = exception;
    }

    @Override
    protected boolean shouldHaltOnError(@NonNull final ImageMultiPageDocument multiPageDocument,
            @NonNull final ImportedFileValidationException exception) {
        mException = exception;
        return true;
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
}
