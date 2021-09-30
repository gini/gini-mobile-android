package net.gini.android;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import bolts.Task;


/**
 * Handy class to wrap Bolt's tasks around a volley request. Can be used as both the response
 * listener and error listener of a volley request.
 *
 * @param <T> The response type of the request.
 */
public class RequestTaskCompletionSource<T> implements Response.Listener<T>, Response.ErrorListener {
    private final Task<T>.TaskCompletionSource mCompletionSource;

    public static <T> RequestTaskCompletionSource<T> newCompletionSource() {
        return new RequestTaskCompletionSource<T>();
    }

    private RequestTaskCompletionSource() {
        mCompletionSource = Task.create();
    }

    /**
     * Returns the task which will be completed by this completion source.
     */
    public Task<T> getTask() {
        return mCompletionSource.getTask();
    }

    @Override
    public void onResponse(T response) {
        mCompletionSource.setResult(response);
    }

    @Override
    public synchronized void onErrorResponse(VolleyError error) {
        mCompletionSource.setError(error);
    }
}
