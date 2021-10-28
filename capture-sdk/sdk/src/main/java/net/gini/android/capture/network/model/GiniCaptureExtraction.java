package net.gini.android.capture.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contains a Gini API <a href="http://developer.gini.net/gini-api/html/document_extractions.html#extractions">extraction</a>.
 *
 * <p> An extraction contains an entity describing the general semantic type of the extraction (e.g.
 * a date), which also determines the format of the value containing the information as text.
 * Optionally there may be a box describing the position of the extraction value on the document. In
 * most instances, extractions without a bounding box are meta information (e.g. doctype).
 */
public class GiniCaptureExtraction implements Parcelable {

    public static final Creator<GiniCaptureExtraction> CREATOR =
            new Creator<GiniCaptureExtraction>() {

                @Override
                public GiniCaptureExtraction createFromParcel(final Parcel in) {
                    return new GiniCaptureExtraction(in);
                }

                @Override
                public GiniCaptureExtraction[] newArray(final int size) {
                    return new GiniCaptureExtraction[size];
                }

            };

    private final String mEntity;
    private String mValue;
    private GiniCaptureBox mGiniCaptureBox;
    private boolean mIsDirty;

    /**
     * Value object for an extraction from the Gini API.
     *
     * @param value  normalized textual representation of the text/information provided by the
     *               extraction value (e.g. bank number without spaces between the digits).
     *               Changing this value marks the extraction as dirty
     * @param entity key (primary identification) of an entity type (e.g. banknumber). See <a
     *               href="http://developer.gini.net/gini-api/html/document_extractions.html#available-extraction-entities">Extraction
     *               Entities</a> for a full list
     * @param box    (optional) bounding box containing the position of the extraction value on the
     *               document. Only available for some extractions. Changing this value marks the
     *               extraction as dirty
     */
    public GiniCaptureExtraction(@NonNull final String value, @NonNull final String entity,
                                 @Nullable final GiniCaptureBox box) {
        mValue = value;
        mEntity = entity;
        mGiniCaptureBox = box;
        mIsDirty = false;
    }

    protected GiniCaptureExtraction(final Parcel in) {
        mEntity = in.readString();
        mValue = in.readString();
        mGiniCaptureBox = in.readParcelable(GiniCaptureBox.class.getClassLoader());
        mIsDirty = in.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mEntity);
        dest.writeString(getValue());
        dest.writeParcelable(getBox(), flags);
        dest.writeInt(isDirty() ? 1 : 0);
    }

    /**
     * @return normalized textual representation of the text/information provided by the extraction
     * value (e.g. bank number without spaces between the digits)
     */
    @NonNull
    public synchronized String getValue() {
        return mValue;
    }

    /**
     * Set a new value for this extraction. Marks the extraction as dirty.
     *
     * @param newValue new value
     */
    public synchronized void setValue(@NonNull final String newValue) {
        mValue = newValue;
        mIsDirty = true;
    }

    /**
     * @return key (primary identification) of an entity type (e.g. banknumber). See <a
     * href="http://developer.gini.net/gini-api/html/document_extractions.html#available-extraction-entities">Extraction
     * Entities</a> for a full list
     */
    @NonNull
    public synchronized String getEntity() {
        return mEntity;
    }

    /**
     * @return bounding box containing the position of the extraction value on the document
     */
    @Nullable
    public synchronized GiniCaptureBox getBox() {
        return mGiniCaptureBox;
    }

    /**
     * Set a new bounding box. Marks the extraction as dirty.
     *
     * @param newBox new bounding box
     */
    public synchronized void setBox(@Nullable final GiniCaptureBox newBox) {
        mGiniCaptureBox = newBox;
        mIsDirty = true;
    }

    /**
     * @return {@code true} if the value or the bounding box has been changed
     */
    public synchronized boolean isDirty() {
        return mIsDirty;
    }

    /**
     * @param isDirty pass {@code true} to mark the extraction as dirty
     */
    public synchronized void setIsDirty(final boolean isDirty) {
        mIsDirty = isDirty;
    }

    @Override
    public String toString() {
        return "GiniCaptureExtraction{"
                + "mEntity='" + mEntity + '\''
                + ", mValue='" + mValue + '\''
                + ", mGiniCaptureBox=" + mGiniCaptureBox
                + ", mIsDirty=" + mIsDirty
                + '}';
    }
}
