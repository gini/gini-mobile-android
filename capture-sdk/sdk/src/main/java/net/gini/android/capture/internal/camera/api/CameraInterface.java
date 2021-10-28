package net.gini.android.capture.internal.camera.api;

import android.content.Context;
import android.graphics.Point;
import android.view.View;

import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.internal.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Internal use only.
 *
 * <p>
 *     An interface which defines an API for the camera used with the Gini Capture SDK.
 * </p>
 * <p>
 *     We use this interface with the deprecated Camera API and the new Camera2 API to publish a common API for the required
 *     camera features.
 * </p>
 *
 * @suppress
 */
public interface CameraInterface {
    /**
     * <p>
     *     Opens the first back-facing camera.
     * </p>
     * @return a {@link CompletableFuture} that completes when the camera was opened
     */
    @NonNull
    CompletableFuture<Void> open();

    /**
     * <p>
     *     Closes the camera.
     * </p>
     */
    void close();


    /**
     * <p>
     *     Starts the camera preview.
     * </p>
     */
    @NonNull
    CompletableFuture<Void> startPreview();

    /**
     * <p>
     *     Stops the camera preview.
     * </p>
     */
    void stopPreview();

    /**
     * <p>
     *     Get the state of the preview.
     * </p>
     * @return {@code true}, if the preview is running
     */
    boolean isPreviewRunning();

    /**
     * <p>
     *     Enables tap-to-focus using the given view by adding touch handling to it and transforming the touch point coordinates
     *     to the camera sensor's coordinate system.
     * </p>
     * <p>
     *     <b>Note</b>: the view should have the same size as the camera preview and be above it. You could also set the
     *     camera preview {@link android.view.SurfaceView} directly as the tap view..
     * </p>
     * @param listener the listener for tap to focus events
     */
    void enableTapToFocus(@Nullable TapToFocusListener listener);

    /**
     * Disables tap-to-focus.
     */
    void disableTapToFocus();

    /**
     * <p>
     *     Start a focus run.
     * </p>
     * @return a {@link CompletableFuture} that completes with the result of the focus operation
     */
    @NonNull
    CompletableFuture<Boolean> focus();

    /**
     * <p>
     *     Take a picture with the camera.
     * </p>
     * @return a {@link CompletableFuture} that completes with the {@link Photo} object taken
     */
    @NonNull
    CompletableFuture<Photo> takePicture();

    /**
     * <p>
     *      Set a callback to recieve preview images from the camera.
     * </p>
     * @param previewCallback callback implementation
     */
    void setPreviewCallback(@Nullable PreviewCallback previewCallback);

    /**
     * The view which shows the camera preview.
     *
     * @param context Android context
     * @return the camera preview view
     */
    View getPreviewView(@NonNull final Context context);

    boolean isFlashAvailable();

    boolean isFlashEnabled();

    void setFlashEnabled(final boolean enabled);

    /**
     * Listener for tap to focus.
     */
    interface TapToFocusListener {
        void onFocusing(@NonNull final Point point, @NonNull final Size previewViewSize);

        void onFocused(boolean success);
    }

    /**
     * Callback to receive preview images.
     */
    interface PreviewCallback {

        void onPreviewFrame(@NonNull final byte[] image, @NonNull final Size imageSize,
                            final int rotation);
    }
}
