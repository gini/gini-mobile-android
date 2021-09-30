package net.gini.android.authorization.requests;


import static net.gini.android.helpers.TestUtils.areEqualURIQueries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.RequestFuture;

import net.gini.android.MediaTypes;
import net.gini.android.helpers.MockNetwork;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


@RunWith(AndroidJUnit4.class)
public class TokenRequestTest {

    public RequestFuture<JSONObject> requestFuture;
    public TokenRequest loginRequest;
    public RequestQueue requestQueue;
    public MockNetwork mockNetwork;
    private RetryPolicy retryPolicy;

    @Before
    public void setUp() {
        retryPolicy = new DefaultRetryPolicy();
        requestFuture = RequestFuture.newFuture();
        loginRequest = new TokenRequest("foobar", "1234", "https://user.gini.net/oauth/token?grant_type=client_credentials", null,
                requestFuture, requestFuture, retryPolicy);
        mockNetwork = new MockNetwork();
        requestQueue = new RequestQueue(new NoCache(), mockNetwork);
        requestQueue.start();
    }

    @Test
    public void testJSONAcceptHeader() throws AuthFailureError {
        Map<String, String> headers = loginRequest.getHeaders();

        assertEquals(MediaTypes.APPLICATION_JSON, headers.get("Accept"));
    }

    @Test
    public void testAuthorizationHeader() throws AuthFailureError {
        Map<String, String> headers = loginRequest.getHeaders();

        assertEquals("Basic Zm9vYmFyOjEyMzQ=", headers.get("Authorization"));
    }

    @Test
    public void testCorrectURL() {
        assertEquals("https://user.gini.net/oauth/token?grant_type=client_credentials", loginRequest.getUrl());
    }

    @Test
    public void testEmptyBody() {
        assertNull(loginRequest.getBody());
    }

    @Test
    public void testBody() {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("foo", "bar");
        data.put("bar", "foo");
        loginRequest = new TokenRequest("foobar", "1234", "https://user.gini.net", data, null, null, retryPolicy);

        assertTrue(areEqualURIQueries("foo=bar&bar=foo", new String(loginRequest.getBody())));
    }

    @Test
    public void testBodyContentTypeHeader() throws AuthFailureError {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("foo", "bar");
        data.put("bar", "foo");
        loginRequest = new TokenRequest("foobar", "1234", "https://user.gini.net", data, null, null, retryPolicy);

        assertEquals(MediaTypes.APPLICATION_FORM_URLENCODED, loginRequest.getBodyContentType());
    }

    @Test
    public void testSuccessfulResponse() throws ExecutionException, InterruptedException, JSONException {
        NetworkResponse response = MockNetwork.createResponse(200,
                "{\"access_token\":\"74c1e7fe-e464-451f-a6eb-8f0998c46ff6\",\"token_type\":\"bearer\",\"expires_in\":3599}", null);
        mockNetwork.setResponseToReturn(response);

        requestQueue.add(loginRequest);

        JSONObject responseData = requestFuture.get();
        assertEquals(responseData.getInt("expires_in"), 3599);
        assertEquals(responseData.getString("access_token"), "74c1e7fe-e464-451f-a6eb-8f0998c46ff6");
        assertEquals(responseData.getString("token_type"), "bearer");
    }
}
