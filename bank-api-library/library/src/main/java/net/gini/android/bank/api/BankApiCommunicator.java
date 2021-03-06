package net.gini.android.bank.api;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static net.gini.android.core.api.Utils.checkNotNull;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.MediaTypes;
import net.gini.android.core.api.RequestTaskCompletionSource;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.requests.BearerJsonObjectRequest;
import net.gini.android.core.api.requests.RetryPolicyFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import bolts.Task;

/**
 * Created by Alpár Szotyori on 27.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */
public class BankApiCommunicator extends ApiCommunicator {

    public BankApiCommunicator(String baseUriString, GiniApiType giniApiType, RequestQueue mRequestQueue, RetryPolicyFactory retryPolicyFactory) {
        super(baseUriString, giniApiType, mRequestQueue, retryPolicyFactory);
    }

    public Task<JSONObject> sendFeedback(final String documentId, final JSONObject extractions, final Session session)
            throws JSONException {
        final String url = getBaseUri().buildUpon().path(String.format("documents/%s/extractions/feedback",
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
        final String url = getBaseUri().buildUpon().path(String.format("documents/%s/extractions/feedback",
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

    public Task<Bitmap> getPreview(final String documentId, final int pageNumber,
                                   PreviewSize previewSize, final Session session) {
        final String url = getBaseUri().buildUpon().path(String.format("documents/%s/pages/%s/%s",
                checkNotNull(documentId), pageNumber,
                previewSize.getDimensions())).toString();
        final String accessToken = checkNotNull(session).getAccessToken();
        RequestTaskCompletionSource<Bitmap> completionSource = RequestTaskCompletionSource.newCompletionSource();
        final ImageRequest imageRequest = new ImageRequest(url, completionSource, 0, 0, ARGB_8888, completionSource) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "BEARER " + accessToken);
                headers.put("Accept", MediaTypes.IMAGE_JPEG);
                return headers;
            }
        };
        imageRequest.setRetryPolicy(getRetryPolicyFactory().newRetryPolicy());
        getRequestQueue().add(imageRequest);

        return completionSource.getTask();
    }

    public Task<JSONObject> resolvePaymentRequests(final String id, final JSONObject body, final Session session) {
        final String url = getBaseUri().buildUpon().path("/paymentRequests/").appendPath(id).appendPath("payment")
                .toString();

        return doRequestWithBodyAndJsonResponse(url, POST, body, checkNotNull(session));
    }

    public Task<JSONObject> getPayment(final String id, final Session session) {
        final String url = getBaseUri().buildUpon().path("/paymentRequests/").appendPath(id).appendPath("payment")
                .toString();

        return doRequestWithJsonResponse(url, GET, checkNotNull(session));
    }

    public Task<JSONObject> logErrorEvent(@NonNull final JSONObject errorEvent, @NonNull final Session session) {
        final String url = getBaseUri().buildUpon().appendPath("events").appendPath("error").toString();
        return doRequestWithBodyAndJsonResponse(url, POST, errorEvent, session);
    }
}
