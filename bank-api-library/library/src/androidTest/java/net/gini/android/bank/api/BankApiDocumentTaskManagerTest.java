//package net.gini.android.bank.api;
//
//import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import android.net.Uri;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.filters.MediumTest;
//
//import com.squareup.moshi.JsonAdapter;
//import com.squareup.moshi.Moshi;
//
//import net.gini.android.bank.api.models.ReturnReason;
//import net.gini.android.core.api.authorization.Session;
//import net.gini.android.core.api.authorization.SessionManager;
//import net.gini.android.core.api.models.CompoundExtraction;
//import net.gini.android.core.api.models.Document;
//import net.gini.android.core.api.models.Extraction;
//import net.gini.android.bank.api.models.Payment;
//import net.gini.android.bank.api.models.ResolvePaymentInput;
//import net.gini.android.bank.api.models.ResolvedPayment;
//import net.gini.android.bank.api.models.ExtractionsContainer;
//import net.gini.android.core.api.models.SpecificExtraction;
//import net.gini.android.bank.api.requests.ErrorEvent;
////import net.gini.android.health.api.GiniHealthApiType;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mockito;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import bolts.Task;
//
//// TODO: migrate to BankApiDocumentManagerTest
//@MediumTest
//@RunWith(AndroidJUnit4.class)
//public class BankApiDocumentTaskManagerTest {
//
//    private BankApiDocumentTaskManager mDocumentTaskManager;
//    private SessionManager mSessionManager;
//    private BankApiCommunicator mApiCommunicator;
//    private Session mSession;
//    private Moshi moshi;
//
//    @Before
//    public void setUp() {
//        // https://code.google.com/p/dexmaker/issues/detail?id=2
//        System.setProperty("dexmaker.dexcache", getApplicationContext().getCacheDir().getPath());
//
//        mApiCommunicator = Mockito.mock(BankApiCommunicator.class);
//        mSessionManager = Mockito.mock(SessionManager.class);
//        moshi = new Moshi.Builder().build();
//        mDocumentTaskManager = new BankApiDocumentTaskManager(mApiCommunicator, mSessionManager, new GiniHealthApiType(3), moshi);
//
//        // Always mock the session away since it is not what is tested here.
//        mSession = new Session("1234-5678-9012", new Date(new Date().getTime() + 10000));
//        when(mSessionManager.getSession()).thenReturn(Task.forResult(mSession));
//    }
//
//    private JSONObject readJSONFile(final String filename) throws IOException, JSONException {
//        InputStream inputStream = getApplicationContext().getResources().getAssets().open(filename);
//        int size = inputStream.available();
//        byte[] buffer = new byte[size];
//        @SuppressWarnings("unused")
//        int read = inputStream.read(buffer);
//        inputStream.close();
//        return new JSONObject(new String(buffer));
//    }
//
//    private JSONArray readJSONArrayFile(final String filename) throws IOException, JSONException {
//        InputStream inputStream = getApplicationContext().getResources().getAssets().open(filename);
//        int size = inputStream.available();
//        byte[] buffer = new byte[size];
//        @SuppressWarnings("unused")
//        int read = inputStream.read(buffer);
//        inputStream.close();
//        return new JSONArray(new String(buffer));
//    }
//
//    private Task<JSONObject> createResolvePaymentJsonTask() throws IOException, JSONException {
//        return Task.forResult(readJSONFile("resolved-payment.json"));
//    }
//
//    private Task<JSONObject> createPaymentJSONTask() throws IOException, JSONException {
//        return Task.forResult(readJSONFile("payment.json"));
//    }
//
//    private Task<JSONObject> createExtractionsJSONTask() throws IOException, JSONException {
//        return Task.forResult(readJSONFile("extractions.json"));
//    }
//
//    @Test
//    public void testGetExtractionsParsesReturnReasons() throws Exception {
//        when(mApiCommunicator.getExtractions(eq("1234"), any(Session.class))).thenReturn(createExtractionsJSONTask());
//        Document document = new Document("1234", Document.ProcessingState.COMPLETED, "foobar", 1, new Date(),
//                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
//                new ArrayList<Uri>());
//
//        Task<ExtractionsContainer> extractionsTask = mDocumentTaskManager.getAllExtractions(document);
//        extractionsTask.waitForCompletion();
//        if (extractionsTask.isFaulted()) {
//            throw extractionsTask.getError();
//        }
//        final ExtractionsContainer extractions = extractionsTask.getResult();
//        assertNotNull(extractions);
//
//        assertEquals(4, extractions.getReturnReasons().size());
//
//        final ReturnReason returnReason = extractions.getReturnReasons().get(0);
//
//        assertEquals("r1", returnReason.getId());
//        assertEquals("Anderes Aussehen als angeboten", returnReason.getLocalizedLabels().get("de"));
//    }
//
//    @Test
//    public void testSendFeedbackThrowsWithNullArguments() throws JSONException {
//        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
//                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
//                new ArrayList<Uri>());
//
//        try {
//            mDocumentTaskManager.sendFeedbackForExtractions(null, null, null);
//            fail("Exception not thrown");
//        } catch (NullPointerException ignored) {
//        }
//
//        try {
//            mDocumentTaskManager.sendFeedbackForExtractions(document, null, null);
//            fail("Exception not thrown");
//        } catch (NullPointerException ignored) {
//        }
//
//        try {
//            mDocumentTaskManager.sendFeedbackForExtractions(null, new HashMap<String, SpecificExtraction>(), new HashMap<String, CompoundExtraction>());
//            fail("Exception not thrown");
//        } catch (NullPointerException ignored) {
//        }
//    }
//
//    @Test
//    public void testSendFeedbackReturnsTask() throws JSONException {
//        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
//                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
//                new ArrayList<Uri>());
//        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
//        final HashMap<String, CompoundExtraction> compoundExtractions = new HashMap<>();
//
//        assertNotNull(mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, compoundExtractions));
//    }
//
//    @Test
//    public void testSendFeedbackResolvesToDocumentInstance() throws JSONException, InterruptedException {
//        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
//                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
//                new ArrayList<Uri>());
//        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
//        final HashMap<String, CompoundExtraction> compoundExtractions = new HashMap<>();
//
//        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(JSONObject.class), any(Session.class))).thenReturn(
//                Task.forResult(new JSONObject()));
//
//        Task<Document> updateTask = mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, compoundExtractions);
//        updateTask.waitForCompletion();
//        assertNotNull(updateTask.getResult());
//    }
//
//    @Test
//    public void testSendFeedbackSavesExtractions() throws JSONException, InterruptedException {
//        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
//                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
//                new ArrayList<Uri>());
//        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
//        extractions.put("amountToPay",
//                new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
//        extractions.put("senderName",
//                new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));
//
//        extractions.get("amountToPay").setValue("23:EUR");
//        mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, new HashMap<String, CompoundExtraction>()).waitForCompletion();
//
//        ArgumentCaptor<JSONObject> dataCaptor = ArgumentCaptor.forClass(JSONObject.class);
//        verify(mApiCommunicator).sendFeedback(eq("1234"), dataCaptor.capture(), any(JSONObject.class), any(Session.class));
//        final JSONObject updateData = dataCaptor.getValue();
//        // Should update the amountToPay
//        assertTrue(updateData.has("amountToPay"));
//        final JSONObject amountToPay = updateData.getJSONObject("amountToPay");
//        assertEquals("23:EUR", amountToPay.getString("value"));
//        assertTrue(updateData.has("senderName"));
//    }
//
//    @Test
//    public void testSendFeedbackMarksExtractionsAsNotDirty() throws JSONException, InterruptedException {
//        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(JSONObject.class), any(Session.class))).thenReturn(
//                Task.forResult(new JSONObject()));
//        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
//                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
//                new ArrayList<Uri>());
//        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
//        extractions.put("amountToPay",
//                new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
//        extractions.put("senderName",
//                new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));
//
//        extractions.get("amountToPay").setValue("23:EUR");
//
//        final HashMap<String, CompoundExtraction> compoundExtractions = new HashMap<>();
//        Task<Document> updateTask = mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, compoundExtractions);
//
//        updateTask.waitForCompletion();
//        assertFalse(extractions.get("amountToPay").isDirty());
//    }
//
//    @Test
//    public void testResolvePaymentRequest() throws Exception {
//        when(mApiCommunicator.resolvePaymentRequests(any(String.class), any(JSONObject.class), any(Session.class)))
//                .thenReturn(createResolvePaymentJsonTask());
//
//        Task<ResolvedPayment> paymentRequestTask = mDocumentTaskManager.resolvePaymentRequest("", new ResolvePaymentInput("", "", "", "", null));
//        paymentRequestTask.waitForCompletion();
//        if (paymentRequestTask.isFaulted()) {
//            throw paymentRequestTask.getError();
//        }
//        ResolvedPayment resolvedPayment = new ResolvedPayment("ginipay-example://payment-requester", "Dr. med. Hackler", "DE02300209000106531065", "CMCIDEDDXXX", "335.50:EUR", "ReNr AZ356789Z", ResolvedPayment.Status.PAID);
//        assertEquals(resolvedPayment, paymentRequestTask.getResult());
//    }
//
//    @Test
//    public void testGetPayment() throws Exception {
//        when(mApiCommunicator.getPayment(any(String.class), any(Session.class))).thenReturn(createPaymentJSONTask());
//
//        Task<Payment> paymentRequestTask = mDocumentTaskManager.getPayment("");
//        paymentRequestTask.waitForCompletion();
//        if (paymentRequestTask.isFaulted()) {
//            throw paymentRequestTask.getError();
//        }
//        Payment payment = new Payment("2020-12-07T15:53:26", "Dr. med. Hackler", "DE02300209000106531065", "335.50:EUR", "ReNr AZ356789Z", "CMCIDEDDXXX");
//        assertEquals(payment, paymentRequestTask.getResult());
//    }
//
//    @Test
//    public void logErrorEvent() throws Exception {
//        when(mApiCommunicator.logErrorEvent(any(JSONObject.class), any(Session.class)))
//                .thenReturn(Task.forResult(new JSONObject()));
//
//        final ErrorEvent errorEvent = new ErrorEvent(
//                "sm-g950f", "Android", "12",
//                "1.2.0", "1.0.0", "Bad things happen"
//        );
//
//        final Task<Void> requestTask = mDocumentTaskManager.logErrorEvent(errorEvent);
//        requestTask.waitForCompletion();
//        if (requestTask.isFaulted()) {
//            throw requestTask.getError();
//        }
//
//        final ArgumentCaptor<JSONObject> requestBodyCaptor = ArgumentCaptor.forClass(JSONObject.class);
//        verify(mApiCommunicator).logErrorEvent(requestBodyCaptor.capture(), any(Session.class));
//        final JSONObject requestBody = requestBodyCaptor.getValue();
//
//        final JsonAdapter<ErrorEvent> adapter = moshi.adapter(ErrorEvent.class);
//        final ErrorEvent sentErrorEvent = adapter.fromJson(requestBody.toString());
//
//        assertEquals(errorEvent, sentErrorEvent);
//    }
//
//}
