package net.gini.android.capture.internal.camera.photo;

import android.graphics.Bitmap;
import android.os.Parcelable;

import net.gini.android.capture.Document;
import net.gini.android.capture.document.ImageDocument;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Internal use only.
 *
 * @suppress
 */
public interface Photo extends Parcelable {

    boolean isImported();

    byte[] getData();

    void setData(byte[] data);

    int getRotationForDisplay();

    int getRotationDelta();

    void setRotationForDisplay(int rotationDegrees);

    String getDeviceOrientation();

    String getDeviceType();

    Document.Source getSource();

    Document.ImportMethod getImportMethod();

    Bitmap getBitmapPreview();

    ImageDocument.ImageFormat getImageFormat();

    /**
     * Corrects the format after the image data was re-encoded.
     *
     * <p> Used by {@link PhotoCompressionModifier}, which always writes JPEG:
     * an imported HEIC arrives here as {@link ImageDocument.ImageFormat#HEIC}
     * and has to report JPEG once its bytes are JPEG, so that the document
     * built from it is uploaded with the right content type.
     */
    void setImageFormat(ImageDocument.ImageFormat imageFormat);

    PhotoEdit edit();

    void updateBitmapPreview();

    void updateExif();

    void updateRotationDeltaBy(int i);

    void saveToFile(File file);

    void setParcelableMemoryCacheTag(@NonNull final String tag);
    
    @Nullable
    String getParcelableMemoryCacheTag();
}
