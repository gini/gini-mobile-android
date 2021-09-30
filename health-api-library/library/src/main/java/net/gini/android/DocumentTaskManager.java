package net.gini.android;

import static net.gini.android.Utils.CHARSET_UTF8;
import static net.gini.android.Utils.checkNotNull;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import net.gini.android.authorization.Session;
import net.gini.android.authorization.SessionManager;
import net.gini.android.models.Box;
import net.gini.android.models.CompoundExtraction;
import net.gini.android.models.Document;
import net.gini.android.models.Extraction;
import net.gini.android.models.ExtractionsContainer;
import net.gini.android.models.Payment;
import net.gini.android.models.PaymentKt;
import net.gini.android.models.PaymentProvider;
import net.gini.android.models.PaymentProviderKt;
import net.gini.android.models.PaymentRequest;
import net.gini.android.models.PaymentRequestInput;
import net.gini.android.models.PaymentRequestKt;
import net.gini.android.models.ResolvePaymentInput;
import net.gini.android.models.ResolvedPayment;
import net.gini.android.models.ResolvedPaymentKt;
import net.gini.android.models.ReturnReason;
import net.gini.android.models.SpecificExtraction;
import net.gini.android.requests.ErrorEvent;
import net.gini.android.requests.PaymentRequestBody;
import net.gini.android.requests.PaymentRequestBodyKt;
import net.gini.android.requests.ResolvePaymentBody;
import net.gini.android.requests.ResolvePaymentBodyKt;
import net.gini.android.response.LocationResponse;
import net.gini.android.response.PaymentProviderResponse;
import net.gini.android.response.PaymentRequestResponse;
import net.gini.android.response.PaymentResponse;
import net.gini.android.response.ResolvePaymentResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import bolts.Continuation;
import bolts.Task;

/**
 * The DocumentTaskManager is a high level API on top of the Gini API, which is used via the ApiCommunicator. It
 * provides high level methods to handle document related tasks easily.
 */
public class DocumentTaskManager {

    private final GiniApiType mGiniApiType;
    private Map<Document, Boolean> mDocumentPollingsInProgress = new ConcurrentHashMap<>();

    /**
     * The available document type hints. See the documentation for more information.
     */
    public enum DocumentType {
        BANK_STATEMENT("BankStatement"),
        CONTRACT("Contract"),
        INVOICE("Invoice"),
        RECEIPT("Receipt"),
        REMINDER("Reminder"),
        REMITTANCE_SLIP("RemittanceSlip"),
        TRAVEL_EXPENSE_REPORT("TravelExpenseReport"),
        OTHER("Other");

        private final String apiDoctypeHint;

        DocumentType(String apiDoctypeHint) {
            this.apiDoctypeHint = apiDoctypeHint;
        }

        public String getApiDoctypeHint() {
            return apiDoctypeHint;
        }
    }

    /**
     * The time in milliseconds between HTTP requests when a document is polled.
     */
    public static long POLLING_INTERVAL = 1000;

    /**
     * The default compression rate which is used for JPEG compression in per cent.
     */
    public final static int DEFAULT_COMPRESSION = 50;

    /**
     * The ApiCommunicator instance which is used to communicate with the Gini API.
     */
    final ApiCommunicator mApiCommunicator;  // Visible for testing

    /**
     * The ApiCommunicator instance which is used to communicate with the Gini API.
     */
    private final Moshi mMoshi;  // Visible for testing
    /**
     * The SessionManager instance which is used to create the documents.
     */
    private final SessionManager mSessionManager;

    public DocumentTaskManager(final ApiCommunicator apiCommunicator, final SessionManager sessionManager,
                               final GiniApiType giniApiType, Moshi moshi) {
        mApiCommunicator = checkNotNull(apiCommunicator);
        mSessionManager = checkNotNull(sessionManager);
        mGiniApiType = checkNotNull(giniApiType);
        mMoshi = moshi;
    }

    /**
     * A Continuation that uses the JSON response from the Gini API and returns a new Document instance from the JSON.
     */
    private static final Continuation<JSONObject, Document> DOCUMENT_FROM_RESPONSE =
            new Continuation<JSONObject, Document>() {
                @Override
                public Document then(Task<JSONObject> task) throws Exception {
                    return Document.fromApiResponse(task.getResult());
                }
            };

