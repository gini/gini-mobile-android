package net.gini.android;


import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.android.volley.Request.Method.DELETE;
import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static com.android.volley.Request.Method.PUT;

import static net.gini.android.helpers.TestUtils.areEqualURIs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;

import android.net.Uri;
import androidx.test.filters.MediumTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import net.gini.android.authorization.Session;
import net.gini.android.requests.DefaultRetryPolicyFactory;
import net.gini.android.requests.RetryPolicyFactory;

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
public class ApiCommunicatorTest {

    private ApiCommunicator mApiCommunicator;
    private RequestQueue mRequestQueue;
    private RetryPolicyFactory retryPolicyFactory;

    @Before
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getApplicationContext().getCacheDir().getPath());
        retryPolicyFactory = new DefaultRetryPolicyFactory();
        mRequestQueue = Mockito.mock(RequestQueue.class);
        mApiCommunicator = new ApiCommunicator("https://pay-api.gini.net/", GiniApiType.DEFAULT, mRequestQueue, retryPolicyFactory);
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
    public void testConstructionThrowsNullPointerExceptionWithNullArguments() {
        try {
            new ApiCommunicator(null, null, null, retryPolicyFactory);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            new ApiCommunicator("https://pay-api.gini.net", GiniApiType.DEFAULT, null, retryPolicyFactory);
            fail("NullPointerException not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testUploadDocumentThrowsWithNullArguments() {
        try {
            mApiCommunicator.uploadDocument(null, null, null, null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.uploadDocument(null, "image/jpeg", null, null, createSession(), null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.uploadDocument(createUploadData(), null, null, null, createSession(), null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.uploadDocument(createUploadData(), MediaTypes.IMAGE_JPEG, null, null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

    }

    @Test
    public void testUploadDocumentReturnsTask() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();
        assertNotNull(mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session, null));
    }

    @Test
    public void testUploadDocumentWithNameReturnsTask() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        assertNotNull(mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, "foobar.jpg", session, null));
    }

    @Test
    public void testUploadDocumentHasCorrectAccessToken() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession("1234-5678");

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session, null);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        Map headers = requestCaptor.getValue().getHeaders();
        assertEquals("BEARER 1234-5678", headers.get("Authorization"));
    }

    @Test
    public void testUploadDocumentHasCorrectContentType() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session, null);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        assertEquals(MediaTypes.IMAGE_JPEG, requestCaptor.getValue().getBodyContentType());
    }

    @Test
    public void testUploadDocumentHasCorrectAcceptHeader() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session, null);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        Map headers = requestCaptor.getValue().getHeaders();
        final String acceptHeader = (String) headers.get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V2));
    }

    @Test
    public void testUploadDocumentHasCorrectBody() throws AuthFailureError {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session, null);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals(documentData, request.getBody());
    }

    @Test
    public void testUploadDocumentHasCorrectUrlAndMethod() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session, null);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://pay-api.gini.net/documents/", request.getUrl());
        assertEquals(POST, request.getMethod());
    }

    @Test
    public void testUploadDocumentSubmitsFilename() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, "foobar.jpg", null, session, null);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://pay-api.gini.net/documents/?filename=foobar.jpg", request.getUrl());
    }

    @Test
    public void testUploadDocumentSubmitsDoctypeHint() {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, "invoice", session, null);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://pay-api.gini.net/documents/?doctype=invoice", request.getUrl());
    }

    @Test
    public void testDeleteDocumentsReturnsTask() {
        final Session session = createSession();

        assertNotNull(mApiCommunicator.deleteDocument("1234", session));
    }

    @Test
    public void testDeleteDocumentDeletesDocument() {
        final Session session = createSession();

        mApiCommunicator.deleteDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals(DELETE, request.getMethod());
    }

    @Test
    public void testDeleteDocumentDeletesTheCorrectDocument() {
        final Session session = createSession();

        mApiCommunicator.deleteDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents/1234", request.getUrl());
    }

    @Test
    public void testDeleteDocumentUsesTheCorrectSession() throws AuthFailureError {
        final Session session = createSession("4321-1234");

        mApiCommunicator.deleteDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("Bearer 4321-1234", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testDeleteDocumentThrowsWithWrongArguments() {
        try {
            mApiCommunicator.deleteDocument((String) null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.deleteDocument("1234", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.deleteDocument((String) null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testGetDocumentThrowsWithNullArguments() {
        try {
            final String documentId = null;
            mApiCommunicator.getDocument(documentId, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            final Uri documentUri = null;
            mApiCommunicator.getDocument(documentUri, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }


        try {
            mApiCommunicator.getDocument("1234", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            final String documentId = null;
            mApiCommunicator.getDocument(documentId, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            final String documentUri = null;
            mApiCommunicator.getDocument(documentUri, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

    }

    @Test
    public void testGetDocumentReturnsTask() {
        Session session = createSession();

        assertNotNull(mApiCommunicator.getDocument("1234", session));
    }

    @Test
    public void testGetDocumentGetsCorrectDocument() {
        Session session = createSession();

        mApiCommunicator.getDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents/1234", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    @Test
    public void testGetDocumentSendsCorrectAuthorizationHeaders() throws AuthFailureError {
        Session session = createSession("4321-1234");

        mApiCommunicator.getDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 4321-1234", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testGetDocumentSendsCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession();

        mApiCommunicator.getDocument("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_V2));
    }

    @Test
    public void testGetExtractionsThrowsWithNullArguments() {
        try {
            mApiCommunicator.getExtractions(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions("1234-4321", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions(null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetExtractionsGetsTheCorrectDocument() {
        Session session = createSession();

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents/1234/extractions", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    @Test
    public void testGetExtractionsHasCorrectAuthorizationHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 1234-1234", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testGetExtractionsHasCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_V2));
    }

    @Test
    public void testGetIncubatorExtractionsThrowsWithNullArguments() {
        try {
            mApiCommunicator.getExtractions(null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions("1234-4321", null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.getExtractions(null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetIncubatorExtractionsGetsTheCorrectDocument() {
        Session session = createSession();

        mApiCommunicator.getExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents/1234/extractions", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    @Test
    public void testGetIncubatorExtractionsHasCorrectAuthorizationHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getIncubatorExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 1234-1234", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testGetIncubatorExtractionsHasCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession("1234-1234");

        mApiCommunicator.getIncubatorExtractions("1234", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_INCUBATOR));
    }

    @Test
    public void testErrorReportForDocumentsThrowsWithNullArguments() {
        try {
            mApiCommunicator.errorReportForDocument(null, null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.errorReportForDocument("1234", null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.errorReportForDocument(null, null, null, createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mApiCommunicator.errorReportForDocument(null, "foobar", "foobar", createSession());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testErrorReportForDocumentHasCorrectUrl() throws URISyntaxException {
        Session session = createSession();

        mApiCommunicator.errorReportForDocument("1234", "short summary", "and a description", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(areEqualURIs("https://pay-api.gini.net/documents/1234/errorreport?description=and%20a%20description&summary=short%20summary",
                request.getUrl()));
    }

    @Test
    public void testErrorReportForDocumentHasCorrectAuthorizationHeader() throws AuthFailureError {
        Session session = createSession("4444-2222");

        mApiCommunicator.errorReportForDocument("1234", "short summary", "and a description", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 4444-2222", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testErrorReportHasCorrectAcceptHeader() throws AuthFailureError {
        Session session = createSession("4444-2222");

        mApiCommunicator.errorReportForDocument("1234", "short summary", "and a description", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertTrue(((String) request.getHeaders().get("Accept")).contains(MediaTypes.GINI_JSON_V2));
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
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V2));
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
    public void testGetLayoutHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.getLayoutForDocument("1234-4321", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents/1234-4321/layout", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    @Test
    public void testGetLayoutHasCorrectAcceptHeader() throws AuthFailureError {
        final Session session = createSession();

        mApiCommunicator.getLayoutForDocument("1234-4321", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V2));
    }

    @Test
    public void testGetLayoutHasCorrectAuthorizationHeader() throws AuthFailureError {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.getLayoutForDocument("1234-4321", session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testGetDocumentListHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.getDocumentList(0, 23, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/documents?offset=0&limit=23", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    @Test
    public void testGetDocumentListHasCorrectAcceptHeader() throws AuthFailureError {
        final Session session = createSession();

        mApiCommunicator.getDocumentList(0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V2));
    }

    @Test
    public void testGetDocumentListHasCorrectAuthorizationHeader() throws AuthFailureError {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.getDocumentList(0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testSearchDocumentsHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.searchDocuments("foo bär", null, 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/search?q=foo%20b%C3%A4r&offset=0&limit=20", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    @Test
    public void testSearchDocumentsWithDoctypeHasCorrectUrl() {
        final Session session = createSession();

        mApiCommunicator.searchDocuments("foo bär", "invoice", 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("https://pay-api.gini.net/search?q=foo%20b%C3%A4r&offset=0&limit=20&docType=invoice", request.getUrl());
        assertEquals(GET, request.getMethod());
    }

    @Test
    public void testSearchDocumentsHasCorrectAcceptHeader() throws AuthFailureError {
        final Session session = createSession();

        mApiCommunicator.searchDocuments("foobar", null, 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String acceptHeader = (String) request.getHeaders().get("Accept");
        assertTrue(acceptHeader.contains(MediaTypes.GINI_JSON_V2));
    }

    @Test
    public void testSearchDocumentsHasCorrectAuthorizationHeader() throws AuthFailureError {
        final Session session = createSession("9999-8888-7777");

        mApiCommunicator.searchDocuments("foobar", null, 0, 20, session);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        assertEquals("BEARER 9999-8888-7777", request.getHeaders().get("Authorization"));
    }

    @Test
    public void testDocumentMetadataIsAddedToTheRequestHeaders() throws Exception {
        final byte[] documentData = createUploadData();
        final Session session = createSession();

        final DocumentMetadata metadata = new DocumentMetadata();
        final String branchId = "4321";
        metadata.setBranchId(branchId);
        final String customMetadataName = "Test";
        final String customMetadataValue = "Unit";
        metadata.add(customMetadataName, customMetadataValue);

        mApiCommunicator.uploadDocument(documentData, MediaTypes.IMAGE_JPEG, null, null, session, metadata);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertTrue(request.getHeaders().containsKey(DocumentMetadata.BRANCH_ID_HEADER_FIELD_NAME));
        String value = (String) request.getHeaders().get(DocumentMetadata.BRANCH_ID_HEADER_FIELD_NAME);
        assertEquals(value, branchId);

        assertTrue(request.getHeaders().containsKey(DocumentMetadata.HEADER_FIELD_NAME_PREFIX + customMetadataName));
        value = (String) request.getHeaders().get(DocumentMetadata.HEADER_FIELD_NAME_PREFIX + customMetadataName);
        assertEquals(value, customMetadataValue);
    }

    @Test
    public void testGetApiUrl() {
        mApiCommunicator.getPageImage("aa9a4630-8e05-11eb-ad19-3bfb1a96d239", 2, createSession());

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mRequestQueue).add(requestCaptor.capture());
        final Request request = requestCaptor.getValue();

        assertEquals("https://pay-api.gini.net/documents/aa9a4630-8e05-11eb-ad19-3bfb1a96d239/pages/2/large", request.getUrl());
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
