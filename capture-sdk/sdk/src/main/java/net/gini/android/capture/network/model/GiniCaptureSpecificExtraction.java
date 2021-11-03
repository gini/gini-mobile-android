package net.gini.android.capture.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contains a Gini API <a href="http://developer.gini.net/gini-api/html/document_extractions.html#specific-extractions">specific
 * extraction</a>.
 *
 * <p> A specific extraction assings a semantic property to an extraction and it may contain a list
 * of extraction candidates.
 *
 * <p> The <a href="http://developer.gini.net/gini-api/html/document_extractions.html#available-extraction-candidates">extraction
 * candidates</a> are other suggestions for this specific extraction (e.g. all amounts on the
 * document). Candidates are of the same entity as the found extraction.
 */
public class GiniCaptureSpecificExtraction extends GiniCaptureExtraction {

    public static final Parcelable.Creator<GiniCaptureSpecificExtraction> CREATOR =
            new Parcelable.Creator<GiniCaptureSpecificExtraction>() {

                @Override
                public GiniCaptureSpecificExtraction createFromParcel(final Parcel in) {
                    return new GiniCaptureSpecificExtraction(in);
                }

                @Override
                public GiniCaptureSpecificExtraction[] newArray(final int size) {
                    return new GiniCaptureSpecificExtraction[size];
                }

            };
    private final String mName;
    private final List<GiniCaptureExtraction> mCandidates;

    /**
     * Value object for a specific extraction from the Gini API.
     *
     * @param name       The specific extraction's name, e.g. "amountToPay". See <a
     *                   href="http://developer.gini.net/gini-api/html/document_extractions.html#available-specific-extractions">Available
     *                   Specific Extractions</a> for a full list
     * @param value      normalized textual representation of the text/information provided by the
     *                   extraction value (e.g. bank number without spaces between the digits).
     *                   Changing this value marks the extraction as dirty
     * @param entity     key (primary identification) of an entity type (e.g. banknumber). See <a
     *                   href="http://developer.gini.net/gini-api/html/document_extractions.html#available-extraction-entities">Extraction
     *                   Entities</a> for a full list
     * @param box        (optional) bounding box containing the position of the extraction value on
     *                   the document. Only available for some extractions. Changing this value
     *                   marks the extraction as dirty
     * @param candidates A list containing other candidates for this specific extraction. Candidates
     *                   are of the same entity as the found extraction
     */
    public GiniCaptureSpecificExtraction(@NonNull final String name, @NonNull final String value,
                                         @NonNull final String entity,
                                         @Nullable final GiniCaptureBox box,
                                         @NonNull final List<GiniCaptureExtraction> candidates) {
        super(value, entity, box);
        mName = name;
        mCandidates = candidates;
    }

    private GiniCaptureSpecificExtraction(final Parcel in) {
        super(in);
        mName = in.readString();
        final List<GiniCaptureExtraction> candidates = new ArrayList<>();
        in.readTypedList(candidates, GiniCaptureExtraction.CREATOR);
        mCandidates = candidates;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mName);
        dest.writeTypedList(mCandidates);
    }

    /**
     * @return the specific extraction's name. See <a href="http://developer.gini.net/gini-api/html/document_extractions.html#available-specific-extractions">Available
     * Specific Extractions</a> for a full list
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * @return a list containing other candidates for this specific extraction
     */
    @NonNull
    public List<GiniCaptureExtraction> getCandidates() {
        return mCandidates;
    }

    @NonNull
    @Override
    public String toString() {
        return "GiniCaptureSpecificExtraction{"
                + "mName='" + mName + '\''
                + ", mCandidates=" + mCandidates
                + "} " + super.toString();
    }
}
