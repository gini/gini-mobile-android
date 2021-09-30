package net.gini.android.requests;


import android.net.Uri;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import net.gini.android.authorization.Session;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BearerLocationRequest extends JsonRequest<Uri> {
    private final String mAccessToken;

    public BearerLocationRequest(int method, String url, JSONObject jsonRequest,
                                 Session session,
                                 Response.Listener<Uri> listener,
                                 Response.ErrorListener errorListener,
                                 RetryPolicy retryPolicy) {
        super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener, errorListener);
        mAccessToken = session.getAccessToken();
        setRetryPolicy(retryPolicy);
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "BEARER " + mAccessToken);
        return headers;
    }

    @Override
    protected Response<Uri> parseNetworkResponse(NetworkResponse response) {
        final String locationString = response.headers.get("Location");
        final Uri locationUri = Uri.parse(locationString);
        return Response.success(locationUri, HttpHeaderParser.parseCacheHeaders(response));
    }
}
