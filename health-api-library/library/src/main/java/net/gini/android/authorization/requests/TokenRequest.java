package net.gini.android.authorization.requests;


import static net.gini.android.Utils.mapToUrlEncodedString;

import androidx.annotation.Nullable;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;

import net.gini.android.MediaTypes;
import net.gini.android.Utils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Request to do a login request to the Gini User Center API in order to login the client.
 */
public class TokenRequest extends JsonObjectRequest {

    private final String mAuthorizationCredentials;
    private final Map<String, String> mRequestData;

    public TokenRequest(String clientId, String clientSecret, String url, @Nullable Map<String, String> requestData,
                        Response.Listener<JSONObject> listener, Response.ErrorListener errorListener, RetryPolicy retryPolicy) {
        super(Method.POST, url, null, listener, errorListener);

        mAuthorizationCredentials =
                Base64.encodeToString(String.format("%s:%s", clientId, clientSecret).getBytes(Utils.CHARSET_UTF8),
                                      Base64.NO_WRAP);
        setRetryPolicy(retryPolicy);
        mRequestData = requestData;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Basic " + mAuthorizationCredentials);
        headers.put("Accept", MediaTypes.APPLICATION_JSON);
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return MediaTypes.APPLICATION_FORM_URLENCODED;
    }

    @Override
    public byte[] getBody() {
        byte[] body = null;
        if (mRequestData != null) {
            body = mapToUrlEncodedString(mRequestData).getBytes(Utils.CHARSET_UTF8);
        }
        return body;
    }
}
