package net.gini.android.health.api;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.util.Size;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.squareup.moshi.Moshi;

import net.gini.android.core.api.authorization.Session;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.models.CompoundExtraction;
import net.gini.android.core.api.models.Document;
import net.gini.android.core.api.models.Extraction;
import net.gini.android.core.api.models.Payment;
import net.gini.android.core.api.models.ResolvePaymentInput;
import net.gini.android.core.api.models.ResolvedPayment;
import net.gini.android.core.api.models.SpecificExtraction;
import net.gini.android.health.api.models.Page;
import net.gini.android.health.api.models.PaymentProvider;
import net.gini.android.health.api.models.PaymentRequestInput;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bolts.Task;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class HealthApiDocumentTaskManagerTest {

    private HealthApiDocumentTaskManager mDocumentTaskManager;
    private SessionManager mSessionManager;
    private HealthApiCommunicator mApiCommunicator;
    private Session mSession;
    private Moshi moshi;

    @Before
    public void setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", getApplicationContext().getCacheDir().getPath());

        mApiCommunicator = Mockito.mock(HealthApiCommunicator.class);
        mSessionManager = Mockito.mock(SessionManager.class);
        moshi = new Moshi.Builder().build();
        mDocumentTaskManager = new HealthApiDocumentTaskManager(mApiCommunicator, mSessionManager, new GiniHealthApiType(3), moshi);

        // Always mock the session away since it is not what is tested here.
        mSession = new Session("1234-5678-9012", new Date(new Date().getTime() + 10000));
        when(mSessionManager.getSession()).thenReturn(Task.forResult(mSession));
    }

    private JSONObject readJSONFile(final String filename) throws IOException, JSONException {
        InputStream inputStream = getApplicationContext().getResources().getAssets().open(filename);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        @SuppressWarnings("unused")
        int read = inputStream.read(buffer);
        inputStream.close();
        return new JSONObject(new String(buffer));
    }

    private JSONArray readJSONArrayFile(final String filename) throws IOException, JSONException {
        InputStream inputStream = getApplicationContext().getResources().getAssets().open(filename);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        @SuppressWarnings("unused")
        int read = inputStream.read(buffer);
        inputStream.close();
        return new JSONArray(new String(buffer));
    }

    private Task<JSONArray> createPagesJSONTask() throws IOException, JSONException {
        return Task.forResult(readJSONArrayFile("pages.json"));
    }

    private Task<JSONArray> createPaymentProvidersJSONTask() throws IOException, JSONException {
        return Task.forResult(readJSONArrayFile("payment-providers.json"));
    }

    private Task<JSONObject> createPaymentProviderJSONTask() throws IOException, JSONException {
        return Task.forResult(readJSONFile("payment-provider.json"));
    }

    private Task<JSONObject> createResolvePaymentJsonTask() throws IOException, JSONException {
        return Task.forResult(readJSONFile("resolved-payment.json"));
    }

    private Task<JSONObject> createLocationHeaderJSONTask(String url) {
        return Task.forResult(new JSONObject(Collections.singletonMap("location", url)));
    }

    private Task<JSONObject> createPaymentJSONTask() throws IOException, JSONException {
        return Task.forResult(readJSONFile("payment.json"));
    }

    @Test
    public void testSendFeedbackThrowsWithNullArguments() throws JSONException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());

        try {
            mDocumentTaskManager.sendFeedbackForExtractions(null, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mDocumentTaskManager.sendFeedbackForExtractions(document, null, null);
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }

        try {
            mDocumentTaskManager.sendFeedbackForExtractions(null, new HashMap<String, SpecificExtraction>(), new HashMap<String, CompoundExtraction>());
            fail("Exception not thrown");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testSendFeedbackReturnsTask() throws JSONException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        final HashMap<String, CompoundExtraction> compoundExtractions = new HashMap<>();

        assertNotNull(mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, compoundExtractions));
    }

    @Test
    public void testSendFeedbackResolvesToDocumentInstance() throws JSONException, InterruptedException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        final HashMap<String, CompoundExtraction> compoundExtractions = new HashMap<>();

        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(JSONObject.class), any(Session.class))).thenReturn(
                Task.forResult(new JSONObject()));

        Task<Document> updateTask = mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, compoundExtractions);
        updateTask.waitForCompletion();
        assertNotNull(updateTask.getResult());
    }

    @Test
    public void testSendFeedbackSavesExtractions() throws JSONException, InterruptedException {
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        extractions.put("amountToPay",
                new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
        extractions.put("senderName",
                new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));

        extractions.get("amountToPay").setValue("23:EUR");
        mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, new HashMap<String, CompoundExtraction>()).waitForCompletion();

        ArgumentCaptor<JSONObject> dataCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(mApiCommunicator).sendFeedback(eq("1234"), dataCaptor.capture(), any(JSONObject.class), any(Session.class));
        final JSONObject updateData = dataCaptor.getValue();
        // Should update the amountToPay
        assertTrue(updateData.has("amountToPay"));
        final JSONObject amountToPay = updateData.getJSONObject("amountToPay");
        assertEquals("23:EUR", amountToPay.getString("value"));
        assertTrue(updateData.has("senderName"));
    }

    @Test
    public void testSendFeedbackMarksExtractionsAsNotDirty() throws JSONException, InterruptedException {
        when(mApiCommunicator.sendFeedback(eq("1234"), any(JSONObject.class), any(JSONObject.class), any(Session.class))).thenReturn(
                Task.forResult(new JSONObject()));
        final Document document = new Document("1234", Document.ProcessingState.PENDING, "foobar.jpg", 1, new Date(),
                Document.SourceClassification.NATIVE, Uri.parse(""), new ArrayList<Uri>(),
                new ArrayList<Uri>());
        final HashMap<String, SpecificExtraction> extractions = new HashMap<String, SpecificExtraction>();
        extractions.put("amountToPay",
                new SpecificExtraction("amountToPay", "42:EUR", "amount", null, new ArrayList<Extraction>()));
        extractions.put("senderName",
                new SpecificExtraction("senderName", "blah", "senderName", null, new ArrayList<Extraction>()));

        extractions.get("amountToPay").setValue("23:EUR");

        final HashMap<String, CompoundExtraction> compoundExtractions = new HashMap<>();
        Task<Document> updateTask = mDocumentTaskManager.sendFeedbackForExtractions(document, extractions, compoundExtractions);

        updateTask.waitForCompletion();
        assertFalse(extractions.get("amountToPay").isDirty());
    }

    @Test
    public void testGetPageImage() throws Exception {
        final byte[] expectedBytes = new byte[] {1, 2};
        when(mApiCommunicator.getPages(eq("documentId"), any())).thenReturn(createPagesJSONTask());
        when(mApiCommunicator.getFile(eq("https://api.gini.net/documents/ba626ad0-7ec2-11ec-854d-b5b2580e2dc4/pages/1/1280x1810"), any(Session.class)))
                .thenReturn(Task.forResult(expectedBytes));
        when(mApiCommunicator.getBaseUri()).thenReturn(Uri.parse("https://api.gini.net"));

        Task<byte[]> pageImage = mDocumentTaskManager.getPageImage("documentId", 1);
        pageImage.waitForCompletion();
        if (pageImage.isFaulted()) {
            throw pageImage.getError();
        }

        assertEquals(expectedBytes, pageImage.getResult());
    }

    @Test
    public void testGetPageImageReturnsLargestImageSmallerThanMaxImageSize() throws Exception {
        final byte[] expectedBytes = new byte[] {1, 2};
        when(mApiCommunicator.getPages(eq("documentId"), any())).thenReturn(createPagesJSONTask());
        when(mApiCommunicator.getFile(eq("https://api.gini.net/documents/ba626ad0-7ec2-11ec-854d-b5b2580e2dc4/pages/2/1280x1810"), any(Session.class)))
                .thenReturn(Task.forResult(expectedBytes));
        when(mApiCommunicator.getBaseUri()).thenReturn(Uri.parse("https://api.gini.net"));

        Task<byte[]> pageImage = mDocumentTaskManager.getPageImage("documentId", 2);
        pageImage.waitForCompletion();
        if (pageImage.isFaulted()) {
            throw pageImage.getError();
        }

        assertEquals(expectedBytes, pageImage.getResult());
    }

    @Test
    public void testGetPaymentProviders() throws Exception {
        when(mApiCommunicator.getPaymentProviders(any(Session.class))).thenReturn(createPaymentProvidersJSONTask());
        when(mApiCommunicator.getFile(any(String.class), any(Session.class))).thenReturn(Task.forResult(new byte[0]));

        Task<List<PaymentProvider>> paymentProvidersTask = mDocumentTaskManager.getPaymentProviders();
        paymentProvidersTask.waitForCompletion();
        if (paymentProvidersTask.isFaulted()) {
            throw paymentProvidersTask.getError();
        }
        final List<PaymentProvider> paymentProvidersResult = paymentProvidersTask.getResult();
        assertEquals(getPaymentProviders(), paymentProvidersResult);
    }

    @Test
    public void testGetPaymentProvider() throws Exception {
        when(mApiCommunicator.getPaymentProvider(any(String.class), any(Session.class))).thenReturn(createPaymentProviderJSONTask());
        when(mApiCommunicator.getFile(any(String.class), any(Session.class))).thenReturn(Task.forResult(new byte[0]));

        Task<PaymentProvider> paymentProvidersTask = mDocumentTaskManager.getPaymentProvider("");
        paymentProvidersTask.waitForCompletion();
        if (paymentProvidersTask.isFaulted()) {
            throw paymentProvidersTask.getError();
        }
        final PaymentProvider paymentProviderResult = paymentProvidersTask.getResult();
        assertEquals(getPaymentProviders().get(0), paymentProviderResult);
    }

    private List<PaymentProvider> getPaymentProviders() {
        final List<PaymentProvider> paymentProviders = new ArrayList<>();
        paymentProviders.add(new PaymentProvider("7e72441c-32f8-11eb-b611-c3190574373c", "ING-DiBa", "com.example.bank", "3.5.1",
                new PaymentProvider.Colors("112233", "44AAFF"), new byte[0]));
        paymentProviders.add(new PaymentProvider("9a9b41f2-32f8-11eb-9fb5-e378350b0392", "Deutsche Bank", "com.example.bank", "6.9.1",
                new PaymentProvider.Colors("557788", "00DDEE"), new byte[0]));
        return paymentProviders;
    }

    @Test
    public void testCreatePaymentRequest() throws Exception {
        when(mApiCommunicator.postPaymentRequests(any(JSONObject.class), any(Session.class)))
                .thenReturn(createLocationHeaderJSONTask("https://pay-api.gini.net/paymentRequests/7b5a7f79-ae7c-4040-b6cf-25cde58ad937"));

        Task<String> paymentRequestTask = mDocumentTaskManager.createPaymentRequest(new PaymentRequestInput("", "", "", "", "", null, ""));
        paymentRequestTask.waitForCompletion();
        if (paymentRequestTask.isFaulted()) {
            throw paymentRequestTask.getError();
        }
        assertEquals("7b5a7f79-ae7c-4040-b6cf-25cde58ad937", paymentRequestTask.getResult());
    }

    @Test
    public void testResolvePaymentRequest() throws Exception {
        when(mApiCommunicator.resolvePaymentRequests(any(String.class), any(JSONObject.class), any(Session.class)))
                .thenReturn(createResolvePaymentJsonTask());

        Task<ResolvedPayment> paymentRequestTask = mDocumentTaskManager.resolvePaymentRequest("", new ResolvePaymentInput("", "", "", "", null));
        paymentRequestTask.waitForCompletion();
        if (paymentRequestTask.isFaulted()) {
            throw paymentRequestTask.getError();
        }
        ResolvedPayment resolvedPayment = new ResolvedPayment("ginipay-example://payment-requester", "Dr. med. Hackler", "DE02300209000106531065", "CMCIDEDDXXX", "335.50:EUR", "ReNr AZ356789Z", ResolvedPayment.Status.PAID);
        assertEquals(resolvedPayment, paymentRequestTask.getResult());
    }


    @Test
    public void testGetPayment() throws Exception {
        when(mApiCommunicator.getPayment(any(String.class), any(Session.class))).thenReturn(createPaymentJSONTask());

        Task<Payment> paymentRequestTask = mDocumentTaskManager.getPayment("");
        paymentRequestTask.waitForCompletion();
        if (paymentRequestTask.isFaulted()) {
            throw paymentRequestTask.getError();
        }
        Payment payment = new Payment("2020-12-07T15:53:26", "Dr. med. Hackler", "DE02300209000106531065", "335.50:EUR", "ReNr AZ356789Z", "CMCIDEDDXXX");
        assertEquals(payment, paymentRequestTask.getResult());
    }

}
