package net.gini.android.bank.api;

import androidx.annotation.NonNull;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.gini.android.bank.api.response.PaymentResponse;
import net.gini.android.bank.api.response.ResolvePaymentResponse;
import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.models.CompoundExtraction;
import net.gini.android.core.api.models.Document;
import net.gini.android.core.api.models.Extraction;
import net.gini.android.bank.api.models.Payment;
import net.gini.android.bank.api.models.PaymentKt;
import net.gini.android.core.api.models.PaymentRequest;
import net.gini.android.bank.api.models.ResolvePaymentInput;
import net.gini.android.bank.api.models.ResolvedPayment;
import net.gini.android.bank.api.models.ResolvedPaymentKt;
import net.gini.android.core.api.models.SpecificExtraction;
import net.gini.android.bank.api.requests.ErrorEvent;
import net.gini.android.bank.api.requests.ResolvePaymentBody;
import net.gini.android.bank.api.requests.ResolvePaymentBodyKt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Alpár Szotyori on 27.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */
public class BankApiDocumentTaskManager extends DocumentTaskManager<BankApiCommunicator> {

    public BankApiDocumentTaskManager(BankApiCommunicator apiCommunicator, SessionManager sessionManager, GiniApiType giniApiType, Moshi moshi) {
        super(apiCommunicator, sessionManager, giniApiType, moshi);
    }

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in
     * the Gini API documentation.
     *
     * @param document    The document for which the extractions should be updated.
     * @param extractions A Map where the key is the name of the specific extraction and the value is the
     *                    SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                    method of this manager.
     *
     * @return A Task which will resolve to the same document instance when storing the updated
     * extractions was successful.
     *
     * @throws JSONException When a value of an extraction is not JSON serializable.
     */
    public Task<Document> sendFeedbackForExtractions(@NonNull final Document document,
                                                     @NonNull final Map<String, SpecificExtraction> extractions)
            throws JSONException {
        final String documentId = document.getId();
        final JSONObject feedbackForExtractions = new JSONObject();
        for (Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
            final Extraction extraction = entry.getValue();
            final JSONObject extractionData = new JSONObject();
            extractionData.put("value", extraction.getValue());
            extractionData.put("entity", extraction.getEntity());
            feedbackForExtractions.put(entry.getKey(), extractionData);
        }

        return getSessionManager().getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return getApiCommunicator().sendFeedback(documentId, feedbackForExtractions, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<JSONObject, Document>() {
            @Override
            public Document then(Task<JSONObject> task) throws Exception {
                for (Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
                    entry.getValue().setIsDirty(false);
                }
                return document;
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in
     * the Gini API documentation.
     *
     * @param document            The document for which the extractions should be updated.
     * @param extractions         A Map where the key is the name of the specific extraction and the value is the
     *                            SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @param compoundExtractions A Map where the key is the name of the compound extraction and the value is the
     *                            CompoundExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @return A Task which will resolve to the same document instance when storing the updated
     * extractions was successful.
     * @throws JSONException When a value of an extraction is not JSON serializable.
     */
    public Task<Document> sendFeedbackForExtractions(@NonNull final Document document,
                                                     @NonNull final Map<String, SpecificExtraction> extractions,
                                                     @NonNull final Map<String, CompoundExtraction> compoundExtractions)
            throws JSONException {
        final String documentId = document.getId();

        final JSONObject feedbackForExtractions = new JSONObject();
        for (Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
            final Extraction extraction = entry.getValue();
            final JSONObject extractionData = new JSONObject();
            extractionData.put("value", extraction.getValue());
            extractionData.put("entity", extraction.getEntity());
            feedbackForExtractions.put(entry.getKey(), extractionData);
        }

        final JSONObject feedbackForCompoundExtractions = new JSONObject();
        for (Map.Entry<String, CompoundExtraction> compoundExtractionEntry : compoundExtractions.entrySet()) {
            final CompoundExtraction compoundExtraction = compoundExtractionEntry.getValue();
            final JSONArray specificExtractionsFeedbackObjects = new JSONArray();
            for (final Map<String, SpecificExtraction> specificExtractionMap : compoundExtraction.getSpecificExtractionMaps()) {
                final JSONObject specificExtractionsFeedback = new JSONObject();
                for (Map.Entry<String, SpecificExtraction> specificExtractionEntry : specificExtractionMap.entrySet()) {
                    final Extraction extraction = specificExtractionEntry.getValue();
                    final JSONObject extractionData = new JSONObject();
                    extractionData.put("value", extraction.getValue());
                    extractionData.put("entity", extraction.getEntity());
                    specificExtractionsFeedback.put(specificExtractionEntry.getKey(), extractionData);
                }
                specificExtractionsFeedbackObjects.put(specificExtractionsFeedback);
            }
            feedbackForCompoundExtractions.put(compoundExtractionEntry.getKey(), specificExtractionsFeedbackObjects);
        }

        return getSessionManager().getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return getApiCommunicator().sendFeedback(documentId, feedbackForExtractions, feedbackForCompoundExtractions, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<JSONObject, Document>() {
            @Override
            public Document then(Task<JSONObject> task) throws Exception {
                for (Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
                    entry.getValue().setIsDirty(false);
                }
                return document;
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Mark a {@link PaymentRequest} as paid.
     *
     * @param requestId id of request
     * @param resolvePaymentInput information of the actual payment
     */
    public Task<ResolvedPayment> resolvePaymentRequest(final String requestId, final ResolvePaymentInput resolvePaymentInput) {
        return getSessionManager().getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws JSONException {
                final Session session = task.getResult();
                JsonAdapter<ResolvePaymentBody> adapter = getMoshi().adapter(ResolvePaymentBody.class);
                String body = adapter.toJson(ResolvePaymentBodyKt.toResolvePaymentBody(resolvePaymentInput));

                return getApiCommunicator().resolvePaymentRequests(requestId, new JSONObject(body), session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, ResolvedPayment>() {
                    @Override
                    public ResolvedPayment then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<ResolvePaymentResponse> adapter = getMoshi().adapter(ResolvePaymentResponse.class);
                        ResolvePaymentResponse resolvePaymentResponse = adapter.fromJson(task.getResult().toString());

                        return ResolvedPaymentKt.toResolvedPayment(Objects.requireNonNull(resolvePaymentResponse));
                    }
                });
    }

    /**
     * Get information about the payment of the {@link PaymentRequest}
     *
     * @param id of the paid {@link PaymentRequest}
     */
    public Task<Payment> getPayment(final String id) {
        return getSessionManager().getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) {
                final Session session = task.getResult();
                return getApiCommunicator().getPayment(id, session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, Payment>() {
                    @Override
                    public Payment then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<PaymentResponse> adapter = getMoshi().adapter(PaymentResponse.class);
                        PaymentResponse paymentResponse = adapter.fromJson(task.getResult().toString());

                        return PaymentKt.toPayment(Objects.requireNonNull(paymentResponse));
                    }
                });
    }

    public Task<Void> logErrorEvent(final ErrorEvent errorEvent) {
        return getSessionManager().getSession()
                .onSuccessTask(task -> {
                    final Session session = task.getResult();
                    JsonAdapter<ErrorEvent> adapter = getMoshi().adapter(ErrorEvent.class);
                    String body = adapter.toJson(errorEvent);
                    return getApiCommunicator().logErrorEvent(new JSONObject(body), session);
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccessTask(task -> null, Task.BACKGROUND_EXECUTOR);
    }
}
