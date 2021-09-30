package net.gini.android.authorization.requests;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;

import net.gini.android.authorization.Session;

import java.util.HashMap;
import java.util.Map;

public class BearerByteArrayRequest extends Request<byte[]> {
    final private Session mSession;
    private final Response.Listener<byte[]> mListener;

    public BearerByteArrayRequest(int method, String url,
                                  Session session,
                                  Response.Listener<byte[]> listener, Response.ErrorListener errorListener,
                                  RetryPolicy retryPolicy) {
        super(method, url, errorListener);
        setRetryPolicy(retryPolicy);
        mSession = session;
        mListener = listener;
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "BEARER " + mSession.getAccessToken());
        return headers;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
        } catch (NullPointerException npe) {
            return Response.error(new ParseError(npe));
        }
    }

    @Override
    protected void deliverResponse(byte[] response) {
        mListener.onResponse(response);
    }
}
