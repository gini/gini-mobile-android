package net.gini.android.capture.internal.document;

import android.content.Context;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.cache.PhotoMemoryCache;
import net.gini.android.capture.internal.camera.photo.Photo;
import net.gini.android.capture.internal.camera.photo.PhotoFactoryDocumentAsyncTask;
import net.gini.android.capture.internal.util.Size;

import androidx.annotation.NonNull;

/**
 * Internal use only.
 *
 * @suppress
 */
class ImageDocumentRenderer implements DocumentRenderer {

    private final ImageDocument mImageDocument;
    private Photo mPhoto;

    ImageDocumentRenderer(@NonNull final ImageDocument document) {
        mImageDocument = document;
    }

    @Override
    public void toBitmap(@NonNull final Context context, @NonNull final Size targetSize,
            @NonNull final Callback callback) {
        if (GiniCapture.hasInstance()) {
            getFromCache(context, callback);
        } else if (mPhoto == null) {
            createWithAsyncTask(callback);
        } else {
            callback.onBitmapReady(mPhoto.getBitmapPreview(),
                    mImageDocument.getRotationForDisplay());
        }
    }

    private void createWithAsyncTask(@NonNull final Callback callback) {
        final PhotoFactoryDocumentAsyncTask asyncTask = new PhotoFactoryDocumentAsyncTask(
                new AsyncCallback<Photo, Exception>() {
                    @Override
                    public void onSuccess(final Photo result) {
                        mPhoto = result;
                        callback.onBitmapReady(mPhoto.getBitmapPreview(),
                                mImageDocument.getRotationForDisplay());
                    }

                    @Override
                    public void onError(final Exception exception) {
                        callback.onBitmapReady(null, 0);
                    }

                    @Override
                    public void onCancelled() {
                        callback.onBitmapReady(null, 0);
                    }
                });
        asyncTask.execute(mImageDocument);
    }

    private void getFromCache(@NonNull final Context context,
            @NonNull final Callback callback) {
        final PhotoMemoryCache photoMemoryCache =
                GiniCapture.getInstance().internal().getPhotoMemoryCache();
        photoMemoryCache.get(context, mImageDocument, new AsyncCallback<Photo, Exception>() {
            @Override
            public void onSuccess(final Photo result) {
                callback.onBitmapReady(result.getBitmapPreview(),
                        mImageDocument.getRotationForDisplay());
            }

            @Override
            public void onError(final Exception exception) {
                callback.onBitmapReady(null, 0);
            }

            @Override
            public void onCancelled() {
                callback.onBitmapReady(null, 0);
            }
        });
    }

    @Override
    public void getPageCount(@NonNull final Context context,
            @NonNull final AsyncCallback<Integer, Exception> asyncCallback) {
        asyncCallback.onSuccess(1);
    }
}
