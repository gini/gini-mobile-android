package net.gini.android.capture.internal.camera.photo;

import net.gini.android.capture.Document;
import net.gini.android.capture.document.ImageDocument;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * @suppress
 */
public final class PhotoFactory {

    public static Photo newPhotoFromJpeg(final byte[] bytes,
            final int orientation,
            @NonNull final String deviceOrientation,
            @NonNull final String deviceType,
            @NonNull final Document.Source source) {
        return new MutablePhoto(bytes, orientation, deviceOrientation, deviceType, source,
                Document.ImportMethod.NONE, ImageDocument.ImageFormat.JPEG, false);
    }

    public static Photo newPhotoFromDocument(final ImageDocument document) {
        // HEIC joins JPEG here so that it gets a real PhotoEdit: the
        // compression step re-encodes it to the JPEG the Gini API expects.
        // PNG and GIF keep the no-op edit and are uploaded as they are.
        if (document.getFormat() == ImageDocument.ImageFormat.JPEG
                || document.getFormat() == ImageDocument.ImageFormat.HEIC) {
            return new MutablePhoto(document);
        }
        return new ImmutablePhoto(document);
    }

    private PhotoFactory() {
    }
}