    /**
     * Deletes a Gini partial document and all its parent composite documents.
     * <br>
     * Partial documents can be deleted only, if they don't belong to any composite documents and
     * this method deletes the parents before deleting the partial document.
     *
     * @param documentId The id of an existing partial document
     * @return A Task which will resolve to an empty string.
     */
    public Task<String> deletePartialDocumentAndParents(@NonNull final String documentId) {
        return getDocument(documentId).onSuccessTask(new Continuation<Document, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Document> documentTask) throws Exception {
                final Document document = documentTask.getResult();
                return deleteDocuments(document.getCompositeDocuments());
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Void, Task<Session>>() {
            @Override
            public Task<Session> then(final Task<Void> task) throws Exception {
                return mSessionManager.getSession();
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Session, Task<String>>() {
            @Override
            public Task<String> then(final Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.deleteDocument(documentId, session);
            }
        });
    }

    /**
     * Deletes a Gini document.
     * <p>
     * For deleting partial documents use {@link #deletePartialDocumentAndParents(String)} instead.
     *
     * @param documentId The id of an existing document
     * @return A Task which will resolve to an empty string.
     */
    public Task<String> deleteDocument(@NonNull final String documentId) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<String>>() {
            @Override
            public Task<String> then(final Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.deleteDocument(documentId, session);
            }
        });
    }

