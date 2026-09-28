package net.gini.android.bank.api.models;

import android.os.Parcel;

import androidx.annotation.NonNull;

import net.gini.android.core.api.models.CompoundExtraction;
import net.gini.android.core.api.models.SpecificExtraction;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Alpar Szotyori on 13.02.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

/**
 * The ExtractionsContainer contains specific extractions (e.g. "amountToPay") and compound extractions (e.g. "lineItems").
 * <p>
 * See the
 * <a href="https://pay-api.gini.net/documentation/#document-extractions-for-payment">Gini Bank API documentation</a>
 * for a list of the names of the specific extractions and compound specific extractions.
 */

// Cannot rename the class as it will be a breaking change for the clients!
@SuppressWarnings("java:S2176")
public class ExtractionsContainer extends net.gini.android.core.api.models.ExtractionsContainer {

    /**
     * Contains a document's extractions from the Gini Bank API.
     *
     * @param specificExtractions
     * @param compoundExtractions
     */
    public ExtractionsContainer(@NonNull final Map<String, SpecificExtraction> specificExtractions,
                                @NonNull final Map<String, CompoundExtraction> compoundExtractions) {
        super(specificExtractions, compoundExtractions);
    }

    /**
     * Contains a document's extractions from the Gini Bank API.
     *
     * @param specificExtractions
     * @param compoundExtractions
     * @param returnReasons ignored
     * @deprecated Return reasons are no longer supported and are ignored. Use
     * {@link #ExtractionsContainer(Map, Map)} instead. This constructor will be removed in the
     * next major version.
     */
    @Deprecated
    public ExtractionsContainer(@NonNull final Map<String, SpecificExtraction> specificExtractions,
                                @NonNull final Map<String, CompoundExtraction> compoundExtractions,
                                @NonNull final List<ReturnReason> returnReasons) {
        this(specificExtractions, compoundExtractions);
    }

    /**
     * @return an empty list
     * @deprecated Return reasons are no longer supported. This list is always empty and will be
     * removed in the next major version.
     */
    @Deprecated
    @NonNull
    public List<ReturnReason> getReturnReasons() {
        return Collections.emptyList();
    }

    protected ExtractionsContainer(Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
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
