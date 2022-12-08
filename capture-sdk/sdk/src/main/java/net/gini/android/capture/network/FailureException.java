package net.gini.android.capture.network;

public class FailureException extends Exception {

    public final ErrorType errorType;

    public FailureException(final ErrorType errorType) {
        this.errorType = errorType;
    }
}
