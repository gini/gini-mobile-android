package net.gini.android.helpers;

import static android.os.Parcelable.Creator;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelHelper {

    public static <T extends Parcelable> T doRoundTrip(final T input, final Creator<T> creator) {
        // First, write to parcel
        final Parcel parcel = Parcel.obtain();
        input.writeToParcel(parcel, input.describeContents());
        // Then read from parcel
        parcel.setDataPosition(0);
        return creator.createFromParcel(parcel);
    }
}
