package net.gini.android.capture.internal.network;

import static com.google.common.truth.Truth.assertThat;

import static net.gini.android.capture.internal.network.GiniCaptureNetworkServiceStub.DEFAULT_DOCUMENT_ID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import net.gini.android.capture.Document;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureDocumentHelper;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.internal.cache.DocumentDataMemoryCache;
import net.gini.android.capture.network.AnalysisResult;
import net.gini.android.capture.network.Error;
import net.gini.android.capture.network.GiniCaptureNetworkCallback;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.Result;
import net.gini.android.capture.util.CancellationToken;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * Created by Alpar Szotyori on 16.04.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

@RunWith(AndroidJUnit4.class)
public class NetworkRequestsManagerTest {

    private Context mContext;
    private GiniCaptureNetworkService mGiniCaptureNetworkService;
    private DocumentDataMemoryCache mDocumentDataMemoryCache;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mGiniCaptureNetworkService = spy(new GiniCaptureNetworkServiceStub());
        mDocumentDataMemoryCache = new DocumentDataMemoryCache();
    }

    @Test
    public void should_uploadDocument() throws Exception {
        // Given
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(mGiniCaptureNetworkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        // When
        final NetworkRequestResult<GiniCaptureDocument> requestResult =
                networkRequestsManager.upload(mContext, document).get();
        // Then
        assertThat(requestResult.getApiDocumentId()).isEqualTo(DEFAULT_DOCUMENT_ID);
        assertThat(requestResult.getGiniCaptureDocument()).isEqualTo(document);
    }

    @Test
    public void should_throwException_forFailedDocumentUpload() throws Exception {
        // Given
        final String errorMessage = "Something went wrong.";
        final GiniCaptureNetworkService networkService = new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken upload(@NonNull final Document document,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                callback.failure(new Error(errorMessage));
                return new CallbackCancellationToken(callback);
            }
        };
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        // When
        ExecutionException exception = null;
        try {
            networkRequestsManager.upload(mContext, document).get();
        } catch (final ExecutionException e) {
            exception = e;
        }
        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getCause().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void should_allowUploadingSameDocument_afterFailure() throws Exception {
        // Given
        final GiniCaptureNetworkService networkService = new GiniCaptureNetworkServiceStub() {
            int counter = 0;

            @Override
            public CancellationToken upload(@NonNull final Document document,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                counter++;
                if (counter == 1) {
                    callback.failure(new Error("Something went wrong."));
                } else {
                    super.upload(document, callback);
                }
                return new CallbackCancellationToken(callback);
            }
        };
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        // When
        ExecutionException firstException = null;
        try {
            networkRequestsManager.upload(mContext, document).get();
        } catch (final ExecutionException e) {
            firstException = e;
        }
        final NetworkRequestResult<GiniCaptureDocument> secondRequestResult =
                networkRequestsManager.upload(mContext, document).get();
        // Then
        assertThat(firstException).isNotNull();
        assertThat(secondRequestResult).isNotNull();
    }

    @Test
    public void should_uploadSameDocument_onlyOnce() throws Exception {
        // Given
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(mGiniCaptureNetworkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        // When
        final NetworkRequestResult<GiniCaptureDocument> firstRequestResult =
                networkRequestsManager.upload(mContext, document).get();
        final NetworkRequestResult<GiniCaptureDocument> secondRequestResult =
                networkRequestsManager.upload(mContext, document).get();
        // Then
        assertThat(firstRequestResult).isEqualTo(secondRequestResult);
        Mockito.verify(mGiniCaptureNetworkService)
                .upload(eq(document), any(GiniCaptureNetworkCallback.class));
    }

    @Test
    public void should_deleteDocument() throws Exception {
        // Given
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(mGiniCaptureNetworkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        networkRequestsManager.upload(mContext, document);
        // When
        final NetworkRequestResult<GiniCaptureDocument> requestResult =
                networkRequestsManager.delete(document).get();
        // Then
        assertThat(requestResult.getApiDocumentId()).isEqualTo(DEFAULT_DOCUMENT_ID);
        assertThat(requestResult.getGiniCaptureDocument()).isEqualTo(document);
    }

    @Test
    public void should_throwException_forFailedDocumentDeletion() throws Exception {
        // Given
        final String errorMessage = "Something went wrong.";
        final GiniCaptureNetworkService networkService = new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken delete(@NonNull final String giniApiDocumentId,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                callback.failure(new Error(errorMessage));
                return new CallbackCancellationToken(callback);
            }
        };
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        networkRequestsManager.upload(mContext, document);
        // When
        ExecutionException exception = null;
        try {
            networkRequestsManager.delete(document).get();
        } catch (final ExecutionException e) {
            exception = e;
        }
        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getCause().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void should_allowDeletingSameDocument_afterFailure() throws Exception {
        // Given
        final GiniCaptureNetworkService networkService = new GiniCaptureNetworkServiceStub() {
            int counter = 0;

            @Override
            public CancellationToken delete(@NonNull final String giniApiDocumentId,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                counter++;
                if (counter == 1) {
                    callback.failure(new Error("Something went wrong."));
                } else {
                    super.delete(giniApiDocumentId, callback);
                }
                return new CallbackCancellationToken(callback);
            }
        };
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        networkRequestsManager.upload(mContext, document);
        // When
        ExecutionException firstException = null;
        try {
            networkRequestsManager.delete(document).get();
        } catch (final ExecutionException e) {
            firstException = e;
        }
        final NetworkRequestResult<GiniCaptureDocument> secondRequestResult =
                networkRequestsManager.delete(document).get();
        // Then
        assertThat(firstException).isNotNull();
        assertThat(secondRequestResult).isNotNull();
    }

    @Test(expected = CancellationException.class)
    public void should_completeSecondDocumentDeletion_withCancellation_forSameDocument() throws Exception {
        // Given
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(mGiniCaptureNetworkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        networkRequestsManager.upload(mContext, document);
        // When
        networkRequestsManager.delete(document).get();
        networkRequestsManager.delete(document).get();
    }

    @Test
    public void should_waitForDocumentRequests_toComplete_beforeDeletingDocument()
            throws Exception {
        // Given
        final Queue<Runnable> uploadCompletionRunnables = new ConcurrentLinkedQueue<>();
        // Simulate upload delays and queue completion to be executed on the main thread
        final GiniCaptureNetworkService networkService = spy(new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken upload(@NonNull final Document document,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                final Thread delayThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep((long) (200 + Math.random() * 300));
                            uploadCompletionRunnables.add(new Runnable() {
                                @Override
                                public void run() {
                                    callback.success(new Result(DEFAULT_DOCUMENT_ID));
                                }
                            });
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                delayThread.start();
                return new CallbackCancellationToken(callback);
            }
        });
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        networkRequestsManager.upload(mContext, document);
        // When
        // Wait for completion on a secondary thread
        final AtomicReference<NetworkRequestResult<GiniCaptureDocument>> requestResult =
                new AtomicReference<>();
        final Thread waitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requestResult.set(networkRequestsManager.delete(document).get());
                } catch (final InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        waitThread.start();
        // Run the upload completion runnables on the main thread
        while (waitThread.isAlive()) {
            final Runnable runnable = uploadCompletionRunnables.poll();
            if (runnable != null) {
                runnable.run();
            }
        }
        // Then
        assertThat(requestResult.get()).isNotNull();
        assertThat(requestResult.get().getApiDocumentId()).isEqualTo(DEFAULT_DOCUMENT_ID);
        assertThat(requestResult.get().getGiniCaptureDocument()).isEqualTo(document);
        verify(networkService).upload(any(Document.class),
                any(GiniCaptureNetworkCallback.class));
        verify(networkService).delete(eq(DEFAULT_DOCUMENT_ID), any(GiniCaptureNetworkCallback.class));
    }

    @Test(expected = CancellationException.class)
    public void should_completeDocumentDeletionWithCancellation_whenApiDocumentId_isNotAvailable()
            throws Exception {
        // Given
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(mGiniCaptureNetworkService, mDocumentDataMemoryCache);
        final ImageMultiPageDocument multiPageDocument =
                GiniCaptureDocumentHelper.newMultiPageDocument();
        // When
        final NetworkRequestResult<GiniCaptureDocument> requestResult =
                networkRequestsManager.delete(multiPageDocument).get();
    }

    @Test
    public void should_analyzeMultiPageDocument() throws Exception {
        // Given
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(mGiniCaptureNetworkService, mDocumentDataMemoryCache);
        final ImageMultiPageDocument multiPageDocument =
                GiniCaptureDocumentHelper.newMultiPageDocument();
        for (final ImageDocument imageDocument : multiPageDocument.getDocuments()) {
            networkRequestsManager.upload(mContext, imageDocument);
        }
        // When
        final AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>
                requestResult = networkRequestsManager.analyze(
                multiPageDocument).get();
        // Then
        assertThat(requestResult.getApiDocumentId()).isEqualTo(DEFAULT_DOCUMENT_ID);
        assertThat(requestResult.getGiniCaptureDocument()).isEqualTo(multiPageDocument);
    }

    @Test
    public void should_analyzeSameDocument_onlyOnce() throws Exception {
        // Given
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(mGiniCaptureNetworkService, mDocumentDataMemoryCache);
        final ImageMultiPageDocument multiPageDocument =
                GiniCaptureDocumentHelper.newMultiPageDocument();
        for (final ImageDocument imageDocument : multiPageDocument.getDocuments()) {
            networkRequestsManager.upload(mContext, imageDocument);
        }
        // When
        final NetworkRequestResult<GiniCaptureMultiPageDocument> firstRequestResult =
                networkRequestsManager.analyze(multiPageDocument).get();
        final NetworkRequestResult<GiniCaptureMultiPageDocument> secondRequestResult =
                networkRequestsManager.analyze(multiPageDocument).get();
        // Then
        assertThat(firstRequestResult).isEqualTo(secondRequestResult);
        verify(mGiniCaptureNetworkService)
                .analyze(any(LinkedHashMap.class),
                        any(GiniCaptureNetworkCallback.class));
    }

    @Test
    public void should_waitForPageDocumentUploads_toComplete_beforeAnalyzingMultiPageDocument()
            throws Exception {
        // Given
        final Queue<Runnable> uploadCompletionRunnables = new ConcurrentLinkedQueue<>();
        // Simulate upload delays and queue completion to be executed on the main thread
        final GiniCaptureNetworkService networkService = spy(new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken upload(@NonNull final Document document,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                final Thread delayThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep((long) (200 + Math.random() * 300));
                            uploadCompletionRunnables.add(new Runnable() {
                                @Override
                                public void run() {
                                    callback.success(new Result(DEFAULT_DOCUMENT_ID));
                                }
                            });
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                delayThread.start();
                return new CallbackCancellationToken(callback);
            }
        });
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final ImageMultiPageDocument multiPageDocument =
                GiniCaptureDocumentHelper.newMultiPageDocument();
        // Upload the page documents
        for (final ImageDocument imageDocument : multiPageDocument.getDocuments()) {
            networkRequestsManager.upload(mContext, imageDocument);
        }
        // When
        // Wait for completion on a secondary thread
        final AtomicReference<NetworkRequestResult<GiniCaptureMultiPageDocument>> requestResult =
                new AtomicReference<>();
        final Thread waitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requestResult.set(networkRequestsManager.analyze(multiPageDocument).get());
                } catch (final InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        waitThread.start();
        // Run the upload completion runnables on the main thread
        while (waitThread.isAlive()) {
            final Runnable runnable = uploadCompletionRunnables.poll();
            if (runnable != null) {
                runnable.run();
            }
        }
        // Then
        assertThat(requestResult.get()).isNotNull();
        assertThat(requestResult.get().getApiDocumentId()).isEqualTo(DEFAULT_DOCUMENT_ID);
        assertThat(requestResult.get().getGiniCaptureDocument()).isEqualTo(multiPageDocument);
        verify(networkService, times(3)).upload(any(Document.class),
                any(GiniCaptureNetworkCallback.class));
    }


    @Test
    public void should_cancelDocumentRequests() throws Exception {
        // Given
        final AtomicReference<CancellationToken> cancellationToken =
                new AtomicReference<>();
        final GiniCaptureNetworkService networkService = spy(new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken upload(@NonNull final Document document,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                final CancellationToken token = spy(new CallbackCancellationToken(callback));
                cancellationToken.set(token);
                return token;
            }
        });
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final ImageDocument document = GiniCaptureDocumentHelper.newImageDocument();
        // When
        networkRequestsManager.upload(mContext, document);
        networkRequestsManager.cancel(document);
        // Then
        assertThat(cancellationToken.get()).isNotNull();
        verify(cancellationToken.get()).cancel();
    }

    @Test
    public void should_allowUploadingSameDocument_afterCancellation() throws Exception {
        // Given
        final GiniCaptureNetworkService networkService = spy(new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken upload(@NonNull final Document document,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                return new CallbackCancellationToken(callback);
            }
        });
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        // When
        networkRequestsManager.upload(mContext, document);
        networkRequestsManager.cancel(document);
        networkRequestsManager.upload(mContext, document);
        // Then
        verify(networkService, times(2)).upload(eq(document),
                any(GiniCaptureNetworkCallback.class));
    }

    @Test
    public void should_allowDeletingSameDocument_afterCancelation() throws Exception {
        // Given
        final GiniCaptureNetworkService networkService = spy(new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken delete(@NonNull final String giniApiDocumentId,
                    @NonNull final GiniCaptureNetworkCallback<Result, Error> callback) {
                return new CancellationToken() {
                    @Override
                    public void cancel() {
                        callback.cancelled();
                    }
                };
            }
        });
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final GiniCaptureDocument document = GiniCaptureDocumentHelper.newImageDocument();
        // When
        networkRequestsManager.upload(mContext, document);
        networkRequestsManager.delete(document);
        networkRequestsManager.cancel(document);
        networkRequestsManager.delete(document);
        // Then
        verify(networkService, times(2)).delete(any(String.class),
                any(GiniCaptureNetworkCallback.class));
    }

    @Test
    public void should_allowAnalyzingSameDocument_afterCancelation() throws Exception {
        // Given
        final GiniCaptureNetworkService networkService = spy(new GiniCaptureNetworkServiceStub() {
            @Override
            public CancellationToken analyze(@NonNull final LinkedHashMap<String, Integer> giniApiDocumentIdRotationMap,
                    @NonNull final GiniCaptureNetworkCallback<AnalysisResult, Error> callback) {
                // Cancellation
                return new CallbackCancellationToken(callback);
            }
        });
        final NetworkRequestsManager networkRequestsManager =
                new NetworkRequestsManager(networkService, mDocumentDataMemoryCache);
        final ImageMultiPageDocument multiPageDocument =
                GiniCaptureDocumentHelper.newMultiPageDocument();
        for (final ImageDocument imageDocument : multiPageDocument.getDocuments()) {
            networkRequestsManager.upload(mContext, imageDocument);
        }
        // When
        networkRequestsManager.analyze(multiPageDocument);
        networkRequestsManager.cancel(multiPageDocument);
        networkRequestsManager.analyze(multiPageDocument);
        // Then
        verify(networkService, times(2)).analyze(any(LinkedHashMap.class),
                any(GiniCaptureNetworkCallback.class));
    }
}