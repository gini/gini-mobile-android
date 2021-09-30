package net.gini.android.models;

import static net.gini.android.Utils.checkNotNull;
import static net.gini.android.internal.BundleHelper.bundleListToMapList;
import static net.gini.android.internal.BundleHelper.mapListToBundleList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Alpar Szotyori on 13.02.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */
public class CompoundExtraction implements Parcelable {

    private final String mName;
    private final List<Map<String, SpecificExtraction>> mSpecificExtractionMaps;

    /**
     * Value object for a compound extraction from the Gini API.
     *
     * @param name                      The compound extraction's name, e.g. "amountToPay".
     * @param specificExtractionMaps    A list of specific extractions bundled into separate maps.
     */
    public CompoundExtraction(@NonNull final String name,
            @NonNull final List<Map<String, SpecificExtraction>> specificExtractionMaps) {
        mName = checkNotNull(name);
        mSpecificExtractionMaps = checkNotNull(specificExtractionMaps);
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public List<Map<String, SpecificExtraction>> getSpecificExtractionMaps() {
        return mSpecificExtractionMaps;
    }

    protected CompoundExtraction(final Parcel in) {
        mName = in.readString();
        final List<Bundle> bundleList = new ArrayList<>();
        in.readTypedList(bundleList, Bundle.CREATOR);
        mSpecificExtractionMaps = bundleListToMapList(bundleList, getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeTypedList(mapListToBundleList(mSpecificExtractionMaps));
    }

    public static final Parcelable.Creator<CompoundExtraction> CREATOR = new Parcelable.Creator<CompoundExtraction>() {
        @Override
        public CompoundExtraction createFromParcel(Parcel in) {
            return new CompoundExtraction(in);
        }

        @Override
        public CompoundExtraction[] newArray(int size) {
            return new CompoundExtraction[size];
        }
    };
}
