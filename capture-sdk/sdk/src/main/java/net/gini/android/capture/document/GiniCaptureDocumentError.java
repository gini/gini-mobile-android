package net.gini.android.capture.document;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
/**
 * Created by Alpar Szotyori on 15.03.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public class GiniCaptureDocumentError implements Parcelable {

    public static final Creator<GiniCaptureDocumentError> CREATOR =
            new Creator<GiniCaptureDocumentError>() {
                @Override
                public GiniCaptureDocumentError createFromParcel(final Parcel in) {
                    return new GiniCaptureDocumentError(in);
                }

                @Override
                public GiniCaptureDocumentError[] newArray(final int size) {
                    return new GiniCaptureDocumentError[size];
                }
            };
    private final String mMessage;
    private final ErrorCode mErrorCode;


    public GiniCaptureDocumentError(@NonNull final String message,
                                    @NonNull final ErrorCode errorCode) {
        mMessage = message;
        mErrorCode = errorCode;
    }

    GiniCaptureDocumentError(final Parcel in) {
        mMessage = in.readString();
        mErrorCode = (ErrorCode) in.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(mMessage);
        dest.writeSerializable(mErrorCode);
    }

    @NonNull
    public String getMessage() {
        return mMessage;
    }

    @NonNull
    public ErrorCode getErrorCode() {
        return mErrorCode;
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public enum ErrorCode {
        UPLOAD_FAILED,
        FILE_VALIDATION_FAILED
    }
}
