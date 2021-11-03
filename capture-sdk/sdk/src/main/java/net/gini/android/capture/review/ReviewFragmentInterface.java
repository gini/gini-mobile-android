package net.gini.android.capture.review;

import androidx.annotation.NonNull;

/**
 * Methods which Review Fragment must implement.
 */
public interface ReviewFragmentInterface {

    /**
     * Set a listener for review events.
     *
     * <p> By default the hosting Activity is expected to implement the {@link
     * ReviewFragmentListener}. In case that is not feasible you may set the listener using this
     * method.
     *
     * <p> <b>Note:</b> the listener is expected to be available until the fragment is attached to
     * an activity. Make sure to set the listener before that.
     *
     * @param listener {@link ReviewFragmentListener} instance
     */
    void setListener(@NonNull final ReviewFragmentListener listener);
}
