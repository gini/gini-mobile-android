package net.gini.android.core.api.authorization.requests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;

import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.MediaTypes;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.test.TestGiniApiType;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class BearerJsonObjectRequestTest {

    private RetryPolicy retryPolicy;

    @Before
    public void setUp() throws Exception {
        retryPolicy = new DefaultRetryPolicy();
    }

    @Test
    public void testAcceptHeader() throws AuthFailureError {
        Session session = new Session("1234-5678-9012", new Date());
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com",
                null, session, new TestGiniApiType(), null, null, retryPolicy);

        Map<String, String> headers = request.getHeaders();
        final String acceptHeader = request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains("application/vnd.gini.v"));
    }

    @Test
    public void testContentTypeHeader() throws AuthFailureError, JSONException {
        Session session = new Session("1234-5678-9012", new Date());
        JSONObject payload = new JSONObject();
        payload.put("foo", "bar");
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com",
                payload, session, new TestGiniApiType(), null, null, retryPolicy);

        assertEquals("application/json; charset=utf-8", request.getBodyContentType());
    }

    @Test
    public void testCustomContentTypeHeader() throws AuthFailureError, JSONException {
        Session session = new Session("1234-5678-9012", new Date());
        JSONObject payload = new JSONObject();
        payload.put("foo", "bar");
        BearerJsonObjectRequest request = new BearerJsonObjectRequest(Request.Method.GET, "https://example.com", payload, session,
                new TestGiniApiType(), null, null, retryPolicy, "application/test.vnd.v42");

        assertEquals("application/test.vnd.v42", request.getBodyContentType());
    }
}
