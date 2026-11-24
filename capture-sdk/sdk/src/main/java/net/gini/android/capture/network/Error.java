package net.gini.android.capture.network;

/**
 * Created by Alpar Szotyori on 29.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.internal.util.FileImportValidator;

/**
 * Used by the {@link GiniCaptureNetworkService} to return error messages.
 */
public class Error {

    private String mMessage;
    private Throwable mCause;

    private Integer mStatusCode;
    private Map<String, List<String>> mHeaders;
    private FileImportValidator.Error mFileImportErrors;
    private Exception mException;

    /**
     * Create a new error.
     *
     * @param message error message
     */
    public Error(@NonNull final String message) {
        mMessage = message;
        mCause = null;
    }

    /**
     * Create a new error with a cause.
     *
     * @param message error message
     * @param cause the cause of the error
     */
    public Error(@NonNull final String message, @NonNull final Throwable cause) {
        mMessage = message;
        mCause = cause;
    }

    /**
     * Create a new error with status code and headers.
     *
     * The error message is the exception's message. If it has none, then the message names the
     * exception or, without an exception, the status code.
     *
     * @param statusCode API response status code
     * @param headers API response headers
     * @param exception the cause of the error
     */
    public Error(final Integer statusCode,final Map<String, List<String>> headers, final Throwable exception) {
        mStatusCode = statusCode;
        mHeaders = headers;
        mCause = exception;
        mMessage = createMessage(statusCode, exception);
    }

    @NonNull
    private static String createMessage(@Nullable final Integer statusCode,
            @Nullable final Throwable exception) {
        if (exception != null && exception.getMessage() != null) {
            return exception.getMessage();
        }
        if (exception != null) {
            return exception.getClass().getName();
        }
        if (statusCode != null) {
            return "Request failed with status code " + statusCode;
        }
        return "Request failed";
    }

    /**
     * Create a new error for file handling
     *
     * @param fileImportErrors import error type
     */
    public Error(FileImportValidator.Error fileImportErrors) {
        mFileImportErrors = fileImportErrors;
        mMessage = "File import failed: " + fileImportErrors;
    }

    /**
     * @return error message
     */
    @NonNull
    public String getMessage() {
        return mMessage;
    }

    /**
     * @return error cause
     */
    @Nullable
    public Throwable getCause() {
        return mCause;
    }

    /**
     * @return error status code
     */
    @Nullable
    public Integer getStatusCode() {
        return mStatusCode;
    }

    /**
     * @return error response headers
     */
    @Nullable
    public Map<String, List<String>> getHeaders() {
        return mHeaders;
    }

    /**
     * @return error file import type
     */
    public FileImportValidator.Error getFileImportErrors() {
        return mFileImportErrors;
    }
}
