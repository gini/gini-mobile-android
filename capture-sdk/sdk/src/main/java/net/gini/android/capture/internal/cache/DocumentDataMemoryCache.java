package net.gini.android.capture.internal.cache;

import android.content.Context;
import android.util.LruCache;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.document.GiniCaptureDocument;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 16.03.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public class DocumentDataMemoryCache extends MemoryCache<GiniCaptureDocument, byte[]> {

    private static final int RUNNING_WORKERS_LIMIT = 3;

    public DocumentDataMemoryCache() {
        super(RUNNING_WORKERS_LIMIT);
    }

    @Override
    protected LruCache<GiniCaptureDocument, byte[]> createCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        return new LruCache<GiniCaptureDocument, byte[]>(cacheSize) {
            @Override
            protected void entryRemoved(final boolean evicted, final GiniCaptureDocument key,
                    final byte[] oldValue,
                    final byte[] newValue) {
                if (newValue == null) {
                    key.unloadData();
                }
            }

            @Override
            protected int sizeOf(final GiniCaptureDocument key, final byte[] value) {
                return value.length / 1024;
            }
        };
    }

    @Override
    protected MemoryCache.Worker<GiniCaptureDocument, byte[]> createWorker(
            @NonNull final List<MemoryCache.Worker<GiniCaptureDocument, byte[]>> runningWorkers,
            @NonNull final GiniCaptureDocument subject,
            @NonNull final AsyncCallback<byte[], Exception> callback) {
        return new DocumentDataWorker(runningWorkers, subject,
                new AsyncCallback<byte[], Exception>() {
                    @Override
                    public void onSuccess(final byte[] result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(final Exception exception) {
                        callback.onError(exception);
                    }

                    @Override
                    public void onCancelled() {
                        callback.onCancelled();
                    }
                });
    }

    private static class DocumentDataWorker extends MemoryCache.Worker<GiniCaptureDocument, byte[]> {

        private DocumentDataWorker(
                @NonNull final List<MemoryCache.Worker<GiniCaptureDocument, byte[]>> runningWorkers,
                @NonNull final GiniCaptureDocument subject,
                @NonNull final AsyncCallback<byte[], Exception> callback) {
            super(runningWorkers, subject, callback);
        }

        @Override
        protected void doExecute(@NonNull final Context context,
                @NonNull final GiniCaptureDocument subject,
                @NonNull final AsyncCallback<byte[], Exception> callback) {
            subject.loadData(context, new AsyncCallback<byte[], Exception>() {
                @Override
                public void onSuccess(final byte[] result) {
                    callback.onSuccess(result);
                }

                @Override
                public void onError(final Exception exception) {
                    callback.onError(exception);
                }

                @Override
                public void onCancelled() {
                    callback.onCancelled();
                }
            });
        }
    }

}
