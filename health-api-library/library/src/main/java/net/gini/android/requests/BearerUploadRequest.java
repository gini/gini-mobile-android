package net.gini.android.requests;

import android.net.Uri;

import com.android.volley.Response;
import com.android.volley.RetryPolicy;

import net.gini.android.GiniApiType;
import net.gini.android.MediaTypes;
import net.gini.android.authorization.Session;

import java.util.HashMap;
import java.util.Map;


public class BearerUploadRequest extends BearerLocationRequest{
    private final byte[] mUploadData;
    private final String mContentType;
    private final String mAccessToken;
    private final Map<String, String> mHeaders;
    private final GiniApiType mGiniApiType;

    public BearerUploadRequest(int method, String url, byte[] uploadData, String contentType,
            final Session session,
            final GiniApiType giniApiType,
            Response.Listener<Uri> listener,
            Response.ErrorListener errorListener,
            RetryPolicy retryPolicy,
            final Map<String, String> headers) {
        super(method, url, null, session, listener, errorListener, retryPolicy);
        mUploadData = uploadData;
        mContentType = contentType;
        mAccessToken = session.getAccessToken();
        mGiniApiType = giniApiType;
        mHeaders = headers;
    }

    @Override
    public byte[] getBody() {
        return mUploadData;
    }

    @Override
    public Map<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>(mHeaders);
        headers.put("Accept", String.format("%s, %s", MediaTypes.APPLICATION_JSON, mGiniApiType.getGiniJsonMediaType()));
        headers.put("Authorization", "BEARER " + mAccessToken);
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return mContentType;
    }
}
