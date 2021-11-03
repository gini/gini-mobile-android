package net.gini.android.capture;

/**
 * Callback interface for asynchronous tasks.
 */
public interface AsyncCallback<T, E extends Exception> {

    void onSuccess(T result);

    void onError(E exception);

    void onCancelled();
}
