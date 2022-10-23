package net.gini.android.capture.internal.camera.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import java.io.ByteArrayOutputStream;

public class PhotoCropModifier implements PhotoModifier {

    private final Photo mPhoto;
    private final int[] mScreenSize;
    private final Rect aRect;

    public PhotoCropModifier(Photo mPhoto, int[] mScreenSize, Rect aRect) {
        this.mPhoto = mPhoto;
        this.mScreenSize = mScreenSize;
        this.aRect = aRect;
    }

    @Override
    public void modify() {

        if (mPhoto.getData() == null)
            return;

        synchronized (aRect) {
            byte[] originalBytes = mPhoto.getData();
            try {
                float rotation = 0;

                switch (mPhoto.getRotationForDisplay() % 360) {
                    case 90:
                        rotation = 90;
                        break;
                    case 180:
                        rotation = 180;
                        break;
                    case 270:
                        rotation = 270;
                        break;
                    default:
                        break;
                }

                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);

                Bitmap originalBitmap = BitmapFactory.decodeByteArray(mPhoto.getData(),
                        0, mPhoto.getData().length);

                Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(),
                        originalBitmap.getHeight(), matrix, false);


                int x1 = rotatedBitmap.getWidth() * aRect.left / mScreenSize[0];
                int y1 = rotatedBitmap.getHeight() * aRect.top / mScreenSize[1];
                int width1 = rotatedBitmap.getWidth() * aRect.width() / mScreenSize[0];
                int height1 = rotatedBitmap.getHeight() * aRect.height() / mScreenSize[1];

                matrix.postRotate(finalRotation(rotation));

                Bitmap cropped = Bitmap.createBitmap(rotatedBitmap, x1, y1,
                        width1, height1, matrix, false);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.PNG, 100, stream);

                byte[] byteArray = stream.toByteArray();
                mPhoto.setData(byteArray);

                originalBitmap.recycle();
                cropped.recycle();

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                mPhoto.setData(originalBytes);
            }
        }
    }

    private int finalRotation(float rotation) {
        if (rotation == 90)
            return 180;
        if (rotation == 180)
            return 90;
        return 0;
    }
}
