package net.gini.android.capture.network.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.gini.android.capture.internal.util.BundleHelperKt.fromMapList;
import static net.gini.android.capture.internal.util.BundleHelperKt.toMapList;

/**
 * Contains a Gini API compound extraction.
 *
 * <p> A compound extraction contains one or more specific extraction maps. For example line items are compound extractions where each line
 * is represented by a specific extraction map. Each specific extraction represents a column on that line.
 */
public class GiniCaptureCompoundExtraction implements Parcelable {

    public static final Creator<GiniCaptureCompoundExtraction> CREATOR = new Creator<GiniCaptureCompoundExtraction>() {
        @Override
        public GiniCaptureCompoundExtraction createFromParcel(final Parcel in) {
            return new GiniCaptureCompoundExtraction(in);
        }

        @Override
        public GiniCaptureCompoundExtraction[] newArray(final int size) {
            return new GiniCaptureCompoundExtraction[size];
        }
    };
    private final String mName;
    private final List<Map<String, GiniCaptureSpecificExtraction>> mSpecificExtractionMaps;

    /**
     * Value object for a compound extraction from the Gini API.
     *
     * @param name                   The compound extraction's name, e.g. "amountToPay".
     * @param specificExtractionMaps A list of specific extractions bundled into separate maps.
     */
    public GiniCaptureCompoundExtraction(@NonNull final String name,
                                         @NonNull final List<Map<String, GiniCaptureSpecificExtraction>> specificExtractionMaps) {
        mName = name;
        mSpecificExtractionMaps = specificExtractionMaps;
    }

    protected GiniCaptureCompoundExtraction(final Parcel in) {
        mName = in.readString();
        final List<Bundle> bundleList = new ArrayList<>();
        in.readTypedList(bundleList, Bundle.CREATOR);
        mSpecificExtractionMaps = toMapList(bundleList, getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeString(mName);
        dest.writeTypedList(fromMapList(mSpecificExtractionMaps));
    }

    @NonNull
    @Override
    public String toString() {
        return "GiniCaptureCompoundExtraction{"
                + "mName='" + mName + '\''
                + ", mSpecificExtractionMaps=" + mSpecificExtractionMaps
                + '}';
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public List<Map<String, GiniCaptureSpecificExtraction>> getSpecificExtractionMaps() {
        return mSpecificExtractionMaps;
    }
}
