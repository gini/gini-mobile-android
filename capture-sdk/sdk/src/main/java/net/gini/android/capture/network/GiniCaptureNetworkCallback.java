package net.gini.android.capture.network;

/**
 * Created by Alpar Szotyori on 22.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Used by the {@link GiniCaptureNetworkService} and {@link GiniCaptureNetworkApi} to return the
 * outcome of network calls.
 *
 * @param <R> result type
 * @param <E> error type
 */
public interface GiniCaptureNetworkCallback<R, E> {

    /**
     * Called when the network call failed.
     *
     * @param error failure error
     */
    void failure(E error);

    /**
     * Called when the network call completed successfully.
     *
     * @param result network call result
     */
    void success(R result);

    /**
     * Called when the network call has been cancelled.
     */
    void cancelled();
}
