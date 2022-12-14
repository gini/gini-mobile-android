package net.gini.android.capture.internal.camera.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Size;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

                Bitmap originalPhotoBitmap = BitmapFactory.decodeByteArray(mPhoto.getData(),
                        0, mPhoto.getData().length);

                Bitmap rotatedPhotoBitmap = Bitmap.createBitmap(originalPhotoBitmap, 0, 0, originalPhotoBitmap.getWidth(),
                        originalPhotoBitmap.getHeight(), matrix, false);

                final float cropScalePercent = 0.15f;

                // Scale the crop rect
                final int scaledCropX = (int) (mCropRect.left - (mCropRect.width() * cropScalePercent));
                final int scaledCropY = (int) (mCropRect.top - (mCropRect.height() * cropScalePercent));
                final int scaledCropWidth = (int) (mCropRect.width() * (1 + cropScalePercent * 2));
                final int scaledCropHeight = (int) (mCropRect.height() * (1 + cropScalePercent * 2));

                // Transfer the crop rect into the photo's coordinate space
                // and limit it to the bounds of the photo
                final int photoCropX = Math.max(0,
                        rotatedPhotoBitmap.getWidth() * scaledCropX / mCameraPreviewSize.getWidth());
                final int photoCropY = Math.max(0,
                        rotatedPhotoBitmap.getHeight() * scaledCropY / mCameraPreviewSize.getHeight());
                final int photoCropWidth = Math.min(rotatedPhotoBitmap.getWidth(),
                        rotatedPhotoBitmap.getWidth() * scaledCropWidth / mCameraPreviewSize.getWidth());
                final int photoCropHeight = Math.min(rotatedPhotoBitmap.getHeight(),
                        rotatedPhotoBitmap.getHeight() * scaledCropHeight / mCameraPreviewSize.getHeight());

                // Crop the rect from the photo
                Bitmap croppedBitmap = Bitmap.createBitmap(rotatedPhotoBitmap, photoCropX, photoCropY,
                        photoCropWidth, photoCropHeight, null, false);

                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, mQuality, stream);

                    byte[] byteArray = stream.toByteArray();

                    mPhoto.setRotationForDisplay(0);
                    mPhoto.setData(byteArray);
                    mPhoto.updateBitmapPreview();
                    mPhoto.updateExif();
                }

                originalPhotoBitmap.recycle();
                croppedBitmap.recycle();
                rotatedPhotoBitmap.recycle();

            } catch (IllegalArgumentException | IOException e) {
                e.printStackTrace();
                mPhoto.setData(originalBytes);
            }
        }
    }
}