    private Task<Void> deleteDocuments(@NonNull final List<Uri> documentUris) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Session> sessionTask) throws Exception {
                final Session session = sessionTask.getResult();
                final List<Task<String>> deleteTasks = new ArrayList<>();
                for (final Uri documentUri : documentUris) {
                    deleteTasks.add(mApiCommunicator.deleteDocument(documentUri, session));
                }
                return Task.whenAll(deleteTasks);
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Uploads raw data and creates a new Gini partial document.
     *
     * @param document     A byte array representing an image, a pdf or UTF-8 encoded text
     * @param contentType  The media type of the uploaded data
     * @param filename     Optional the filename of the given document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createPartialDocument(@NonNull final byte[] document, @NonNull final String contentType,
                                                @Nullable final String filename, @Nullable final DocumentType documentType) {
        return createPartialDocumentInternal(document, contentType, filename, documentType, null);
    }

    /**
     * Uploads raw data and creates a new Gini partial document.
     *
     * @param document         A byte array representing an image, a pdf or UTF-8 encoded text
     * @param contentType      The media type of the uploaded data
     * @param filename         Optional the filename of the given document
     * @param documentType     Optional a document type hint. See the documentation for the document type hints for
     *                         possible values
     * @param documentMetadata Additional information related to the document (e.g. the branch id
     *                         to which the client app belongs)
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createPartialDocument(@NonNull final byte[] document, @NonNull final String contentType,
                                                @Nullable final String filename, @Nullable final DocumentType documentType, @NonNull final DocumentMetadata documentMetadata) {
        return createPartialDocumentInternal(document, contentType, filename, documentType, documentMetadata);
    }

    private Task<Document> createPartialDocumentInternal(@NonNull final byte[] document, @NonNull final String contentType,
                                                         @Nullable final String filename, @Nullable final DocumentType documentType, @Nullable final DocumentMetadata documentMetadata) {
        if (!mGiniApiType.getGiniJsonMediaType().equals(MediaTypes.GINI_JSON_V2)) {
            throw new UnsupportedOperationException(
                    "Partial documents may be used only with the default Gini API. Use GiniApiType.DEFAULT.");
        }
        return createDocumentInternal(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                final String partialDocumentMediaType = MediaTypes
                        .forPartialDocument(mGiniApiType.getGiniPartialMediaType(), checkNotNull(contentType));
                return mApiCommunicator
                        .uploadDocument(document, partialDocumentMediaType, filename, apiDoctypeHint, session, documentMetadata);
            }
        });
    }

    /**
     * Creates a new Gini composite document.
     *
     * @param documents    A list of partial documents which should be part of a multi-page document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createCompositeDocument(@NonNull final List<Document> documents, @Nullable final DocumentType documentType) {
        if (!mGiniApiType.getGiniJsonMediaType().equals(MediaTypes.GINI_JSON_V2)) {
            throw new UnsupportedOperationException(
                    "Composite documents may be used only with the default Gini API. Use GiniApiType.DEFAULT.");
        }
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                final byte[] compositeJson = createCompositeJson(documents);
                return mApiCommunicator
                        .uploadDocument(compositeJson, mGiniApiType.getGiniCompositeJsonMediaType(), null, apiDoctypeHint, session, null);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Uri, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                return getDocument(uploadTask.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Creates a new Gini composite document. The input Map must contain the partial documents as keys. These will be
     * part of the multi-page document. The value for each partial document key is the amount in degrees the document
     * has been rotated by the user.
     *
     * @param documentRotationMap A map of partial documents and their rotation in degrees
     * @param documentType        Optional a document type hint. See the documentation for the document type hints for
     *                            possible values
     * @return A Task which will resolve to the Document instance of the freshly created document.
     */
    public Task<Document> createCompositeDocument(@NonNull final LinkedHashMap<Document, Integer> documentRotationMap,
                                                  @Nullable final DocumentType documentType) {
        if (!mGiniApiType.getGiniJsonMediaType().equals(MediaTypes.GINI_JSON_V2)) {
            throw new UnsupportedOperationException(
                    "Composite documents may be used only with the default Gini API. Use GiniApiType.DEFAULT.");
        }
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                final byte[] compositeJson = createCompositeJson(documentRotationMap);
                return mApiCommunicator
                        .uploadDocument(compositeJson, mGiniApiType.getGiniCompositeJsonMediaType(), null, apiDoctypeHint, session, null);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccessTask(new Continuation<Uri, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                return getDocument(uploadTask.getResult());
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    private byte[] createCompositeJson(@NonNull final List<Document> documents)
            throws JSONException {
        final LinkedHashMap<Document, Integer> documentRotationMap = new LinkedHashMap<>();
        for (final Document document : documents) {
            documentRotationMap.put(document, 0);
        }
        return createCompositeJson(documentRotationMap);
    }

    private byte[] createCompositeJson(@NonNull final LinkedHashMap<Document, Integer> documentRotationMap)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray partialDocuments = new JSONArray();
        for (final Map.Entry<Document, Integer> entry : documentRotationMap.entrySet()) {
            final Document document = entry.getKey();
            int rotation = entry.getValue();
            // Converts input degrees to degrees between [0,360)
            rotation = ((rotation % 360) + 360) % 360;
            final JSONObject partialDoc = new JSONObject();
            partialDoc.put("document", document.getUri());
            partialDoc.put("rotationDelta", rotation);
            partialDocuments.put(partialDoc);
        }
        jsonObject.put("partialDocuments", partialDocuments);
        return jsonObject.toString().getBytes(CHARSET_UTF8);
    }

    /**
     * Uploads raw data and creates a new Gini document.
     *
     * @param document     A byte array representing an image, a pdf or UTF-8 encoded text
     * @param filename     Optional the filename of the given document.
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values.
     * @return A Task which will resolve to the Document instance of the freshly created document.
     *
     * <b>Important:</b> If using the default Gini API, then use {@link #createPartialDocument(byte[], String, String, DocumentType)} to upload the
     * document and then call {@link #createCompositeDocument(LinkedHashMap, DocumentType)}
     * (or {@link #createCompositeDocument(List, DocumentType)}) to finish document creation. The
     * returned composite document can be used to poll the processing state, to retrieve extractions
     * and to send feedback.
     */
    public Task<Document> createDocument(@NonNull final byte[] document, @Nullable final String filename,
                                         @Nullable final DocumentType documentType) {
        return createDocumentInternal(document, filename, documentType, null);
    }

    /**
     * Uploads raw data and creates a new Gini document.
     *
     * @param document         A byte array representing an image, a pdf or UTF-8 encoded text
     * @param filename         Optional the filename of the given document.
     * @param documentType     Optional a document type hint. See the documentation for the document type hints for
     *                         possible values.
     * @param documentMetadata Additional information related to the document (e.g. the branch id
     *                         to which the client app belongs)
     * @return A Task which will resolve to the Document instance of the freshly created document.
     *
     * <b>Important:</b> If using the default Gini API, then use {@link #createPartialDocument(byte[], String, String, DocumentType)} to upload the
     * document and then call {@link #createCompositeDocument(LinkedHashMap, DocumentType)}
     * (or {@link #createCompositeDocument(List, DocumentType)}) to finish document creation. The
     * returned composite document can be used to poll the processing state, to retrieve extractions
     * and to send feedback.
     */
    public Task<Document> createDocument(@NonNull final byte[] document, @Nullable final String filename,
                                         @Nullable final DocumentType documentType, @NonNull final DocumentMetadata documentMetadata) {
        return createDocumentInternal(document, filename, documentType, documentMetadata);
    }

    private Task<Document> createDocumentInternal(@NonNull final byte[] document, @Nullable final String filename,
                                                  @Nullable final DocumentType documentType, @Nullable final DocumentMetadata documentMetadata) {
        return createDocumentInternal(new Continuation<Session, Task<Uri>>() {
            @Override
            public Task<Uri> then(Task<Session> sessionTask) throws Exception {
                String apiDoctypeHint = null;
                if (documentType != null) {
                    apiDoctypeHint = documentType.getApiDoctypeHint();
                }
                final Session session = sessionTask.getResult();
                return mApiCommunicator
                        .uploadDocument(document, MediaTypes.IMAGE_JPEG, filename, apiDoctypeHint, session, documentMetadata);
            }
        });
    }

    private Task<Document> createDocumentInternal(@NonNull final Continuation<Session, Task<Uri>> successContinuation) {
        return mSessionManager.getSession()
                .onSuccessTask(successContinuation, Task.BACKGROUND_EXECUTOR)
                .onSuccessTask(new Continuation<Uri, Task<Document>>() {
                    @Override
                    public Task<Document> then(Task<Uri> uploadTask) throws Exception {
                        return getDocument(uploadTask.getResult());
                    }
                }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Get the extractions for the given document.
     *
     * @param document The Document instance for whose document the extractions are returned.
     * @return A Task which will resolve to an {@link ExtractionsContainer} object.
     */
    public Task<ExtractionsContainer> getAllExtractions(@NonNull final Document document) {
        final String documentId = document.getId();
        return mSessionManager.getSession()
                .onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
                    @Override
                    public Task<JSONObject> then(Task<Session> sessionTask) {
                        final Session session = sessionTask.getResult();
                        return mApiCommunicator.getExtractions(documentId, session);
                    }
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, ExtractionsContainer>() {
                    @Override
                    public ExtractionsContainer then(Task<JSONObject> task) throws Exception {
                        final JSONObject responseData = task.getResult();
                        final JSONObject candidatesData = responseData.getJSONObject("candidates");
                        Map<String, List<Extraction>> candidates =
                                extractionCandidatesFromApiResponse(candidatesData);

                        final Map<String, SpecificExtraction> specificExtractions =
                                parseSpecificExtractions(responseData.getJSONObject("extractions"), candidates);

                        final Map<String, CompoundExtraction> compoundExtractions =
                                parseCompoundExtractions(responseData.optJSONObject("compoundExtractions"), candidates);

                        final List<ReturnReason> returnReasons = parseReturnReason(responseData.optJSONArray("returnReasons"));

                        return new ExtractionsContainer(specificExtractions, compoundExtractions, returnReasons);
                    }
                }, Task.BACKGROUND_EXECUTOR);

    }

    @NonNull
    private Map<String, SpecificExtraction> parseSpecificExtractions(@NonNull final JSONObject specificExtractionsJson,
                                                                     @NonNull final Map<String, List<Extraction>> candidates)
            throws JSONException {
        final Map<String, SpecificExtraction> specificExtractions = new HashMap<>();
        @SuppressWarnings("unchecked")
        // Quote Android Source: "/* Return a raw type for API compatibility */"
        final Iterator<String> extractionsNameIterator = specificExtractionsJson.keys();
        while (extractionsNameIterator.hasNext()) {
            final String extractionName = extractionsNameIterator.next();
            final JSONObject extractionData = specificExtractionsJson.getJSONObject(extractionName);
            final Extraction extraction = extractionFromApiResponse(extractionData);
            List<Extraction> candidatesForExtraction = new ArrayList<Extraction>();
            if (extractionData.has("candidates")) {
                final String candidatesName = extractionData.getString("candidates");
                if (candidates.containsKey(candidatesName)) {
                    candidatesForExtraction = candidates.get(candidatesName);
                }
            }
            final SpecificExtraction specificExtraction =
                    new SpecificExtraction(extractionName, extraction.getValue(),
                            extraction.getEntity(), extraction.getBox(),
                            candidatesForExtraction);
            specificExtractions.put(extractionName, specificExtraction);
        }
        return specificExtractions;
    }

    private Map<String, CompoundExtraction> parseCompoundExtractions(@Nullable final JSONObject compoundExtractionsJson,
                                                                     @NonNull final Map<String, List<Extraction>> candidates)
            throws JSONException {
        if (compoundExtractionsJson == null) {
            return Collections.emptyMap();
        }
        final HashMap<String, CompoundExtraction> compoundExtractions = new HashMap<>();
        final Iterator<String> extractionsNameIterator = compoundExtractionsJson.keys();
        while (extractionsNameIterator.hasNext()) {
            final String extractionName = extractionsNameIterator.next();
            final List<Map<String, SpecificExtraction>> specificExtractionMaps = new ArrayList<>();
            final JSONArray compoundExtractionData = compoundExtractionsJson.getJSONArray(extractionName);
            for (int i = 0; i < compoundExtractionData.length(); i++) {
                final JSONObject specificExtractionsData = compoundExtractionData.getJSONObject(i);
                specificExtractionMaps.add(parseSpecificExtractions(specificExtractionsData, candidates));
            }
            compoundExtractions.put(extractionName, new CompoundExtraction(extractionName, specificExtractionMaps));
        }
        return compoundExtractions;
    }

    private List<ReturnReason> parseReturnReason(@Nullable final JSONArray returnReasonsJson) throws JSONException {
        if (returnReasonsJson == null) {
            return Collections.emptyList();
        }
        final List<ReturnReason> returnReasons = new ArrayList<>();
        for (int i = 0; i < returnReasonsJson.length(); i++) {
            final JSONObject returnReasonJson = returnReasonsJson.getJSONObject(i);

            Map<String, String> localizedLabels = new HashMap<>();

            final Iterator<String> keys = returnReasonJson.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                if (key.equals("id")) {
                    continue;
                }
                localizedLabels.put(key, returnReasonJson.getString(key));
            }
            returnReasons.add(new ReturnReason(returnReasonJson.getString("id"), localizedLabels));
        }
        return returnReasons;
    }

    /**
     * Get the document with the given unique identifier.
     *
     * @param documentId The unique identifier of the document.
     * @return A document instance representing all the document's metadata.
     */
    public Task<Document> getDocument(@NonNull final String documentId) {
        checkNotNull(documentId);
        return mSessionManager.getSession()
                .onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
                    @Override
                    public Task<JSONObject> then(Task<Session> sessionTask) throws Exception {
                        final Session session = sessionTask.getResult();
                        return mApiCommunicator.getDocument(documentId, session);
                    }
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(DOCUMENT_FROM_RESPONSE, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Get the document with the given unique identifier.
     *
     * <b>Please note that this method may use a slightly corrected URI from which it gets the document (e.g. if the
     * URI's host does not conform to the base URL of the Gini API). Therefore it is not possibly to use this method to
     * get a document from an arbitrary URI.</b>
     *
     * @param documentUri The URI of the document.
     * @return A document instance representing all the document's metadata.
     */
    public Task<Document> getDocument(@NonNull final Uri documentUri) {
        checkNotNull(documentUri);
        return mSessionManager.getSession()
                .onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
                    @Override
                    public Task<JSONObject> then(Task<Session> sessionTask) throws Exception {
                        final Session session = sessionTask.getResult();
                        return mApiCommunicator.getDocument(documentUri, session);
                    }
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(DOCUMENT_FROM_RESPONSE, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Continually checks the document status (via the Gini API) until the document is fully processed. To avoid
     * flooding the network, there is a pause of at least the number of seconds that is set in the POLLING_INTERVAL
     * constant of this class.
     *
     * <b>This method returns a Task which will resolve to a new document instance. It does not update the given
     * document instance.</b>
     *
     * @param document The document which will be polled.
     */
    public Task<Document> pollDocument(@NonNull final Document document) {
        if (document.getState() != Document.ProcessingState.PENDING) {
            return Task.forResult(document);
        }
        mDocumentPollingsInProgress.put(document, false);
        final String documentId = document.getId();
        return getDocument(documentId).continueWithTask(new Continuation<Document, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Document> task) throws Exception {
                if (task.isFaulted() || task.isCancelled()
                        || task.getResult().getState() != Document.ProcessingState.PENDING) {
                    mDocumentPollingsInProgress.remove(document);
                    return task;
                } else {
                    if (mDocumentPollingsInProgress.containsKey(document)
                            && mDocumentPollingsInProgress.get(document)) {
                        mDocumentPollingsInProgress.remove(document);
                        return Task.cancelled();
                    } else {
                        // The continuation is executed in a background thread by Bolts, so it does not block the UI
                        // when we sleep here. Infinite recursions are also prevented by Bolts (the task will then resolve
                        // to a failure).
                        Thread.sleep(POLLING_INTERVAL);
                        return pollDocument(document);
                    }
                }
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Cancels document polling.
     *
     * @param document The document which is being polled
     */
    public void cancelDocumentPolling(@NonNull final Document document) {
        if (mDocumentPollingsInProgress.containsKey(document)) {
            mDocumentPollingsInProgress.put(document, true);
        }
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

        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.sendFeedback(documentId, feedbackForExtractions, session);
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

        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.sendFeedback(documentId, feedbackForExtractions, feedbackForCompoundExtractions, session);
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
     * Sends an error report for the given document to Gini. If the processing result for a document was not
     * satisfactory (e.g. extractions where empty or incorrect), you can create an error report for a document. This
     * allows Gini to analyze and correct the problem that was found.
     *
     * <b>The owner of this document must agree that Gini can use this document for debugging and error analysis.</b>
     *
     * @param document    The erroneous document.
     * @param summary     Optional a short summary of the occurred error.
     * @param description Optional a more detailed description of the occurred error.
     * @return A Task which will resolve to an error ID. This is a unique identifier for your error report
     * and can be used to refer to the reported error towards the Gini support.
     */
    public Task<String> reportDocument(@NonNull final Document document, @Nullable final String summary,
                                       @Nullable final String description) {
        final String documentId = document.getId();
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.errorReportForDocument(documentId, summary, description, session);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<JSONObject, String>() {
            @Override
            public String then(Task<JSONObject> task) throws Exception {
                final JSONObject responseData = task.getResult();
                return responseData.getString("errorId");
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Gets the layout of a document. The layout of the document describes the textual content of a document with
     * positional information, based on the processed document.
     *
     * @param document The document for which the layouts is requested.
     * @return A task which will resolve to a string containing the layout xml.
     */
    public Task<JSONObject> getLayout(@NonNull final Document document) {
        final String documentId = document.getId();
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws Exception {
                final Session session = task.getResult();
                return mApiCommunicator.getLayoutForDocument(documentId, session);
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * A payment provider is a Gini partner which integrated the GiniPay for Banks SDK into their mobile apps.
     *
     * @return A list of {@link PaymentProvider}
     */
    public Task<List<PaymentProvider>> getPaymentProviders() {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONArray>>() {
            @Override
            public Task<JSONArray> then(Task<Session> task) {
                final Session session = task.getResult();
                return mApiCommunicator.getPaymentProviders(session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONArray, List<PaymentProvider>>() {
                    @Override
                    public List<PaymentProvider> then(Task<JSONArray> task) throws Exception {
                        Type type = Types.newParameterizedType(List.class, PaymentProviderResponse.class);
                        JsonAdapter<List<PaymentProviderResponse>> adapter = mMoshi.adapter(type);
                        List<PaymentProviderResponse> paymentProviderResponse = adapter.fromJson(task.getResult().toString());

                        List<PaymentProvider> paymentProviders = new ArrayList<>();
                        for (PaymentProviderResponse paymentProvider : paymentProviderResponse != null ? paymentProviderResponse : Collections.<PaymentProviderResponse>emptyList()) {
                            paymentProviders.add(PaymentProviderKt.toPaymentProvider(paymentProvider));
                        }
                        return paymentProviders;
                    }
                });
    }

    /**
     * @return {@link PaymentProvider] for the given id.
     */
    public Task<PaymentProvider> getPaymentProvider(final String id) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) {
                final Session session = task.getResult();
                return mApiCommunicator.getPaymentProvider(id, session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, PaymentProvider>() {
                    @Override
                    public PaymentProvider then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<PaymentProviderResponse> adapter = mMoshi.adapter(PaymentProviderResponse.class);
                        PaymentProviderResponse paymentProviderResponse = adapter.fromJson(task.getResult().toString());

                        return PaymentProviderKt.toPaymentProvider(Objects.requireNonNull(paymentProviderResponse));
                    }
                });
    }

    /**
     *  A {@link PaymentRequest} is used to have on the backend the intent of making a payment
     *  for a document with its (modified) extractions and specific payment provider.
     *
     *  @return Id of the {@link PaymentRequest}
     */
    public Task<String> createPaymentRequest(final PaymentRequestInput paymentRequestInput) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws JSONException {
                final Session session = task.getResult();
                JsonAdapter<PaymentRequestBody> adapter = mMoshi.adapter(PaymentRequestBody.class);
                String body = adapter.toJson(PaymentRequestBodyKt.toPaymentRequestBody(paymentRequestInput));

                return mApiCommunicator.postPaymentRequests(new JSONObject(body), session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, String>() {
                    @Override
                    public String then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<LocationResponse> adapter = mMoshi.adapter(LocationResponse.class);
                        LocationResponse locationResponse = adapter.fromJson(task.getResult().toString());

                        String location = Objects.requireNonNull(locationResponse).getLocation();

                        return location.substring(location.lastIndexOf("/") + 1);
                    }
                });
    }

    /**
     * @return {PaymentRequest} for the given id
     */
    public Task<PaymentRequest> getPaymentRequest(final String id) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) {
                final Session session = task.getResult();
                return mApiCommunicator.getPaymentRequest(id, session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, PaymentRequest>() {
                    @Override
                    public PaymentRequest then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<PaymentRequestResponse> adapter = mMoshi.adapter(PaymentRequestResponse.class);
                        PaymentRequestResponse requestResponse = adapter.fromJson(task.getResult().toString());

                        return PaymentRequestKt.toPaymentRequest(Objects.requireNonNull(requestResponse));
                    }
                });
    }

    /**
     * @return List of payment {@link PaymentRequest}
     */
    public Task<List<PaymentRequest>> getPaymentRequests() {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONArray>>() {
            @Override
            public Task<JSONArray> then(Task<Session> task) {
                final Session session = task.getResult();
                return mApiCommunicator.getPaymentRequests(session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONArray, List<PaymentRequest>>() {
                    @Override
                    public List<PaymentRequest> then(Task<JSONArray> task) throws Exception {
                        Type type = Types.newParameterizedType(List.class, PaymentRequestResponse.class);
                        JsonAdapter<List<PaymentRequestResponse>> adapter = mMoshi.adapter(type);
                        List<PaymentRequestResponse> paymentRequestResponses = adapter.fromJson(task.getResult().toString());

                        List<PaymentRequest> paymentProviders = new ArrayList<>();
                        for (PaymentRequestResponse paymentRequestResponse : paymentRequestResponses != null ? paymentRequestResponses : Collections.<PaymentRequestResponse>emptyList()) {
                            paymentProviders.add(PaymentRequestKt.toPaymentRequest(paymentRequestResponse));
                        }
                        return paymentProviders;
                    }
                });
    }

    /**
     * Mark a {@link PaymentRequest} as paid.
     *
     * @param requestId id of request
     * @param resolvePaymentInput information of the actual payment
     */
    public Task<ResolvedPayment> resolvePaymentRequest(final String requestId, final ResolvePaymentInput resolvePaymentInput) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) throws JSONException {
                final Session session = task.getResult();
                JsonAdapter<ResolvePaymentBody> adapter = mMoshi.adapter(ResolvePaymentBody.class);
                String body = adapter.toJson(ResolvePaymentBodyKt.toResolvePaymentBody(resolvePaymentInput));

                return mApiCommunicator.resolvePaymentRequests(requestId, new JSONObject(body), session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, ResolvedPayment>() {
                    @Override
                    public ResolvedPayment then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<ResolvePaymentResponse> adapter = mMoshi.adapter(ResolvePaymentResponse.class);
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
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<JSONObject>>() {
            @Override
            public Task<JSONObject> then(Task<Session> task) {
                final Session session = task.getResult();
                return mApiCommunicator.getPayment(id, session);
            }
        }, Task.BACKGROUND_EXECUTOR)
                .onSuccess(new Continuation<JSONObject, Payment>() {
                    @Override
                    public Payment then(Task<JSONObject> task) throws Exception {
                        JsonAdapter<PaymentResponse> adapter = mMoshi.adapter(PaymentResponse.class);
                        PaymentResponse paymentResponse = adapter.fromJson(task.getResult().toString());

                        return PaymentKt.toPayment(Objects.requireNonNull(paymentResponse));
                    }
                });
    }

    /**
     * Get the rendered image of a page as byte[]
     *
     * @param documentId id of document
     * @param page page of document
     */
    public Task<byte[]> getPageImage(final String documentId, final int page) {
        return mSessionManager.getSession().onSuccessTask(new Continuation<Session, Task<byte[]>>() {
            @Override
            public Task<byte[]> then(Task<Session> task) {
                final Session session = task.getResult();
                return mApiCommunicator.getPageImage(documentId, page, session);
            }
        }, Task.BACKGROUND_EXECUTOR);
    }

    public Task<Void> logErrorEvent(final ErrorEvent errorEvent) {
        return mSessionManager.getSession()
                .onSuccessTask(task -> {
                    final Session session = task.getResult();
                    JsonAdapter<ErrorEvent> adapter = mMoshi.adapter(ErrorEvent.class);
                    String body = adapter.toJson(errorEvent);
                    return mApiCommunicator.logErrorEvent(new JSONObject(body), session);
                }, Task.BACKGROUND_EXECUTOR)
                .onSuccessTask(task -> null, Task.BACKGROUND_EXECUTOR);
    }

    /**
     * Helper method which takes the JSON response of the Gini API as input and returns a mapping where the key is the
     * name of the candidates list (e.g. "amounts" or "dates") and the value is a list of extraction instances.
     *
     * @param responseData The JSON data of the key candidates from the response of the Gini API.
     * @return The created mapping as described above.
     * @throws JSONException If the JSON data does not have the expected structure or if there is invalid data.
     */
    protected HashMap<String, List<Extraction>> extractionCandidatesFromApiResponse(@NonNull final JSONObject responseData)
            throws JSONException {
        final HashMap<String, List<Extraction>> candidatesByEntity = new HashMap<String, List<Extraction>>();

        @SuppressWarnings("unchecked") // Quote Android Source: "/* Return a raw type for API compatibility */"
        final Iterator<String> entityNameIterator = responseData.keys();
        while (entityNameIterator.hasNext()) {
            final String entityName = entityNameIterator.next();
            final JSONArray candidatesListData = responseData.getJSONArray(entityName);
            final ArrayList<Extraction> candidates = new ArrayList<Extraction>();
            for (int i = 0, length = candidatesListData.length(); i < length; i += 1) {
                final JSONObject extractionData = candidatesListData.getJSONObject(i);
                candidates.add(extractionFromApiResponse(extractionData));
            }
            candidatesByEntity.put(entityName, candidates);
        }
        return candidatesByEntity;
    }

    /**
     * Helper method which creates an Extraction instance from the JSON data which is returned by the Gini API.
     *
     * @param responseData The JSON data.
     * @return The created Extraction instance.
     * @throws JSONException If the JSON data does not have the expected structure or if there is invalid data.
     */
    protected Extraction extractionFromApiResponse(@NonNull final JSONObject responseData) throws JSONException {
        final String entity = responseData.getString("entity");
        final String value = responseData.getString("value");
        // The box is optional for some extractions.
        Box box = null;
        if (responseData.has("box")) {
            box = Box.fromApiResponse(responseData.getJSONObject("box"));
        }
        return new Extraction(value, entity, box);
    }
}
