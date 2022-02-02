package net.gini.android.bank.api.models;

import static net.gini.android.core.api.Utils.checkNotNull;
import static net.gini.android.core.api.internal.BundleHelper.bundleToMap;
import static net.gini.android.core.api.internal.BundleHelper.mapToBundle;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import net.gini.android.core.api.models.CompoundExtraction;
import net.gini.android.core.api.models.SpecificExtraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * <a href="https://pay-api.gini.net/documentation/#document-extractions-for-payment">Gini Bank API documentation</a>
 * for a list of the names of the specific extractions and compound specific extractions.
 */
public class ExtractionsContainer extends net.gini.android.core.api.models.ExtractionsContainer {

    private final List<ReturnReason> mReturnReasons;

    /**
     * Contains a document's extractions from the Gini Bank API.
     *
     * @param specificExtractions
     * @param compoundExtractions
     * @param returnReasons
     */
    public ExtractionsContainer(@NonNull final Map<String, SpecificExtraction> specificExtractions,
                                @NonNull final Map<String, CompoundExtraction> compoundExtractions,
                                @NonNull final List<ReturnReason> returnReasons) {
        super(specificExtractions,compoundExtractions);
        mReturnReasons = checkNotNull(returnReasons);
    }

    @NonNull
    public List<ReturnReason> getReturnReasons() {
        return mReturnReasons;
    }

    protected ExtractionsContainer(Parcel in) {
        super(in);
        mReturnReasons = new ArrayList<>();
        in.readTypedList(mReturnReasons, ReturnReason.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(mReturnReasons);
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
