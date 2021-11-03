package net.gini.android.capture;

import net.gini.android.capture.internal.util.FileImportValidator;

import androidx.annotation.Nullable;

/**
 * Thrown when a file failed validation.
 * <p>
 * Call {@link ImportedFileValidationException#getValidationError()} to find out the failure reason.
 */
public class ImportedFileValidationException extends Exception {

    private final FileImportValidator.Error mError;

    public ImportedFileValidationException(final FileImportValidator.Error error) {
        mError = error;
    }

    public ImportedFileValidationException(final String message) {
        super(message);
        mError = null;
    }

    @Nullable
    public FileImportValidator.Error getValidationError() {
        return mError;
    }
}
