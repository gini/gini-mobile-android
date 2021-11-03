package net.gini.android.capture;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * <p>
 *     Provides details about the error which caused the Gini Capture SDK to fail.
 * </p>
 */
public class GiniCaptureError implements Parcelable {

    /**
     * <p>
     *     Definition of Gini Capture SDK error codes.
     * </p>
     */
    public enum ErrorCode {
        /**
         * <p>
         *     Couldn't get access to the camera. Most likely cause is either not declaring the camera permission in
         *     the manifest and on Android 6+ not requesting the camera permission from the user. Check the message for details.
         * </p>
         */
        CAMERA_NO_ACCESS,
        /**
         * <p>
         *     Camera couldn't be opened due to an unexpected error. Check the message for details.
         * </p>
         */
        CAMERA_OPEN_FAILED,
        /**
         * <p>
         *     Camera preview could not be started. Likely causes are that the camera was closed before preview could start
         *     or the View used to show the preview images couldn't initialize. Check the message for details.
         * </p>
         */
        CAMERA_NO_PREVIEW,
        /**
         * <p>
         *     Camera couldn't take a picture. Likely causes are that the camera was closed between requesting a picture and
         *     taking the picture or the camera didn't return an image. Check the message for details.
         * </p>
         */
        CAMERA_SHOT_FAILED,
        /**
         * <p>
         *     An unexpected camera error occurred. Check the message for details.
         * </p>
         */
        CAMERA_UNKNOWN,
        /**
         * <p>
         *     An error occurred in the Review Screen. Check the message for details.
         * </p>
         */
        REVIEW,
        /**
         * <p>
         *     An error occurred while a document was imported from the device. Check the messages
         *     for details.
         * </p>
         */
        DOCUMENT_IMPORT,
        /**
         * <p>
         *     An error occurred in the Analysis Screen. Check the message for details.
         * </p>
         */
        ANALYSIS,
        /**
         * The [GiniCapture] instance is missing. Most likely cause is an application process
         * restart.
         */
        MISSING_GINI_CAPTURE_INSTANCE
    }

    private final ErrorCode mErrorCode;
    private final String mMessage;

    /**
     * Internal use only.
     *
     * @suppress
     */
    public GiniCaptureError(final ErrorCode code, final String message) {
        mErrorCode = code;
        mMessage = message;
    }

    /**
     * <p>
     *     Use the {@link ErrorCode} to find the cause of the error.
     * </p>
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return mErrorCode;
    }

    /**
     * <p>
     *     Use the error message to find out the details about the error.
     * </p>
     * <p>
     *     <b>Note:</b> you should not show this message to the user. It is for logging and debugging purposes only.
     * </p>
     * @return the error message
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(mErrorCode.ordinal());
        dest.writeString(mMessage);
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final Creator<GiniCaptureError> CREATOR = new Creator<GiniCaptureError>() {
        @Override
        public GiniCaptureError createFromParcel(final Parcel in) {
            return new GiniCaptureError(in);
        }

        @Override
        public GiniCaptureError[] newArray(final int size) {
            return new GiniCaptureError[size];
        }
    };

    private GiniCaptureError(final Parcel in) {
        mErrorCode = ErrorCode.values()[in.readInt()];
        mMessage = in.readString();
    }
}
