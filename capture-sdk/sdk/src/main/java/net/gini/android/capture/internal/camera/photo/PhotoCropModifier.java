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

                final float cropScalePercent = 0.2f;

                // Scale the crop rect
                final int scaledCropX = (int) (mCropRect.left - (mCropRect.width() * cropScalePercent));
                final int scaledCropY = (int) (mCropRect.top - (mCropRect.height() * cropScalePercent));
                final int scaledCropWidth = (int) (mCropRect.width() * (1 + cropScalePercent * 2));
                final int scaledCropHeight = (int) (mCropRect.height() * (1 + cropScalePercent * 2));

                // Transfer the crop rect into the photo's coordinate space
                int photoCropX = rotatedPhotoBitmap.getWidth() * scaledCropX / mCameraPreviewSize.getWidth();
                int photoCropY = rotatedPhotoBitmap.getHeight() * scaledCropY / mCameraPreviewSize.getHeight();
                int photoCropWidth = rotatedPhotoBitmap.getWidth() * scaledCropWidth / mCameraPreviewSize.getWidth();
                int photoCropHeight = rotatedPhotoBitmap.getHeight() * scaledCropHeight / mCameraPreviewSize.getHeight();

                final Bitmap croppedBitmap;
                if (scaledCropX < 0 || scaledCropY < 0) {
                    // The scaled crop rect is larger than the photo
                    // We will draw the photo into a canvas
                    croppedBitmap = Bitmap.createBitmap(photoCropWidth, photoCropHeight, Bitmap.Config.ARGB_8888);

                    final Canvas canvas = new Canvas();
                    canvas.setBitmap(croppedBitmap);

                    final Paint paint = new Paint();

                    canvas.drawBitmap(rotatedPhotoBitmap, Math.abs(photoCropX), Math.abs(photoCropY), paint);
                } else {
                    // The scaled crop rect is smaller than the photo
                    // We will crop the rect from the photo
                    croppedBitmap = Bitmap.createBitmap(rotatedPhotoBitmap, photoCropX, photoCropY,
                            photoCropWidth, photoCropHeight, null, false);
                }

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
