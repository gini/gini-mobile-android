package net.gini.android.authorization.requests;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;

import net.gini.android.GiniApiType;
import net.gini.android.MediaTypes;
import net.gini.android.Utils;
import net.gini.android.authorization.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BearerJsonObjectRequest extends JsonObjectRequest {
    final private Session mSession;
    final private String contentType;
    private final GiniApiType mGiniApiType;

    public BearerJsonObjectRequest(int method, String url, JSONObject jsonRequest, Session session, @NonNull final GiniApiType giniApiType,
            Response.Listener<JSONObject> listener, Response.ErrorListener errorListener, RetryPolicy retryPolicy) {
        this(method, url, jsonRequest, session, giniApiType, listener, errorListener, retryPolicy, null);
    }

    public BearerJsonObjectRequest(int method, String url, JSONObject jsonRequest,
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
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", String.format("%s, %s", MediaTypes.APPLICATION_JSON, mGiniApiType.getGiniJsonMediaType()));
        headers.put("Authorization", "BEARER " + mSession.getAccessToken());
        return headers;
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            final JSONObject jsonObject = createJSONObject(response);
            return Response.success(jsonObject,
                                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    private JSONObject createJSONObject(NetworkResponse response) throws UnsupportedEncodingException, JSONException {
        // The Gini API always uses UTF-8.
        final String jsonString = new String(response.data, Utils.CHARSET_UTF8);
        if (jsonString.length() > 0) {
            return new JSONObject(jsonString);
        } else {
            return null;
        }
    }
}
