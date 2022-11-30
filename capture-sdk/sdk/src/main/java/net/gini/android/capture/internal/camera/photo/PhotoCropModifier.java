package net.gini.android.capture.internal.camera.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Size;

import java.io.ByteArrayOutputStream;

public class PhotoCropModifier implements PhotoModifier {

    private final Photo mPhoto;
    private final Size mCameraPreviewSize;
    private final Rect mCropRect;
    private final int mQuality;


    public PhotoCropModifier(Photo mPhoto, Size cameraPreviewSize, Rect cropRect, int mQuality) {
        this.mPhoto = mPhoto;
        this.mCameraPreviewSize = cameraPreviewSize;
        this.mCropRect = cropRect;
        this.mQuality = mQuality;
    }

    @Override
    public void modify() {

        if (mPhoto.getData() == null)
            return;

        synchronized (mPhoto) {

            byte[] originalBytes = mPhoto.getData();

            try {

                float rotation = mPhoto.getRotationForDisplay() % 360;

                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);

                Bitmap originalBitmap = BitmapFactory.decodeByteArray(mPhoto.getData(),
                        0, mPhoto.getData().length);

                Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(),
                        originalBitmap.getHeight(), matrix, false);


                int x1 = rotatedBitmap.getWidth() * mCropRect.left / mCameraPreviewSize.getWidth();
                int y1 = rotatedBitmap.getHeight() * mCropRect.top / mCameraPreviewSize.getHeight();
                int width1 = rotatedBitmap.getWidth() * mCropRect.width() / mCameraPreviewSize.getWidth();
                int height1 = rotatedBitmap.getHeight() * mCropRect.height() / mCameraPreviewSize.getHeight();

                Bitmap cropped = Bitmap.createBitmap(rotatedBitmap, x1, y1,
                        width1, height1, null, false);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.JPEG, mQuality, stream);

                byte[] byteArray = stream.toByteArray();

                mPhoto.setRotationForDisplay(0);
                mPhoto.setData(byteArray);
                mPhoto.updateBitmapPreview();
                mPhoto.updateExif();

                originalBitmap.recycle();
                cropped.recycle();
                rotatedBitmap.recycle();

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                mPhoto.setData(originalBytes);
            }
        }
    }
}
