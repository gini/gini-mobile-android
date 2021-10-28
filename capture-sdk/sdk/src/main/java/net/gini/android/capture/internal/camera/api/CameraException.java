package net.gini.android.capture.internal.camera.api;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * Exception that is thrown when there is some issue with the camera (i.e. no connection possible or device doesn't
 * even have a camera).
 *
 * @suppress
 */
public class CameraException extends RuntimeException {

    public enum Type {
        NO_ACCESS,
        NO_BACK_CAMERA,
        OPEN_FAILED,
        NO_PREVIEW,
        SHOT_FAILED
    }

    private final Type type;

    public CameraException(@NonNull final String detailMessage, @NonNull final Type type) {
        super(detailMessage);
        this.type = type;
    }

    public CameraException(final Throwable cause, @NonNull final Type type) {
        super(cause);
        this.type = type;
    }

    @NonNull
    public Type getType() {
        return type;
    }
}
