package net.gini.android.health.api;


import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.android.volley.Request.Method.DELETE;
import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static net.gini.android.core.api.test.shared.helpers.TestUtils.areEqualURIs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.DocumentMetadata;
import net.gini.android.core.api.MediaTypes;
import net.gini.android.core.api.Utils;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.requests.DefaultRetryPolicyFactory;
import net.gini.android.core.api.requests.RetryPolicyFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class HealthApiCommunicatorTest {

    private HealthApiCommunicator mApiCommunicator;
    private RequestQueue mRequestQueue;
    private RetryPolicyFactory retryPolicyFactory;

    @Before
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getApplicationContext().getCacheDir().getPath());
        retryPolicyFactory = new DefaultRetryPolicyFactory();
        mRequestQueue = Mockito.mock(RequestQueue.class);
        mApiCommunicator = new HealthApiCommunicator("https://health-api.gini.net/", new GiniHealthApiType(3), mRequestQueue, retryPolicyFactory);
    }

    public byte[] createUploadData() {
        return "foobar".getBytes(Utils.CHARSET_UTF8);
    }

    public Session createSession(final String accessToken) {
        return new Session(accessToken, new Date());
    }

    public Session createSession() {
        return createSession("1234-5678-9012");
    }

    @Test
    public void testSendFeedbackThrowsExceptionWithNullArguments() throws JSONException {
        try {
            mApiCommunicator.sendFeedback(null, null, null, null);
            fail("Exception not raised");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.sendFeedback("1234-1234", new JSONObject(), new JSONObject(), null);
            fail("Exception not raised");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.sendFeedback("1234-1234", null, null, createSession());
            fail("Exception not raised");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.sendFeedback(null, new JSONObject(), new JSONObject(), createSession());
            fail("Exception not raised");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testSendFeedbackUpdatesCorrectDocument() throws JSONException {
        Session session = createSession();

        mApiCommunicator.sendFeedback("1234-1234", new JSONObject(), new JSONObject(), session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://health-api.gini.net/documents/1234-1234/extractions", request.getUrl());
        assertEquals(POST, request.getMethod());
    }

    @Test
    public void testSendFeedbackSendsCorrectData() throws JSONException, AuthFailureError {
        Session session = createSession();
        JSONObject extractions = new JSONObject();
        JSONObject value = new JSONObject();
        extractions.put("amountToPay", value);
        value.put("value", "32:EUR");

        JSONObject compoundExtractions = new JSONObject();
        JSONArray lineItems = new JSONArray();
        JSONObject lineValue = new JSONObject();
        lineValue.put("value", "10101");
        lineItems.put(lineValue);
        compoundExtractions.put("lineItems", lineItems);

        mApiCommunicator.sendFeedback("1234-1234", extractions, compoundExtractions, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("{\"extractions\":{\"amountToPay\":{\"value\":\"32:EUR\"}},\"compoundExtractions\":{\"lineItems\":[{\"value\":\"10101\"}]}}", new String(request.getBody()));
    }

    @Test
    public void testSendFeedbackHasCorrectAuthorizationHeader() throws AuthFailureError, JSONException {
        Session session = createSession("9999-8888-7777");

        mApiCommunicator.sendFeedback("1234", new JSONObject(), new JSONObject(), session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testSendFeedbackHasCorrectContentType() throws AuthFailureError, JSONException {
        Session session = createSession("9999-8888-7777");

        mApiCommunicator.sendFeedback("1234", new JSONObject(), new JSONObject(), session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains("application/vnd.gini.v"));
    }

    @Test
    public void testGetPages() {
        mApiCommunicator.getPages("aa9a4630-8e05-11eb-ad19-3bfb1a96d239", createSession());

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://health-api.gini.net/documents/aa9a4630-8e05-11eb-ad19-3bfb1a96d239/pages", request.getUrl());
    }

}
