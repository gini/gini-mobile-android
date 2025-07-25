package net.gini.android.capture.internal.network;

/**
 * Created by Alpar Szotyori on 13.04.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.GiniCaptureDebug;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.cache.DocumentDataMemoryCache;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.network.AnalysisResult;
import net.gini.android.capture.network.Error;
import net.gini.android.capture.network.GiniCaptureNetworkCallback;
import net.gini.android.capture.network.GiniCaptureNetworkService;
import net.gini.android.capture.network.Result;
import net.gini.android.capture.util.CancellationToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;

import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Internal use only.
 *
 * @suppress
 */
public class NetworkRequestsManager {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkRequestsManager.class);

    private final Map<String, String> mApiDocumentIds;
    private final Map<String, CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>>
            mDocumentUploadFutures;
    private final Map<String, CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>>
            mDocumentDeleteFutures;
    private final Map<String, CompletableFuture<
            AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>> mDocumentAnalyzeFutures;
    private final Map<UUID, CompletableFuture<ConfigurationNetworkResult>> mConfigurationFutures;
    private final Map<UUID, CompletableFuture<String>> mUserJourneyEventsFutures;

    private final GiniCaptureNetworkService mGiniCaptureNetworkService;
    private final DocumentDataMemoryCache mDocumentDataMemoryCache;

    public NetworkRequestsManager(@NonNull final GiniCaptureNetworkService giniCaptureNetworkService,
                                  @NonNull final DocumentDataMemoryCache documentDataMemoryCache) {
        mGiniCaptureNetworkService = giniCaptureNetworkService;
        mDocumentDataMemoryCache = documentDataMemoryCache;
        mApiDocumentIds = new HashMap<>();
        mDocumentUploadFutures = new HashMap<>();
        mDocumentDeleteFutures = new HashMap<>();
        mDocumentAnalyzeFutures = new HashMap<>();
        mConfigurationFutures = new HashMap<>();
        mUserJourneyEventsFutures = new HashMap<>();
    }


    public CompletableFuture<String> sendEvents(AmplitudeRootModel amplitudeRootModel, UUID id) {

        final CompletableFuture<String> userJourneyEventsFutures =
                mUserJourneyEventsFutures.get(id);
        if (userJourneyEventsFutures != null) {
            return userJourneyEventsFutures;
        }

        final CompletableFuture<String> future =
                new CompletableFuture<>();
        mUserJourneyEventsFutures.put(id, future);

        final CancellationToken cancellationToken =
                mGiniCaptureNetworkService.sendEvents(amplitudeRootModel,
                        new GiniCaptureNetworkCallback<Void, Error>() {
                            @Override
                            public void failure(final Error error) {
                                LOG.error("Send events failed for {}",
                                        error.getMessage());
                                ErrorType errorType = ErrorType.typeFromError(error, false);
                                future.completeExceptionally(new FailureException(errorType));
                            }

                            @Override
                            public void success(final Void result) {
                                LOG.debug("Send events success for {}:",
                                        result);
                                future.complete(null);
                            }

                            @Override
                            public void cancelled() {
                                LOG.debug("Send events cancelled for");
                                future.cancel(false);

                            }
                        });

        future.handle(
                (requestResult, throwable) -> {
                    if (throwable != null) {
                        if (isCancellation(throwable)) {
                            cancellationToken.cancel();
                        } else {
                            ErrorLogger.log(new ErrorLog("Send events failed", throwable));
                        }
                    } else if (requestResult != null) {
                        mUserJourneyEventsFutures.remove(id);
                    }
                    mUserJourneyEventsFutures.remove(id);
                    return requestResult;
                });

        return future;
    }

    public CompletableFuture<ConfigurationNetworkResult> getConfigurations(UUID id) {

        final CompletableFuture<ConfigurationNetworkResult> configurationFuture =
                mConfigurationFutures.get(id);
        if (configurationFuture != null) {
            return configurationFuture;
        }

        final CompletableFuture<ConfigurationNetworkResult> future =
                new CompletableFuture<>();
        mConfigurationFutures.put(id, future);

        final CancellationToken cancellationToken =
                mGiniCaptureNetworkService.getConfiguration(
                        new GiniCaptureNetworkCallback<Configuration, Error>() {
                            @Override
                            public void failure(final Error error) {
                                LOG.error("Get configuration failed for {}: {}",
                                        id,
                                        error.getMessage());
                                ErrorType errorType = ErrorType.typeFromError(error, false);
                                future.completeExceptionally(new FailureException(errorType));
                            }

                            @Override
                            public void success(final Configuration result) {
                                LOG.debug("Get configuration success for {}: {}",
                                        id,
                                        result);
                                future.complete(new ConfigurationNetworkResult(result,
                                        result.getId()));
                            }

                            @Override
                            public void cancelled() {
                                LOG.debug("Get configuration cancelled for {}",
                                        id);
                                future.cancel(false);

                            }
                        });

        future.handle(
                (requestResult, throwable) -> {
                    if (throwable != null) {
                        if (isCancellation(throwable)) {
                            cancellationToken.cancel();
                        } else {
                            ErrorLogger.log(new ErrorLog("Getting configuration failed", throwable));
                        }
                    } else if (requestResult != null) {
                        mConfigurationFutures.remove(id);
                    }
                    mConfigurationFutures.remove(id);
                    return requestResult;
                });

        return future;
    }


    public CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> upload(
            @NonNull final Context context,
            @NonNull final GiniCaptureDocument document) {
        LOG.debug("Upload document {}", document.getId());
        final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> documentUploadFuture =
                mDocumentUploadFutures.get(document.getId());
        if (documentUploadFuture != null) {
            LOG.debug("Document upload already requested for {}", document.getId());
            return documentUploadFuture;
        }

        final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> future =
                new CompletableFuture<>();
        mDocumentUploadFutures.put(document.getId(), future);

        LOG.debug("Load document data for {}", document.getId());
        mDocumentDataMemoryCache.get(context, document, new AsyncCallback<byte[], Exception>() {
            @Override
            public void onSuccess(final byte[] result) {
                LOG.debug("Document data loaded for {}", document.getId());
                GiniCaptureDebug.writeDocumentToFile(context, document, "-upload");
                final CancellationToken cancellationToken =
                        mGiniCaptureNetworkService.upload(document,
                                new GiniCaptureNetworkCallback<Result, Error>() {
                                    @Override
                                    public void failure(final Error error) {
                                        LOG.error("Document upload failed for {}: {}",
                                                document.getId(),
                                                error.getMessage());
                                        ErrorType errorType = ErrorType.typeFromError(error, false);
                                        future.completeExceptionally(new FailureException(errorType));
                                    }

                                    @Override
                                    public void success(final Result result) {
                                        LOG.debug("Document upload success for {}: {}",
                                                document.getId(),
                                                result);
                                        mApiDocumentIds.put(document.getId(),
                                                result.getGiniApiDocumentId());
                                        future.complete(new NetworkRequestResult<>(document,
                                                result.getGiniApiDocumentId(),
                                                result.getGiniApiDocumentFilename()));
                                    }

                                    @Override
                                    public void cancelled() {
                                        LOG.debug("Document upload cancelled for {}",
                                                document.getId());
                                        future.cancel(false);
                                    }
                                });

                future.handle(
                        new CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>,
                                Throwable, NetworkRequestResult<GiniCaptureDocument>>() {
                            @Override
                            public NetworkRequestResult<GiniCaptureDocument> apply(
                                    final NetworkRequestResult<GiniCaptureDocument>
                                            networkRequestResult,
                                    final Throwable throwable) {
                                if (throwable != null) {
                                    if (isCancellation(throwable)) {
                                        cancellationToken.cancel();
                                    } else {
                                        ErrorLogger.log(new ErrorLog("Document upload failed", throwable));
                                    }
                                    mDocumentUploadFutures.remove(document.getId());
                                }
                                return networkRequestResult;
                            }
                        });
            }

            @Override
            public void onError(final Exception exception) {
                LOG.error("Document data loading failed for {}: {}", document.getId(),
                        exception.getMessage());
                future.completeExceptionally(exception);
            }

            @Override
            public void onCancelled() {
                // Not used
            }
        });

        return future;
    }

    public static boolean isCancellation(@NonNull final Throwable throwable) {
        return throwable instanceof CancellationException ||
                (throwable.getCause() != null && throwable.getCause() instanceof CancellationException);
    }

    public CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> delete(
            @NonNull final GiniCaptureDocument document) {
        LOG.debug("Delete document {}", document.getId());
        final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> documentDeleteFuture =
                mDocumentDeleteFutures.get(document.getId());
        if (documentDeleteFuture != null) {
            LOG.debug("Document deletion already requested for {}", document.getId());
            return documentDeleteFuture;
        }
        // Collect futures related to this document or its partial documents
        final List<CompletableFuture> documentFutures = collectRelatedFutures(document);
        return CompletableFuture
                .allOf(documentFutures.toArray(new CompletableFuture[documentFutures.size()]))
                .handle(new CompletableFuture.BiFun<Void, Throwable, Boolean>() {
                    @Override
                    public Boolean apply(final Void aVoid, final Throwable throwable) {
                        if (throwable != null) {
                            return false;
                        }
                        return true;
                    }
                })
                .thenCompose(
                        new CompletableFuture.Fun<Boolean,
                                CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>>() {
                            @Override
                            public CompletableFuture<NetworkRequestResult<
                                    GiniCaptureDocument>> apply(
                                    final Boolean relatedFuturesSucceeded) {
                                return deleteDocument(document, relatedFuturesSucceeded);
                            }
                        });
    }

    @NonNull
    private List<CompletableFuture> collectRelatedFutures(
            @NonNull final GiniCaptureDocument document) {
        final List<CompletableFuture> documentFutures = new ArrayList<>();
        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>
                analyzeFuture = mDocumentAnalyzeFutures.get(document.getId());
        if (analyzeFuture != null) {
            documentFutures.add(analyzeFuture);
        }
        if (document instanceof GiniCaptureMultiPageDocument) {
            final GiniCaptureMultiPageDocument multiPageDocument =
                    (GiniCaptureMultiPageDocument) document;
            for (final Object partialDocument : multiPageDocument.getDocuments()) {
                final GiniCaptureDocument giniCaptureDocument = (GiniCaptureDocument) partialDocument;
                final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>
                        uploadFuture = mDocumentUploadFutures.get(giniCaptureDocument.getId());
                if (uploadFuture != null) {
                    documentFutures.add(uploadFuture);
                }
            }
        } else {
            final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>
                    uploadFuture = mDocumentUploadFutures.get(document.getId());
            if (uploadFuture != null) {
                documentFutures.add(uploadFuture);
            }
        }
        return documentFutures;
    }

    @NonNull
    private CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> deleteDocument(
            @NonNull final GiniCaptureDocument document, final Boolean relatedFuturesSucceeded) {
        final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>
                documentDeleteFuture =
                mDocumentDeleteFutures.get(document.getId());
        if (documentDeleteFuture != null) {
            LOG.debug("Document deletion already requested for {}",
                    document.getId());
            return documentDeleteFuture;
        }

        final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>
                future = new CompletableFuture<>();

        final String apiDocumentId = mApiDocumentIds.get(document.getId());
        if (apiDocumentId == null) {
            LOG.debug(
                    "Document deletion cancelled for {}: no API document id found",
                    document.getId());
            future.cancel(false);
            return future;
        }
        if (!relatedFuturesSucceeded) {
            LOG.debug(
                    "Document deletion cancelled for {}: "
                            + "there was an error with a previous request",
                    document.getId());
            future.cancel(false);
            return future;
        }

        mDocumentDeleteFutures.put(document.getId(), future);

        final CancellationToken cancellationToken =
                mGiniCaptureNetworkService.delete(apiDocumentId,
                        new GiniCaptureNetworkCallback<Result, Error>() {
                            @Override
                            public void failure(final Error error) {
                                LOG.error(
                                        "Document deletion failed for {}: {}",
                                        document.getId(),
                                        error.getMessage());
                                ErrorType errorType = ErrorType.typeFromError(error, false);
                                future.completeExceptionally(new FailureException(errorType));
                            }

                            @Override
                            public void success(final Result result) {
                                LOG.debug(
                                        "Document deletion success for {}",
                                        document.getId());
                                future.complete(
                                        new NetworkRequestResult<>(document,
                                                result.getGiniApiDocumentId(),
                                                result.getGiniApiDocumentFilename()));
                            }

                            @Override
                            public void cancelled() {
                                LOG.debug(
                                        "Document deletion cancelled for {}",
                                        document.getId());
                                future.cancel(false);
                            }
                        });

        future.handle(
                new CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>,
                        Throwable, NetworkRequestResult<GiniCaptureDocument>>() {
                    @Override
                    public NetworkRequestResult<GiniCaptureDocument> apply(
                            final NetworkRequestResult<GiniCaptureDocument> requestResult,
                            final Throwable throwable) {
                        if (throwable != null) {
                            if (isCancellation(throwable)) {
                                cancellationToken.cancel();
                            } else {
                                ErrorLogger.log(new ErrorLog("Document deletion failed", throwable));
                            }
                        } else if (requestResult != null) {
                            mDocumentUploadFutures.remove(document.getId());
                            mDocumentAnalyzeFutures.remove(
                                    document.getId());
                            mApiDocumentIds.remove(document.getId());
                        }
                        mDocumentDeleteFutures.remove(document.getId());
                        return requestResult;
                    }
                });

        return future;
    }

    public CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>> analyze(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        LOG.debug("Analyze document {}", multiPageDocument.getId());
        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>
                documentAnalyzeFuture =
                mDocumentAnalyzeFutures.get(multiPageDocument.getId());
        if (documentAnalyzeFuture != null) {
            LOG.debug("Document analysis already requested for {}", multiPageDocument.getId());
            return documentAnalyzeFuture;
        }

        final List<CompletableFuture> documentFutures = collectRelatedUploadFutures(
                multiPageDocument);

        return CompletableFuture
                .allOf(documentFutures.toArray(new CompletableFuture[documentFutures.size()]))
                .thenCompose(
                        new CompletableFuture.Fun<Void, CompletableFuture<
                                AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>>() {
                            @Override
                            public CompletableFuture<AnalysisNetworkRequestResult<
                                    GiniCaptureMultiPageDocument>> apply(
                                    final Void aVoid) {
                                return analyzeDocument(multiPageDocument);
                            }
                        });
    }

    @NonNull
    private CompletableFuture<AnalysisNetworkRequestResult<
            GiniCaptureMultiPageDocument>> analyzeDocument(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>
                documentAnalyzeFuture = mDocumentAnalyzeFutures.get(multiPageDocument.getId());
        if (documentAnalyzeFuture != null) {
            LOG.debug("Document analysis already requested for {}", multiPageDocument.getId());
            return documentAnalyzeFuture;
        }

        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>> future =
                new CompletableFuture<>();

        final LinkedHashMap<String, Integer>
                giniApiDocumentIdRotationDeltas =
                new LinkedHashMap<>();
        final boolean success = collectGiniApiDocumentIds(giniApiDocumentIdRotationDeltas,
                multiPageDocument);
        if (!success) {
            future.completeExceptionally(new IllegalStateException(
                    "Missing partial document id. All page documents of a multi-page document "
                            + "have to be uploaded before analysis."));
            return future;
        }

        mDocumentAnalyzeFutures.put(multiPageDocument.getId(), future);

        final CancellationToken cancellationToken =
                mGiniCaptureNetworkService.analyze(
                        giniApiDocumentIdRotationDeltas,
                        new GiniCaptureNetworkCallback<AnalysisResult, Error>() {
                            @Override
                            public void failure(final Error error) {
                                LOG.error("Document analysis failed for {}: {}",
                                        multiPageDocument.getId(), error.getMessage());
                                ErrorType errorType = ErrorType.typeFromError(error, false);
                                future.completeExceptionally(new FailureException(errorType));
                            }

                            @Override
                            public void success(
                                    final AnalysisResult result) {
                                LOG.debug("Document analysis success for {}: {}",
                                        multiPageDocument.getId(), result);
                                mApiDocumentIds.put(multiPageDocument.getId(),
                                        result.getGiniApiDocumentId());
                                future.complete(
                                        new AnalysisNetworkRequestResult<>(
                                                multiPageDocument,
                                                result.getGiniApiDocumentId(),
                                                result.getGiniApiDocumentFilename(),
                                                result)
                                );
                            }

                            @Override
                            public void cancelled() {
                                LOG.debug("Document analysis canceleld for {}",
                                        multiPageDocument.getId());
                                future.cancel(false);
                            }
                        });

        future.handle(
                new CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureMultiPageDocument>,
                        Throwable, NetworkRequestResult<GiniCaptureMultiPageDocument>>() {
                    @Override
                    public NetworkRequestResult<GiniCaptureMultiPageDocument> apply(
                            final NetworkRequestResult<GiniCaptureMultiPageDocument>
                                    networkRequestResult,
                            final Throwable throwable) {
                        if (throwable != null) {
                            if (isCancellation(throwable)) {
                                cancellationToken.cancel();
                            } else {
                                ErrorLogger.log(new ErrorLog("Document analysis failed", throwable));
                            }
                            mDocumentAnalyzeFutures.remove(
                                    multiPageDocument.getId());
                        }
                        return networkRequestResult;
                    }
                });
        return future;
    }

    private boolean collectGiniApiDocumentIds(
            final LinkedHashMap<String, Integer> giniApiDocumentIdRotationDeltas, // NOPMD
            final GiniCaptureMultiPageDocument multiPageDocument) {
        for (final Object document : multiPageDocument.getDocuments()) {
            final GiniCaptureDocument giniCaptureDocument =
                    (GiniCaptureDocument) document;
            final String apiDocumentId = mApiDocumentIds.get(
                    giniCaptureDocument.getId());
            if (apiDocumentId != null) {
                int rotationDelta = 0;
                if (giniCaptureDocument instanceof ImageDocument) {
                    rotationDelta =
                            ((ImageDocument) giniCaptureDocument).getRotationDelta();
                }
                giniApiDocumentIdRotationDeltas.put(apiDocumentId, rotationDelta);
            } else {
                LOG.error(
                        "Document analysis failed for {}: missing partial document id for {}",
                        multiPageDocument.getId(),
                        ((GiniCaptureDocument) document).getId());
                return false;
            }
        }
        return true;
    }

    @NonNull
    private List<CompletableFuture> collectRelatedUploadFutures(
            @NonNull final GiniCaptureMultiPageDocument multiPageDocument) {
        final List<CompletableFuture> documentFutures = new ArrayList<>();
        for (final Object document : multiPageDocument.getDocuments()) {
            final GiniCaptureDocument giniCaptureDocument = (GiniCaptureDocument) document;
            final CompletableFuture documentFuture = mDocumentUploadFutures.get(
                    giniCaptureDocument.getId());
            if (documentFuture != null) {
                documentFutures.add(documentFuture);
            }
        }
        return documentFutures;
    }

    public void cancel(@NonNull final GiniCaptureDocument document) {
        cancelFuture(mDocumentUploadFutures.get(document.getId()));
        cancelFuture(mDocumentAnalyzeFutures.get(document.getId()));
        cancelFuture(mDocumentDeleteFutures.get(document.getId()));
    }

    private void cancelFuture(@Nullable final CompletableFuture future) {
        if (future != null) {
            future.cancel(false);
        }
    }

    public void cleanup() {
        cancelAll();
        mApiDocumentIds.clear();
        mDocumentUploadFutures.clear();
        mDocumentAnalyzeFutures.clear();
        mDocumentDeleteFutures.clear();
        mGiniCaptureNetworkService.cleanup();
    }

    public void cancelAll() {
        cancelFutures(mDocumentUploadFutures);
        cancelFutures(mDocumentAnalyzeFutures);
        cancelFutures(mDocumentDeleteFutures);
    }

    private <T extends CompletableFuture> void cancelFutures(
            @NonNull final Map<String, T> futures) {
        // Iterate with a copy of the map because the original map can be altered as the result
        // of the cancellation
        final Map<String, T> copy = new HashMap<>(futures);
        for (final T future : copy.values()) {
            cancelFuture(future);
        }
    }

}
