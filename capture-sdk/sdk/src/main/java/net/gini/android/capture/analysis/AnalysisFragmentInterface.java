package net.gini.android.capture.analysis;

import android.view.View;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 */
public interface AnalysisFragmentInterface {

    /**
     * <p>
     *     Set a listener for analysis events.
     * </p>
     * <p>
     *     By default the hosting Activity is expected to implement
     *     the {@link AnalysisFragmentListener}. In case that is not feasible you may set the
     *     listener using this method.
     * </p>
     * <p>
     *     <b>Note:</b> the listener is expected to be available until the fragment is
     *     attached to an activity. Make sure to set the listener before that.
     * </p>
     * @param listener {@link AnalysisFragmentListener} instance
     */
    void setListener(@NonNull final AnalysisFragmentListener listener);
}
