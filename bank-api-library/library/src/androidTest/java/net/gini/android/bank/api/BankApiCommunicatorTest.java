package net.gini.android.bank.api;


import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.android.volley.Request.Method.POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.MediaTypes;
import net.gini.android.core.api.Utils;
import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.requests.DefaultRetryPolicyFactory;
import net.gini.android.core.api.requests.RetryPolicyFactory;
import net.gini.android.health.api.GiniHealthApiType;
import net.gini.android.health.api.HealthApiCommunicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Date;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BankApiCommunicatorTest {

    private BankApiCommunicator mApiCommunicator;
    private RequestQueue mRequestQueue;
    private RetryPolicyFactory retryPolicyFactory;

    @Before
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getApplicationContext().getCacheDir().getPath());
        retryPolicyFactory = new DefaultRetryPolicyFactory();
        mRequestQueue = Mockito.mock(RequestQueue.class);
        mApiCommunicator = new BankApiCommunicator("https://pay-api.gini.net/", new GiniHealthApiType(3), mRequestQueue, retryPolicyFactory);
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
    public void testGetPreviewThrowsWithNullArguments() {
        try {
            mApiCommunicator.getPreview(null, 0, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getPreview("1234", 1, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.MEDIUM, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetPreviewHasCorrectUrlWithBigPreview() {
        Session session = createSession();

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.BIG, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents/1234/pages/1/1280x1810", request.getUrl());
    }

    @Test
    public void testGetPreviewHasCorrectUrlWithMediumPreview() {
        Session session = createSession();

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.MEDIUM, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents/1234/pages/1/750x900", request.getUrl());
    }

    @Test
    public void testGetPreviewHasCorrectAuthorizationHeader() throws AuthFailureError {
        Session session = createSession("9876-5432");

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.BIG, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9876-5432", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testGetPreviewHasCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession();

        mApiCommunicator.getPreview("1234", 1, ApiCommunicator.PreviewSize.MEDIUM, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals(MediaTypes.IMAGE_JPEG, request.getHeaders().get("Accept"));
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
        assertEquals("https://pay-api.gini.net/documents/1234-1234/extractions/feedback", request.getUrl());
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
    public void logErrorEventHasCorrectAuthorizationHeader() throws Exception {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.logErrorEvent(new JSONObject(), session);

        final ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    @Test
    public void logErrorEventHasCorrectUrl() throws Exception {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.logErrorEvent(new JSONObject(), session);

        final ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://pay-api.gini.net/events/error", request.getUrl());
    }

}
