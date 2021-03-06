package net.gini.android.core.api.models;

import static net.gini.android.core.api.Utils.checkNotNull;
import static net.gini.android.core.api.internal.BundleHelper.bundleToMap;
import static net.gini.android.core.api.internal.BundleHelper.mapToBundle;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 13.02.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

/**
 * The ExtractionsContainer contains specific extractions (e.g. "amountToPay"), compound extractions (e.g. "lineItems")
 * and return reasons (used to allow users to specify in the Return Assistant why they return an item).
 * <p>
 * See the
 * <a href="http://developer.gini.net/gini-api/html/document_extractions.html">Gini API documentation</a>
 * for a list of the names of the specific extractions and compound specific extractions.
 */
public class ExtractionsContainer implements Parcelable {

    private final Map<String, SpecificExtraction> mSpecificExtractions;
    private final Map<String, CompoundExtraction> mCompoundExtractions;

    /**
     * Contains a document's extractions from the Gini API.
     *
     * @param specificExtractions
     * @param compoundExtractions
     */
    public ExtractionsContainer(@NonNull final Map<String, SpecificExtraction> specificExtractions,
            @NonNull final Map<String, CompoundExtraction> compoundExtractions) {
        mSpecificExtractions = checkNotNull(specificExtractions);
        mCompoundExtractions = checkNotNull(compoundExtractions);
    }

    @NonNull
    public Map<String, SpecificExtraction> getSpecificExtractions() {
        return mSpecificExtractions;
    }

    @NonNull
    public Map<String, CompoundExtraction> getCompoundExtractions() {
        return mCompoundExtractions;
    }

    protected ExtractionsContainer(Parcel in) {
        mSpecificExtractions = bundleToMap(in.readBundle(getClass().getClassLoader()));
        mCompoundExtractions = bundleToMap(in.readBundle(getClass().getClassLoader()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBundle(mapToBundle(mSpecificExtractions));
        dest.writeBundle(mapToBundle(mCompoundExtractions));
    }

    public static final Creator<ExtractionsContainer> CREATOR = new Creator<ExtractionsContainer>() {
        @Override
        public ExtractionsContainer createFromParcel(Parcel in) {
            return new ExtractionsContainer(in);
        }

        @Override
        public ExtractionsContainer[] newArray(int size) {
            return new ExtractionsContainer[size];
        }
    };
}
