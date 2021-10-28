package net.gini.android.capture.analysis;

import static net.gini.android.capture.internal.network.NetworkRequestsManager.isCancellation;

import android.app.Application;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureDebug;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureDocumentError;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.internal.network.AnalysisNetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureReturnReason;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Created by Alpar Szotyori on 09.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public class AnalysisInteractor {

    private final Application mApp;

    public AnalysisInteractor(@NonNull final Application app) {
        mApp = app;
    }

    public CompletableFuture<ResultHolder> analyzeMultiPageDocument(
            final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                    multiPageDocument) {
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager = GiniCapture.getInstance()
                    .internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                GiniCaptureDebug.writeDocumentToFile(mApp, multiPageDocument, "_for_analysis");
                for (final Object document : multiPageDocument.getDocuments()) {
                    final GiniCaptureDocument giniCaptureDocument = (GiniCaptureDocument) document;
                    networkRequestsManager.upload(mApp, giniCaptureDocument);
                }
                return networkRequestsManager.analyze(multiPageDocument)
                        .handle(new CompletableFuture.BiFun<AnalysisNetworkRequestResult<
                                GiniCaptureMultiPageDocument>, Throwable, ResultHolder>() {
                            @Override
                            public ResultHolder apply(
                                    final AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>
                                            requestResult,
                                    final Throwable throwable) {
                                if (throwable != null && !isCancellation(throwable)) {
                                    throw new RuntimeException(throwable); // NOPMD
                                } else if (requestResult != null) {
                                    final Map<String, GiniCaptureSpecificExtraction> extractions =
                                            requestResult.getAnalysisResult().getExtractions();
                                    final Map<String, GiniCaptureCompoundExtraction> compoundExtractions =
                                            requestResult.getAnalysisResult().getCompoundExtractions();
                                    if (extractions.isEmpty() && compoundExtractions.isEmpty()) {
                                        return new ResultHolder(Result.SUCCESS_NO_EXTRACTIONS);
                                    } else {
                                        return new ResultHolder(Result.SUCCESS_WITH_EXTRACTIONS,
                                                extractions,
                                                compoundExtractions,
                                                requestResult.getAnalysisResult().getReturnReasons());
                                    }
                                }
                                return null;
                            }
                        });
            } else {
                return CompletableFuture.completedFuture(
                        new ResultHolder(Result.NO_NETWORK_SERVICE));
            }
        } else {
            return CompletableFuture.completedFuture(new ResultHolder(Result.NO_NETWORK_SERVICE));
        }
    }

    public CompletableFuture<Void> deleteMultiPageDocument(
            final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                    multiPageDocument) {
        return deleteDocument(multiPageDocument)
                .handle(new CompletableFuture.BiFun<NetworkRequestResult<GiniCaptureDocument>,
                        Throwable, Void>() {
                    @Override
                    public Void apply(
                            final NetworkRequestResult<GiniCaptureDocument>
                                    giniCaptureDocumentNetworkRequestResult,
                            final Throwable throwable) {
                        return null;
                    }
                })
                .thenCompose(
                        new CompletableFuture.Fun<Void, CompletableFuture<Void>>() {
                            @Override
                            public CompletableFuture<Void> apply(
                                    final Void result) {
                                final NetworkRequestsManager networkRequestsManager =
                                        GiniCapture.getInstance()
                                                .internal().getNetworkRequestsManager();
                                if (networkRequestsManager == null) {
                                    return CompletableFuture.completedFuture(null);
                                }
                                final List<CompletableFuture<NetworkRequestResult<
                                        GiniCaptureDocument>>> futures = new ArrayList<>();
                                for (final GiniCaptureDocument document
                                        : multiPageDocument.getDocuments()) {
                                    networkRequestsManager.cancel(document);
                                    futures.add(networkRequestsManager.delete(document));
                                }
                                return CompletableFuture.allOf(
                                        futures.toArray(new CompletableFuture[0]));
                            }
                        });
    }

    public CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> deleteDocument(
            final GiniCaptureDocument document) {
        if (GiniCapture.hasInstance()) {
            final NetworkRequestsManager networkRequestsManager = GiniCapture.getInstance()
                    .internal().getNetworkRequestsManager();
            if (networkRequestsManager != null) {
                networkRequestsManager.cancel(document);
                return networkRequestsManager.delete(document);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public enum Result {
        SUCCESS_NO_EXTRACTIONS,
        SUCCESS_WITH_EXTRACTIONS,
        NO_NETWORK_SERVICE
    }

    /**
     * Internal use only.
     *
     * @suppress
     */
    public static final class ResultHolder {

        private final Result mResult;
        private final Map<String, GiniCaptureSpecificExtraction> mExtractions;
        private final Map<String, GiniCaptureCompoundExtraction> mCompoundExtractions;
        private final List<GiniCaptureReturnReason> mReturnReasons;

        ResultHolder(@NonNull final Result result) {
            this(result, Collections.<String, GiniCaptureSpecificExtraction>emptyMap(),
                    Collections.<String, GiniCaptureCompoundExtraction>emptyMap(),
                    Collections.<GiniCaptureReturnReason>emptyList());
        }

        ResultHolder(
                @NonNull final Result result,
                @NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                @NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions,
                @NonNull final List<GiniCaptureReturnReason> returnReasons) {
            mResult = result;
            mExtractions = extractions;
            mCompoundExtractions = compoundExtractions;
            mReturnReasons = returnReasons;
        }

        @NonNull
        public Result getResult() {
            return mResult;
        }

        @NonNull
        public Map<String, GiniCaptureSpecificExtraction> getExtractions() {
            return mExtractions;
        }

        @NonNull
        public Map<String, GiniCaptureCompoundExtraction> getCompoundExtractions() {
            return mCompoundExtractions;
        }

        @NonNull
        public List<GiniCaptureReturnReason> getReturnReasons() {
            return mReturnReasons;
        }
    }
}
