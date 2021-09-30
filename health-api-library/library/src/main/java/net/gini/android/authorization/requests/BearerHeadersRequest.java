package net.gini.android.authorization.requests;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import net.gini.android.GiniApiType;
import net.gini.android.MediaTypes;
import net.gini.android.authorization.Session;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BearerHeadersRequest extends JsonObjectRequest {
    final private Session mSession;
    final private String contentType;
    private final GiniApiType mGiniApiType;

    public BearerHeadersRequest(int method, String url, JSONObject jsonRequest, Session session, @NonNull final GiniApiType giniApiType,
                                Response.Listener<JSONObject> listener, Response.ErrorListener errorListener, RetryPolicy retryPolicy) {
        this(method, url, jsonRequest, session, giniApiType, listener, errorListener, retryPolicy, null);
    }

    public BearerHeadersRequest(int method, String url, JSONObject jsonRequest,
                                Session session, @NonNull final GiniApiType giniApiType,
                                Response.Listener<JSONObject> listener, Response.ErrorListener errorListener,
                                RetryPolicy retryPolicy, @Nullable String contentType) {
        super(method, url, jsonRequest, listener, errorListener);
        setRetryPolicy(retryPolicy);
        mSession = session;
        this.contentType = contentType == null ? super.getBodyContentType() : contentType;
        mGiniApiType = giniApiType;
    }

    @Override
    public String getBodyContentType() {
        return contentType;
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", String.format("%s, %s", MediaTypes.APPLICATION_JSON, mGiniApiType.getGiniJsonMediaType()));
        headers.put("Authorization", "BEARER " + mSession.getAccessToken());
        return headers;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            final Map<String, String> headersMap = new HashMap<>();
            for (Map.Entry<String, String> entry : Objects.requireNonNull(response.headers).entrySet()) {
                headersMap.put(entry.getKey(), entry.getValue().toLowerCase());
            }
            final JSONObject jsonObject = new JSONObject(headersMap);
            return Response.success(jsonObject, HttpHeaderParser.parseCacheHeaders(response));
        } catch (NullPointerException npe) {
            return Response.error(new ParseError(npe));
        }
    }
}
