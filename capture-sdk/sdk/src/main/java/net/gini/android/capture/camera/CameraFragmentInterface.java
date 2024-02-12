package net.gini.android.capture.camera;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * <p>
 *     Methods which Camera Fragment must implement.
 * </p>
 *
 * @suppress
 */
interface CameraFragmentInterface {

    /**
     * <p>
     *     Set a listener for camera events.
     * </p>
     * <p>
     *     <b>Note:</b> the listener is expected to be available until the fragment is
     *     attached to an activity. Make sure to set the listener before that.
     * </p>
     * @param listener the {@link CameraFragmentListener} instance
     */
    void setListener(@NonNull final CameraFragmentListener listener);

}
