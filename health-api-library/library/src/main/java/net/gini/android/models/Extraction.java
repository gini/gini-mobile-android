package net.gini.android.models;

import static net.gini.android.Utils.checkNotNull;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class Extraction implements Parcelable {
    private String mValue;
    private final String mEntity;
    private Box mBox;
    private boolean mIsDirty;

    /**
     * Value object for an extraction from the Gini API.
     *
     * @param value         The extraction's value. Changing this value marks the extraction as dirty.
     * @param entity        The extraction's entity.
     * @param box           Optional the box where the extraction is found. Only available on some extractions. Changing
     *                      this value marks the extraction as dirty.
     */
    public Extraction(final String value, final String entity, @Nullable Box box) {
        mValue = checkNotNull(value);
        mEntity = checkNotNull(entity);
        mBox = box;
        mIsDirty = false;
    }

    protected Extraction(final Parcel in) {
        mEntity = in.readString();
        mValue = in.readString();
        mBox = in.readParcelable(Box.class.getClassLoader());
        mIsDirty = in.readInt() != 0;
    }

    public synchronized String getValue() {
        return mValue;
    }

    public synchronized void setValue(final String newValue) {
        mValue = newValue;
        mIsDirty = true;
    }

    public synchronized String getEntity() {
        return mEntity;
    }

    public synchronized Box getBox() {
        return mBox;
    }

    public synchronized void setBox(Box newBox) {
        mBox = newBox;
        mIsDirty = true;
    }

    public synchronized boolean isDirty() {
        return mIsDirty;
    }

    public void setIsDirty(boolean isDirty) {
        mIsDirty = isDirty;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mEntity);
        dest.writeString(mValue);
        dest.writeParcelable(mBox, flags);
        dest.writeInt(mIsDirty ? 1 : 0);
    }

    public static final Parcelable.Creator<Extraction> CREATOR = new Parcelable.Creator<Extraction>() {

        public Extraction createFromParcel(Parcel in) {
            return new Extraction(in);
        }

        public Extraction[] newArray(int size) {
            return new Extraction[size];
        }

    };
}
