package net.gini.android.helpers;

import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;

import net.gini.android.Utils;

import java.util.HashMap;
import java.util.Map;


/** TODO */
public class MockNetwork implements Network {

    public NetworkResponse mResponseToReturn;
    public Request<?> mLastRequest;

    @Override
    public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        mLastRequest = request;
        return mResponseToReturn;
    }

    public void setResponseToReturn(NetworkResponse response) {
        mResponseToReturn = response;
    }

    // TODO
    public static NetworkResponse createResponse(int statusCode, String data, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        return new NetworkResponse(statusCode, data.getBytes(Utils.CHARSET_UTF8), headers, false);
    }
}
