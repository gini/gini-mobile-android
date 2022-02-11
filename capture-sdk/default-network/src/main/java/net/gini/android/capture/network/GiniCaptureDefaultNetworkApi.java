package net.gini.android.capture.network;

import static net.gini.android.capture.network.logging.UtilKt.errorLogFromException;
import static net.gini.android.capture.network.logging.UtilKt.getResponseDetails;

import androidx.annotation.NonNull;

import com.android.volley.VolleyError;

import net.gini.android.bank.api.BankApiDocumentTaskManager;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.internal.camera.api.UIExecutor;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.network.model.CompoundExtractionsMapper;
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.network.model.SpecificExtractionMapper;
import net.gini.android.core.api.models.Document;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Alpar Szotyori on 22.02.2018.
 * <p>
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Default implementation of network calls which can be performed manually from outside the Gini
 * Capture SDK (e.g. for sending feedback).
 *
 * <p> To create an instance use the {@link GiniCaptureDefaultNetworkApi.Builder} returned by the
 * {@link #builder()} method.
 *
 * <p> In order to easily access this implementation pass an instance of it to {@link
 * GiniCapture.Builder#setGiniCaptureNetworkApi(GiniCaptureNetworkApi)} when creating a {@link
 * GiniCapture} instance. You can then get the instance in your app with {@link
 * GiniCapture#getGiniCaptureNetworkApi()}.
 */
public class GiniCaptureDefaultNetworkApi implements GiniCaptureNetworkApi {

    private static final Logger LOG = LoggerFactory.getLogger(GiniCaptureDefaultNetworkApi.class);

    private final GiniCaptureDefaultNetworkService mDefaultNetworkService;
    private final UIExecutor mUIExecutor = new UIExecutor();

    private Map<String, GiniCaptureCompoundExtraction> mUpdatedCompoundExtractions = Collections.emptyMap();

    /**
     * Creates a new {@link GiniCaptureDefaultNetworkApi.Builder} to configure and create a new instance.
     *
     * @return a new {@link GiniCaptureDefaultNetworkApi.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    GiniCaptureDefaultNetworkApi(
            @NonNull final GiniCaptureDefaultNetworkService defaultNetworkService) {
        mDefaultNetworkService = defaultNetworkService;
    }

    @Override
    public void sendFeedback(@NonNull final Map<String, GiniCaptureSpecificExtraction> extractions,
                             @NonNull final GiniCaptureNetworkCallback<Void, Error> callback) {
        final BankApiDocumentTaskManager documentTaskManager = mDefaultNetworkService.getGiniApi()
                .getDocumentTaskManager();
        final net.gini.android.core.api.models.Document document =
                mDefaultNetworkService.getAnalyzedGiniApiDocument();
        // We require the Gini Bank API lib's net.gini.android.core.api.models.Document for sending the feedback
        if (document != null) {
            LOG.debug("Send feedback for api document {} using extractions {}", document.getId(),
                    extractions);
            try {
                final Task<Document> feedbackTask;
                if (mUpdatedCompoundExtractions.isEmpty()) {
                    feedbackTask = documentTaskManager.sendFeedbackForExtractions(document,
                            SpecificExtractionMapper.mapToApiSdk(extractions));
                } else {
                    feedbackTask = documentTaskManager.sendFeedbackForExtractions(document,
                            SpecificExtractionMapper.mapToApiSdk(extractions),
                            CompoundExtractionsMapper.mapToApiSdk(mUpdatedCompoundExtractions));
                }
                feedbackTask.continueWith(new Continuation<net.gini.android.core.api.models.Document, Object>() {
                    @Override
                    public Object then(
                            @NonNull final Task<net.gini.android.core.api.models.Document> task) {
                        mUIExecutor.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (task.isFaulted()) {
                                    LOG.error(
                                            "Send feedback failed for api document {}",
                                            document.getId(), task.getError());
                                    String message = "unknown";
                                    if (task.getError() != null) {
                                        message = task.getError().getMessage();
                                        if (task.getError() instanceof VolleyError) {
                                            final VolleyError volleyError = (VolleyError) task.getError();
                                            message = getResponseDetails(volleyError);
                                            mDefaultNetworkService.handleErrorLog(
                                                    errorLogFromException("Failed to send feedback for document " +
                                                            document.getId(), task.getError()));
                                        }
                                    } else {
                                        mDefaultNetworkService.handleErrorLog(new ErrorLog(
                                                "Failed to send feedback for document " + document.getId(), null));
                                    }
                                    callback.failure(new Error(message));
                                } else {
                                    LOG.debug("Send feedback success for api document {}",
                                            document.getId());
                                    callback.success(null);
                                }
                            }
                        });
                        return null;
                    }
                });
            } catch (final JSONException e) {
                LOG.error("Send feedback failed for api document {}: {}", document.getId(), e);
                mDefaultNetworkService.handleErrorLog(new ErrorLog(
                        "Failed to send feedback for document " + document.getId(), e));
                callback.failure(new Error(e.getMessage()));
            }
        } else {
            LOG.error("Send feedback failed: no api document available");
            mDefaultNetworkService.handleErrorLog(new ErrorLog(
                    "Failed to send feedback: no api document available", null));
            callback.failure(new Error("Feedback not set: no api document available"));
        }
    }

    @Override
    public void deleteGiniUserCredentials() {
        mDefaultNetworkService.getGiniApi().getCredentialsStore().deleteUserCredentials();
    }

    @Override
    public void setUpdatedCompoundExtractions(@NonNull final Map<String, GiniCaptureCompoundExtraction> compoundExtractions) {
        mUpdatedCompoundExtractions = compoundExtractions;
    }

    /**
     * Builder for configuring a new instance of the {@link GiniCaptureDefaultNetworkApi}.
     */
    public static class Builder {

        private GiniCaptureDefaultNetworkService mDefaultNetworkService;

        Builder() {
        }

        /**
         * Set the same {@link GiniCaptureDefaultNetworkService} instance you use for {@link
         * GiniCapture}.
         *
         * @param networkService {@link GiniCaptureDefaultNetworkService} instance
         * @return the {@link Builder} instance
         */
        public Builder withGiniCaptureDefaultNetworkService(
                @NonNull final GiniCaptureDefaultNetworkService networkService) {
            mDefaultNetworkService = networkService;
            return this;
        }

        /**
         * Create a new instance of the {@link GiniCaptureDefaultNetworkApi}.
         *
         * @return new {@link GiniCaptureDefaultNetworkApi} instance
         */
        public GiniCaptureDefaultNetworkApi build() {
            if (mDefaultNetworkService == null) {
                throw new IllegalStateException(
                        "GiniCaptureDefaultNetworkApi requires a GiniCaptureDefaultNetworkService instance.");
            }
            return new GiniCaptureDefaultNetworkApi(mDefaultNetworkService);
        }
    }
}
