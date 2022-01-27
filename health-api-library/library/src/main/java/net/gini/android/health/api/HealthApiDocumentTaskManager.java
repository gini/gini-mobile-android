package net.gini.android.health.api;

import android.net.Uri;
import android.util.Size;

import androidx.annotation.NonNull;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.models.CompoundExtraction;
import net.gini.android.core.api.models.Document;
import net.gini.android.core.api.models.Extraction;
import net.gini.android.core.api.models.PaymentRequest;
import net.gini.android.core.api.models.SpecificExtraction;
import net.gini.android.core.api.requests.PaymentRequestBody;
import net.gini.android.health.api.models.Page;
import net.gini.android.health.api.models.PageKt;
import net.gini.android.health.api.models.PaymentProvider;
import net.gini.android.health.api.models.PaymentProviderKt;
import net.gini.android.health.api.models.PaymentRequestInput;
import net.gini.android.health.api.models.PaymentRequestInputKt;
import net.gini.android.health.api.response.LocationResponse;
import net.gini.android.health.api.response.PageResponse;
import net.gini.android.health.api.response.PaymentProviderResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Alpár Szotyori on 25.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */
public class HealthApiDocumentTaskManager extends DocumentTaskManager<HealthApiCommunicator> {

    public HealthApiDocumentTaskManager(HealthApiCommunicator apiCommunicator, SessionManager sessionManager, GiniApiType giniApiType, Moshi moshi) {
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

    @NonNull
    private Task<List<Page>> getPages(@NonNull final String documentId) {
        return getSessionManager().getSession().onSuccessTask(task -> {
            final Session session = task.getResult();
            return getApiCommunicator().getPages(documentId, session);
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(task -> {
                    final Type type = Types.newParameterizedType(List.class, PageResponse.class);
                    final JsonAdapter<List<PageResponse>> adapter = getMoshi().adapter(type);
                    final List<PageResponse> requestResponse = adapter.fromJson(task.getResult().toString());

                    return PageKt.toPageList(Objects.requireNonNull(requestResponse), getApiCommunicator().getBaseUri());
                });
    }

    /**
     * Get the rendered image of a page as byte[]
     *
     * @param documentId id of document
     * @param page page of document
     */
    public Task<byte[]> getPageImage(final String documentId, final int page) {
        return getPages(documentId)
                .onSuccessTask(task -> {
                    final List<Page> pages = task.getResult();

                    final Uri imageUri = PageKt.getPageByPageNumber(pages, page)
                            .getLargestImageUriSmallerThan(new Size(2000, 2000));

                    if (imageUri != null) {
                        return getFile(imageUri.toString());
                    } else {
                        throw new NoSuchElementException("No page image found for page number " + page + "in document " + documentId);
                    }
                });
    }

    /**
     * A payment provider is a Gini partner which integrated the GiniPay for Banks SDK into their mobile apps.
     *
     * @return A list of {@link PaymentProvider}
     */
    public Task<List<PaymentProvider>> getPaymentProviders() {
        return getSessionManager().getSession()
                .onSuccessTask(task -> {
                    final Session session = task.getResult();
                    return getApiCommunicator().getPaymentProviders(session);
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccessTask(task -> {
                    Type type = Types.newParameterizedType(List.class, PaymentProviderResponse.class);
                    JsonAdapter<List<PaymentProviderResponse>> adapter = getMoshi().adapter(type);
                    List<PaymentProviderResponse> paymentProviderResponses = adapter.fromJson(task.getResult().toString());

                    List<Task<PaymentProvider>> tasks = new ArrayList<>();
                    for (PaymentProviderResponse paymentProviderResponse : Objects.requireNonNull(paymentProviderResponses)) {
                        tasks.add(getFile(paymentProviderResponse.getIconLocation())
                                .onSuccess(fileTask -> {
                                    byte[] icon = fileTask.getResult();
                                    return PaymentProviderKt.toPaymentProvider(paymentProviderResponse, icon);
                                }));
                    }

                    return Task.whenAllResult(tasks);
                });
    }

    /**
     * @return {@link PaymentProvider] for the given id.
     */
    public Task<PaymentProvider> getPaymentProvider(final String id) {
        return getSessionManager().getSession()
                .onSuccessTask(task -> {
                    final Session session = task.getResult();
                    return getApiCommunicator().getPaymentProvider(id, session);
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccessTask(task -> {
                    JsonAdapter<PaymentProviderResponse> adapter = getMoshi().adapter(PaymentProviderResponse.class);
                    final PaymentProviderResponse paymentProviderResponse = adapter.fromJson(task.getResult().toString());

                    return getFile(Objects.requireNonNull(paymentProviderResponse).getIconLocation())
                            .onSuccess(fileTask -> {
                                byte[] icon = fileTask.getResult();
                                return PaymentProviderKt.toPaymentProvider(paymentProviderResponse, icon);
                            });
                });
    }

    /**
     *  A {@link PaymentRequest} is used to have on the backend the intent of making a payment
     *  for a document with its (modified) extractions and specific payment provider.
     *
     *  @return Id of the {@link PaymentRequest}
     */
    public Task<String> createPaymentRequest(final PaymentRequestInput paymentRequestInput) {
        return getSessionManager().getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws JSONException {
                final Session session = task.getResult();
                JsonAdapter<PaymentRequestBody> adapter = getMoshi().adapter(PaymentRequestBody.class);
                String body = adapter.toJson(PaymentRequestInputKt.toPaymentRequestBody(paymentRequestInput));

                return getApiCommunicator().postPaymentRequests(new JSONObject(body), session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, String>() {
                    @Override
                    public String then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<LocationResponse> adapter = getMoshi().adapter(LocationResponse.class);
                        LocationResponse locationResponse = adapter.fromJson(task.getResult().toString());

                        String location = Objects.requireNonNull(locationResponse).getLocation();

                        return location.substring(location.lastIndexOf("/") + 1);
                    }
                });
    }

}
