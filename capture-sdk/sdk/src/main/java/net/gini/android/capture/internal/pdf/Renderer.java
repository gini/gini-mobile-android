package net.gini.android.capture.internal.pdf;

import android.graphics.Bitmap;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.internal.util.Size;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * @suppress
 */
public interface Renderer {

    void toBitmap(@NonNull final Size targetSize,
            @NonNull final AsyncCallback<Bitmap, Exception> asyncCallback);

    void getPageCount(@NonNull final AsyncCallback<Integer, Exception> asyncCallback);

    int getPageCount();

    boolean isPdfPasswordProtected();
}
