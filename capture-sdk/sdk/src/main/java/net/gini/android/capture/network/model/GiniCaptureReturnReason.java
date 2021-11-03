package net.gini.android.capture.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Contains a return reason.
 *
 * <p> Return reasons are shown to the user when a line item is deselected in the return assistant.
 */
public class GiniCaptureReturnReason implements Parcelable {

    private final String mId;
    private final Map<String, String> mLocalizedLabels;

    /**
     * @param id the id of the return reason
     * @param localizedLabels a map of labels where the keys are two letter language codes (ISO 639-1)
     */
    public GiniCaptureReturnReason(@NonNull final String id, @NonNull final Map<String, String> localizedLabels) {
        mId = id;
        mLocalizedLabels = localizedLabels;
    }

    protected GiniCaptureReturnReason(Parcel in) {
        mId = in.readString();
        final int mapSize = in.readInt();
        mLocalizedLabels = new HashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            mLocalizedLabels.put(in.readString(), in.readString());
        }
    }

    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * @return a map of labels where the keys are two letter language codes (ISO 639-1)
     */
    @NonNull
    public Map<String, String> getLocalizedLabels() {
        return mLocalizedLabels;
    }

    @Nullable
    public String getLabelInLocalLanguageOrGerman() {
        final String label = mLocalizedLabels.get(Locale.getDefault().getLanguage());
        return label != null ? label : mLocalizedLabels.get("de");
    }

    @Override
    public String toString() {
        return "GiniCaptureReturnReason{" +
                "mId='" + mId + '\'' +
                ", mLocalizedLabels=" + mLocalizedLabels +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeInt(mLocalizedLabels.size());
        for (final Map.Entry<String, String> entry : mLocalizedLabels.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    public static final Creator<GiniCaptureReturnReason> CREATOR = new Creator<GiniCaptureReturnReason>() {
        @Override
        public GiniCaptureReturnReason createFromParcel(Parcel in) {
            return new GiniCaptureReturnReason(in);
        }

        @Override
        public GiniCaptureReturnReason[] newArray(int size) {
            return new GiniCaptureReturnReason[size];
        }
    };
}
