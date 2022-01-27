package net.gini.android.health.api;

import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static net.gini.android.core.api.Utils.checkNotNull;

import androidx.annotation.NonNull;

import com.android.volley.RequestQueue;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.RequestTaskCompletionSource;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.requests.BearerJsonObjectRequest;
import net.gini.android.core.api.requests.RetryPolicyFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import bolts.Task;

/**
 * Created by Alpár Szotyori on 25.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */
public class HealthApiCommunicator extends ApiCommunicator {

    public HealthApiCommunicator(String baseUriString, GiniApiType giniApiType, RequestQueue mRequestQueue, RetryPolicyFactory retryPolicyFactory) {
        super(baseUriString, giniApiType, mRequestQueue, retryPolicyFactory);
    }

    public Task<JSONObject> sendFeedback(final String documentId, final JSONObject extractions, final Session session)
            throws JSONException {
        final String url = getBaseUri().buildUpon().path(String.format("documents/%s/extractions",
                checkNotNull(documentId))).toString();
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final JSONObject requestData = new JSONObject();
        requestData.put("feedback", checkNotNull(extractions));
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(POST, url, requestData, checkNotNull(session),
                        getGiniApiType(), completionSource, completionSource,
                        getRetryPolicyFactory().newRetryPolicy(), getGiniApiType().getGiniJsonMediaType());
        getRequestQueue().add(request);

        return completionSource.getTask();
    }

    public Task<JSONObject> sendFeedback(final String documentId, final JSONObject extractions,
                                         final JSONObject compoundExtractions, final Session session)
            throws JSONException {
        final String url = getBaseUri().buildUpon().path(String.format("documents/%s/extractions",
                checkNotNull(documentId))).toString();
        final RequestTaskCompletionSource<JSONObject> completionSource =
                RequestTaskCompletionSource.newCompletionSource();
        final JSONObject requestData = new JSONObject();
        requestData.put("extractions", checkNotNull(extractions));
        requestData.put("compoundExtractions", checkNotNull(compoundExtractions));
        final BearerJsonObjectRequest request =
                new BearerJsonObjectRequest(POST, url, requestData, checkNotNull(session),
                        getGiniApiType(), completionSource, completionSource,
                        getRetryPolicyFactory().newRetryPolicy(), getGiniApiType().getGiniJsonMediaType());
        getRequestQueue().add(request);

        return completionSource.getTask();
    }

    public Task<JSONArray> getPages(@NonNull String documentId, @NonNull final Session session) {
        final String url =
                getBaseUri().buildUpon().path(String.format("/documents/%s/pages", checkNotNull(documentId))).toString();
        return doRequestWithJsonArrayResponse(url, GET, session);
    }

    public Task<JSONArray> getPaymentProviders(final Session session) {
        final String url = getBaseUri().buildUpon().path("/paymentProviders").toString();

        return doRequestWithJsonArrayResponse(url, GET, checkNotNull(session));
    }

    public Task<JSONObject> getPaymentProvider(final String id, final Session session) {
        final String url = getBaseUri().buildUpon().path("/paymentProviders/").appendPath(id).toString();

        return doRequestWithJsonResponse(url, GET, checkNotNull(session));
    }

    public Task<JSONObject> postPaymentRequests(final JSONObject body, final Session session) {
        final String url = getBaseUri().buildUpon().path("/paymentRequests")
                .toString();

        return doRequestWithHeadersResponse(url, POST, body, checkNotNull(session));
    }
}
