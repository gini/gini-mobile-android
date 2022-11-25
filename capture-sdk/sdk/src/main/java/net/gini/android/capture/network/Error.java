package net.gini.android.capture.network;

/**
 * Created by Alpar Szotyori on 29.01.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

enum FileImportErrors {
    GENERIC,
    PAGE,
    SIZE,
    UNSUPPORTED,
    PASSWORD,
    CUSTOM
}
/**
 * Used by the {@link GiniCaptureNetworkService} and {@link GiniCaptureNetworkApi} to return error
 * messages.
 */
public class Error {

    private String mMessage;
    private Throwable mCause;

    private Integer mStatusCode;
    private Map<String, ArrayList<String>> mHeaders;
    private FileImportErrors mFileImportErrors;

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
     * @param statusCode API response status code
     * @param headers API response headers
     */
    public Error(final Integer statusCode,final Map<String, ArrayList<String>> headers) {
        mStatusCode = statusCode;
        mHeaders = headers;
    }

    /**
     * Create a new error for file handling
     *
     * @param fileImportErrors import error type
     */
    public Error(FileImportErrors fileImportErrors) {
        mFileImportErrors = fileImportErrors;
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
    public Map<String, ArrayList<String>> getHeaders() {
        return mHeaders;
    }

    /**
     * @return error file import type
     */
    public FileImportErrors getFileImportErrors() {
        return mFileImportErrors;
    }
}
