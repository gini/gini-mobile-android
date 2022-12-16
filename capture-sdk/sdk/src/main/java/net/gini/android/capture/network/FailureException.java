package net.gini.android.capture.network;

import net.gini.android.capture.error.ErrorType;

public class FailureException extends RuntimeException {

    public final ErrorType errorType;

    public FailureException(final ErrorType errorType) {
        this.errorType = errorType;
    }
}
