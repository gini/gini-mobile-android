package net.gini.android.core.api;

import static com.android.volley.Request.Method.DELETE;
import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;

import static net.gini.android.core.api.Utils.checkNotNull;
import static net.gini.android.core.api.Utils.mapToUrlEncodedString;

import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.requests.BearerByteArrayRequest;
import net.gini.android.core.api.authorization.requests.BearerHeadersRequest;
import net.gini.android.core.api.authorization.requests.BearerJsonArrayRequest;
import net.gini.android.core.api.authorization.requests.BearerJsonObjectRequest;
import net.gini.android.core.api.requests.BearerUploadRequest;
import net.gini.android.core.api.requests.RetryPolicyFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import bolts.Task;


/**
 * The ApiCommunicator is responsible for communication with the Gini API. It only converts the server's responses to
 * more convenient objects (e.g. a JSON response to a JSONObject) but does not interpret the results in any way.
 * Therefore it is not recommended to use the ApiCommunicator directly, but to use the DocumentTaskManager instead which
 * provides much more convenient methods to work with the Gini API and uses defined models.
 */
public class ApiCommunicator {

    private final GiniApiType mGiniApiType;
    private final Uri mBaseUri;
    @VisibleForTesting
    public final RequestQueue mRequestQueue; // Visible for testing
    @VisibleForTesting
    public final RetryPolicyFactory mRetryPolicyFactory;

    public ApiCommunicator(final String baseUriString,
            final GiniApiType giniApiType,
            final RequestQueue mRequestQueue,
                           final RetryPolicyFactory retryPolicyFactory) {
        this.mRetryPolicyFactory = retryPolicyFactory;
        this.mGiniApiType = giniApiType;
        mBaseUri = getBaseUri(baseUriString, giniApiType);
        this.mRequestQueue = checkNotNull(mRequestQueue);
    }

    @NonNull
    public Uri getBaseUri() {
        return mBaseUri;
    }

    @NonNull
    public GiniApiType getGiniApiType() {
        return mGiniApiType;
    }

    @NonNull
    public RetryPolicyFactory getRetryPolicyFactory() {
        return mRetryPolicyFactory;
    }

    @NonNull
    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    private Uri getBaseUri(final String baseUriString, final GiniApiType giniApiType) {
        if (baseUriString != null) {
            return Uri.parse(checkNotNull(baseUriString));
        } else {
            return Uri.parse(giniApiType.getBaseUrl());
        }
    }

