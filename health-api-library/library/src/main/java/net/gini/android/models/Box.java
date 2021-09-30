package net.gini.android.models;


import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class Box implements Parcelable {
    private final int mPageNumber;
    private final double mLeft;
    private final double mTop;
    private final double mWidth;
    private final double mHeight;


    public Box(final int pageNumber, final double left, final double top, final double width, final double height) {
        mPageNumber = pageNumber;
        mLeft = left;
        mTop = top;
        mWidth = width;
        mHeight = height;
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public double getLeft() {
        return mLeft;
    }

    public double getTop() {
        return mTop;
    }

    public double getWidth() {
        return mWidth;
    }

    public double getHeight() {
        return mHeight;
    }

    public static Box fromApiResponse(JSONObject responseData) throws JSONException {
        final int pageNumber = responseData.getInt("page");
        final double left = responseData.getDouble("left");
        final double top = responseData.getDouble("top");
        final double width = responseData.getDouble("width");
        final double height = responseData.getDouble("height");
        return new Box(pageNumber, left, top, width, height);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mPageNumber);
        dest.writeDouble(mLeft);
        dest.writeDouble(mTop);
        dest.writeDouble(mWidth);
        dest.writeDouble(mHeight);
    }

    public static final Parcelable.Creator<Box> CREATOR = new Parcelable.Creator<Box>() {

        public Box createFromParcel(Parcel in) {
            final int pageNumber = in.readInt();
            final double left = in.readDouble();
            final double top = in.readDouble();
            final double width = in.readDouble();
            final double height = in.readDouble();
            return new Box(pageNumber, left, top, width, height);
        }

        public Box[] newArray(int size) {
            return new Box[size];
        }
    };
}
