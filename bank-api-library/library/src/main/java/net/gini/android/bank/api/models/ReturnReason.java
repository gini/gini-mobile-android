package net.gini.android.bank.api.models;

import static net.gini.android.core.api.Utils.checkNotNull;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alpar Szotyori on 14.09.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

/**
 * The ReturnReason class is used to allow users to pick a reason for returning a line item in the Return Assistant.
 */
public class ReturnReason implements Parcelable {
    private final String mId;
    private final Map<String, String> mLocalizedLabels;

    /**
     * @param id the id of the return reason
     * @param localizedLabels a map of labels where the keys are two letter language codes (ISO 639-1)
     */
    public ReturnReason(@NonNull final String id, @NonNull final Map<String, String> localizedLabels) {
        mId = checkNotNull(id);
        mLocalizedLabels = checkNotNull(localizedLabels);
    }

    protected ReturnReason(Parcel in) {
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

    public static final Creator<ReturnReason> CREATOR = new Creator<ReturnReason>() {
        @Override
        public ReturnReason createFromParcel(Parcel in) {
            return new ReturnReason(in);
        }

        @Override
        public ReturnReason[] newArray(int size) {
            return new ReturnReason[size];
        }
    };
}