    public Task<Uri> uploadDocument(final byte[] documentData, final String contentType,
                                    @Nullable final String documentName, @Nullable final String docTypeHint,
                                    final Session session, @Nullable final DocumentMetadata documentMetadata) {

        final HashMap<String, String> requestQueryData = new HashMap<String, String>();
        if (documentName != null) {
            requestQueryData.put("filename", documentName);
        }
        if (docTypeHint != null) {
            requestQueryData.put("doctype", docTypeHint);
        }
        final String url = mBaseUri.buildUpon().path("documents/").encodedQuery(mapToUrlEncodedString(requestQueryData))
                .toString();
        final RequestTaskCompletionSource<Uri> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final Map<String, String> metadata;
        if (documentMetadata != null) {
            metadata = documentMetadata.getMetadata();
        } else {
            metadata = Collections.emptyMap();
        }
        final BearerUploadRequest request =
                new BearerUploadRequest(POST, url, checkNotNull(documentData), checkNotNull(contentType), session,
                        mGiniApiType, completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy(),
                        metadata);
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> getDocument(final String documentId, final Session session) {
        final String url = mBaseUri.buildUpon().path("documents/" + checkNotNull(documentId)).toString();
        return getDocument(Uri.parse(url), session);
    }

    public Task<JSONObject> getDocument(final Uri documentUri, final Session session) {
        final String url = uriRelativeToBaseUri(documentUri).toString();
        return doRequestWithJsonResponse(url, GET, session);
    }

    public Task<JSONObject> getExtractions(final String documentId, final Session session) {
        final String url = mBaseUri.buildUpon().path(String.format("documents/%s/extractions",
                                                                   checkNotNull(documentId))).toString();
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(GET, url, null, checkNotNull(session), mGiniApiType,
                        completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> getIncubatorExtractions(final String documentId, final Session session) {
        final String url = mBaseUri.buildUpon().path(String.format("documents/%s/extractions",
                checkNotNull(documentId))).toString();
        final RequestTaskCompletionSource<JSONObject> completionSource = RequestTaskCompletionSource
                .newCompletionSource();
        final BearerJsonObjectRequest request = new BearerJsonObjectRequest(GET, url, null, checkNotNull(session),
                mGiniApiType, completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy()) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = super.getHeaders();
                // The incubator is discriminated from the "normal" extractions by the accept header.
                headers.put("Accept", MediaTypes.GINI_JSON_INCUBATOR);
                return headers;
            }
        };
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<String> deleteDocument(final String documentId, final Session session) {
        final String accessToken = checkNotNull(session).getAccessToken();
        final String url = mBaseUri.buildUpon().path("documents/" + checkNotNull(documentId)).toString();
        final RequestTaskCompletionSource<String> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final StringRequest request = new StringRequest(DELETE, url, completionSource, completionSource) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        request.setRetryPolicy(mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<String> deleteDocument(final Uri documentUri, final Session session) {
        final String accessToken = checkNotNull(session).getAccessToken();
        final RequestTaskCompletionSource<String> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final StringRequest request = new StringRequest(DELETE, documentUri.toString(), completionSource, completionSource) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        request.setRetryPolicy(mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> errorReportForDocument(final String documentId, @Nullable final String summary,
                                                   @Nullable final String description, final Session session) {
        final HashMap<String, String> requestParams = new HashMap<String, String>();
        requestParams.put("summary", summary);
        requestParams.put("description", description);
        final String url = mBaseUri.buildUpon().path("documents/" + checkNotNull(documentId) + "/errorreport")
                .encodedQuery(mapToUrlEncodedString(requestParams)).toString();
        return doRequestWithJsonResponse(url, POST, session);
    }

    public Task<JSONObject> getLayoutForDocument(final String documentId, final Session session) {
        final String url =
                mBaseUri.buildUpon().path(String.format("/documents/%s/layout", checkNotNull(documentId))).toString();
        return doRequestWithJsonResponse(url, GET, session);
    }

    public Task<JSONObject> getDocumentList(final int offset, final int limit, final Session session) {
        final String url = mBaseUri.buildUpon().path("/documents")
                .appendQueryParameter("offset", Integer.toString(offset))
                .appendQueryParameter("limit", Integer.toString(limit)).toString();
        return doRequestWithJsonResponse(url, GET, session);
    }

    public Task<JSONObject> searchDocuments(final String searchTerm, @Nullable final String docType, final int offset,
                                            final int limit, final Session session) {
        final Uri.Builder url = mBaseUri.buildUpon().path("/search").appendQueryParameter("q", searchTerm)
                .appendQueryParameter("offset", Integer.toString(offset))
                .appendQueryParameter("limit", Integer.toString(limit));
        if (docType != null) {
            url.appendQueryParameter("docType", docType);
        }
        return doRequestWithJsonResponse(url.toString(), GET, checkNotNull(session));
    }

    public Task<JSONObject> getPaymentRequest(final String id, final Session session) {
        final String url = mBaseUri.buildUpon().path("/paymentRequests/").appendPath(id).toString();

        return doRequestWithJsonResponse(url, GET, checkNotNull(session));
    }

    public Task<JSONArray> getPaymentRequests(final Session session) {
        final String url = mBaseUri.buildUpon().path("/paymentRequests").toString();

        return doRequestWithJsonArrayResponse(url, GET, checkNotNull(session));
    }

    public Task<byte[]> getFile(@NonNull final String location, final Session session) {
        return doRequestWithByteArrayResponse(checkNotNull(location), GET, checkNotNull(session));
    }

    /**
     * Helper method to do a request that returns data in headers. The request is wrapped in a Task that will resolve to a
     * JSONObject.
     *
     * @param url       The full URL of the request.
     * @param method    The HTTP method of the request.
     * @param session   A valid session for the Gini API.
     * @return          A Task which will resolve to a JSONObject representing the response of the Gini API.
     */
    protected Task<JSONObject> doRequestWithHeadersResponse(final String url, int method, final JSONObject body, final Session session) {
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerHeadersRequest documentsRequest =
                new BearerHeadersRequest(method, url, body, checkNotNull(session),
                        mGiniApiType, completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy(), mGiniApiType.getGiniJsonMediaType());
        mRequestQueue.add(documentsRequest);
        return completionSource.getTask();
    }

    /**
     * Helper method to do a request that sends Json body and returns JSON data. The request is wrapped in a Task that will resolve to a
     * JSONObject.
     *
     * @param url       The full URL of the request.
     * @param method    The HTTP method of the request.
     * @param session   A valid session for the Gini API.
     * @return          A Task which will resolve to a JSONObject representing the response of the Gini API.
     */
    protected Task<JSONObject> doRequestWithBodyAndJsonResponse(final String url, int method, final JSONObject body, final Session session) {
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonObjectRequest documentsRequest =
                new BearerJsonObjectRequest(method, url, body, checkNotNull(session),
                        mGiniApiType, completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy(), mGiniApiType.getGiniJsonMediaType());
        mRequestQueue.add(documentsRequest);
        return completionSource.getTask();
    }

    /**
     * Helper method to do a request that returns JSON data. The request is wrapped in a Task that will resolve to a
     * JSONObject.
     *
     * @param url       The full URL of the request.
     * @param method    The HTTP method of the request.
     * @param session   A valid session for the Gini API.
     * @return          A Task which will resolve to a JSONObject representing the response of the Gini API.
     */
    protected Task<JSONObject> doRequestWithJsonResponse(final String url, int method, final Session session) {
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonObjectRequest documentsRequest =
                new BearerJsonObjectRequest(method, url, null, checkNotNull(session),
                        mGiniApiType, completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(documentsRequest);
        return completionSource.getTask();
    }

    /**
     * Helper method to do a request that returns byte data. The request is wrapped in a Task that will resolve to a
     * byte[].
     *
     * @param url       The full URL of the request.
     * @param method    The HTTP method of the request.
     * @param session   A valid session for the Gini API.
     * @return          A Task which will resolve to a byte[] representing the response of the Gini API.
     */
    private Task<byte[]> doRequestWithByteArrayResponse(final String url, int method, final Session session) {
        final RequestTaskCompletionSource<byte[]> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerByteArrayRequest documentsRequest =
                new BearerByteArrayRequest(method, url, checkNotNull(session), completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(documentsRequest);
        return completionSource.getTask();
    }

    /**
     * Helper method to do a request that returns JSON data. The request is wrapped in a Task that will resolve to a
     * JSONArray.
     *
     * @param url       The full URL of the request.
     * @param method    The HTTP method of the request.
     * @param session   A valid session for the Gini API.
     * @return          A Task which will resolve to a JSONObject representing the response of the Gini API.
     */
    protected Task<JSONArray> doRequestWithJsonArrayResponse(final String url, int method, final Session session) {
        final RequestTaskCompletionSource<JSONArray> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final BearerJsonArrayRequest documentsRequest =
                new BearerJsonArrayRequest(method, url, null, checkNotNull(session),
                        mGiniApiType, completionSource, completionSource, mRetryPolicyFactory.newRetryPolicy());
        mRequestQueue.add(documentsRequest);
        return completionSource.getTask();
    }

    private Uri uriRelativeToBaseUri(Uri uri) {

        return mBaseUri.buildUpon().path(uri.getPath()).query(uri.getQuery()).build();
    }

    public enum PreviewSize {
        /** Medium sized image, maximum dimensions are 750x900. */
        MEDIUM("750x900"),
        /** Big image, maximum dimensions are 1280x1810 */
        BIG("1280x1810");

        private final String mDimensions;

        PreviewSize(final String dimensions) {
            mDimensions = dimensions;
        }

        public String getDimensions() {
            return mDimensions;
        }
    }
}
