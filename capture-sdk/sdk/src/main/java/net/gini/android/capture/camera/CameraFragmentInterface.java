package net.gini.android.capture.camera;

import androidx.annotation.NonNull;

/**
 * <p>
 *     Methods which Camera Fragment must implement.
 * </p>
 */
public interface CameraFragmentInterface {

    /**
     * <p>
     *     Set a listener for camera events.
     * </p>
     * <p>
     *     By default the hosting Activity is expected to implement
     *     the {@link CameraFragmentListener}. In case that is not feasible you may set the
     *     listener using this method.
     * </p>
     * <p>
     *     <b>Note:</b> the listener is expected to be available until the fragment is
     *     attached to an activity. Make sure to set the listener before that.
     * </p>
     * @param listener the {@link CameraFragmentListener} instance
     */
    void setListener(@NonNull final CameraFragmentListener listener);

    /**
     * <p>
     *     Call this method to show the interface elements. The camera preview is always visible.
     * </p>
     * <p>
     *     <b>Note:</b> the interface elements are shown by default.
     * </p>
     *
     */
    void showInterface();

    /**
     * <p>
     *     Call this method to hide the interface elements. The camera preview remains visible.
     * </p>
     * <p>
     *     <b>Note:</b> the interface elements are shown by default.
     * </p>
     *
     */
    void hideInterface();

    /**
     * <p>
     *     Call this method to show an activity indicator and disable user interaction.
     *     The camera preview remains visible.
     * </p>
     */
    void showActivityIndicatorAndDisableInteraction();

    /**
     * <p>
     *     Call this method to hide the activity indicator and enable user interaction.
     * </p>
     */
    void hideActivityIndicatorAndEnableInteraction();

    /**
     * <p>
     *     Call this method to show an error message to the user in the Camera Screen.
     * </p>
     *
     * @param message  a short error message
     * @param duration how long should the error message be shown in ms
     */
    void showError(@NonNull String message, int duration);

}
