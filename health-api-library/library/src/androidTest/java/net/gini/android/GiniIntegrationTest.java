package net.gini.android;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static net.gini.android.helpers.TrustKitHelper.resetTrustKit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import com.android.volley.toolbox.NoCache;

import net.gini.android.authorization.EncryptedCredentialsStore;
import net.gini.android.authorization.UserCredentials;
import net.gini.android.helpers.TestUtils;
import net.gini.android.models.Box;
import net.gini.android.models.CompoundExtraction;
import net.gini.android.models.Document;
import net.gini.android.models.Extraction;
import net.gini.android.models.ExtractionsContainer;
import net.gini.android.models.Payment;
import net.gini.android.models.PaymentProvider;
import net.gini.android.models.PaymentRequest;
import net.gini.android.models.PaymentRequestInput;
import net.gini.android.models.ResolvePaymentInput;
import net.gini.android.models.ResolvedPayment;
import net.gini.android.models.SpecificExtraction;
import net.gini.android.requests.ErrorEvent;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import bolts.Continuation;
import bolts.Task;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GiniIntegrationTest {

    private Gini gini;
    private String clientId;
    private String clientSecret;
    private String apiUri;
    private String userCenterUri;

    @Before
    public void setUp() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testPropertiesInput = assetManager.open("test.properties");
        assertNotNull("test.properties not found", testPropertiesInput);
        final Properties testProperties = new Properties();
        testProperties.load(testPropertiesInput);
        clientId = getProperty(testProperties, "testClientId");
        clientSecret = getProperty(testProperties, "testClientSecret");
        apiUri = getProperty(testProperties, "testApiUri");
        userCenterUri = getProperty(testProperties, "testUserCenterUri");

        Log.d("TEST", "testClientId " + clientId);
        Log.d("TEST", "testClientSecret " + clientSecret);
        Log.d("TEST", "testApiUri " + apiUri);
        Log.d("TEST", "testUserCenterUri " + userCenterUri);

        resetTrustKit();

        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();
    }

    private static String getProperty(Properties properties, String propertyName) {
        Object value = properties.get(propertyName);
        assertNotNull(propertyName + " not set!", value);
        return value.toString();
    }

    @Test
    public void processDocumentByteArray() throws IOException, InterruptedException, JSONException {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);
    }

    @Test
    public void processDocumentWithCustomCache() throws IOException, JSONException, InterruptedException {
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCache(new NoCache()).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);
    }

    @Test
    public void sendFeedback_withCompoundExtractions_forDocument_withoutLineItems() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final Map<Document, Map<String, SpecificExtraction>> documentExtractions = processDocument(testDocument, "image/jpeg", "test.jpg",
                DocumentTaskManager.DocumentType.INVOICE);
        final Document document = documentExtractions.keySet().iterator().next();
        final Map<String, SpecificExtraction> extractions = documentExtractions.values().iterator().next();

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        final Map<String, SpecificExtraction> feedback = new HashMap<>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", extractions.get("amountToPay"));
        feedback.put("bic", extractions.get("bic"));
        feedback.put("paymentRecipient", extractions.get("paymentRecipient"));

        // The document had no compound extractions but we create some and send them back as feedback
        final Box box = new Box(1, 2, 3, 4, 5);
        final List<Map<String, SpecificExtraction>> rows = new ArrayList<>();

        final Map<String, SpecificExtraction> firstRowColumns = new HashMap<>();
        firstRowColumns.put("description", new SpecificExtraction("description", "CORE ICON - Sweatjacke - emerald", "text", box,
                Collections.<Extraction>emptyList()));
        firstRowColumns.put("grossPrice",
                new SpecificExtraction("grossPrice", "39.99:EUR", "amount", box, Collections.<Extraction>emptyList()));
        rows.add(firstRowColumns);

        final Map<String, SpecificExtraction> secondRowColumns = new HashMap<>();
        secondRowColumns.put("description",
                new SpecificExtraction("description", "Strickpullover - yellow", "text", box, Collections.<Extraction>emptyList()));
        secondRowColumns.put("grossPrice",
                new SpecificExtraction("grossPrice", "59.99:EUR", "amount", box, Collections.<Extraction>emptyList()));
        rows.add(secondRowColumns);

        final CompoundExtraction compoundExtraction = new CompoundExtraction("lineItems", rows);

        Map<String, CompoundExtraction> feedbackCompound = new HashMap<>();
        feedbackCompound.put("lineItems", compoundExtraction);

        final Task<Document> sendFeedback = gini.getDocumentTaskManager().sendFeedbackForExtractions(document, feedback, feedbackCompound);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }

    @Test
    public void sendFeedback_withoutCompoundExtractions_forDocument_withoutLineItems() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final Map<Document, Map<String, SpecificExtraction>> documentExtractions = processDocument(testDocument, "image/jpeg", "test.jpg",
                DocumentTaskManager.DocumentType.INVOICE);
        final Document document = documentExtractions.keySet().iterator().next();
        final Map<String, SpecificExtraction> extractions = documentExtractions.values().iterator().next();

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        final Map<String, SpecificExtraction> feedback = new HashMap<>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", extractions.get("amountToPay"));
        feedback.put("bic", extractions.get("bic"));
        feedback.put("paymentRecipient", extractions.get("paymentRecipient"));

        final Task<Document> sendFeedback = gini.getDocumentTaskManager().sendFeedbackForExtractions(document, feedback);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }

    @Test
    public void sendFeedback_withoutCompoundExtractions_forDocument_withLineItems() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("line-items.pdf");
        assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final Map<Document, ExtractionsContainer> documentExtractions = processDocument(testDocument, "application/pdf", "line-items.pdf",
                DocumentTaskManager.DocumentType.INVOICE, extractionsContainer -> {});
        final Document document = documentExtractions.keySet().iterator().next();
        final Map<String, SpecificExtraction> extractions = documentExtractions.values().iterator().next().getSpecificExtractions();

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        final Map<String, SpecificExtraction> feedback = new HashMap<>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", extractions.get("amountToPay"));
        feedback.put("bic", extractions.get("bic"));
        feedback.put("paymentRecipient", extractions.get("paymentRecipient"));

        final Task<Document> sendFeedback = gini.getDocumentTaskManager().sendFeedbackForExtractions(document, feedback);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }

    @Test
    public void sendFeedback_withCompoundExtractions_forDocument_withLineItems() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("line-items.pdf");
        assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final Map<Document, ExtractionsContainer> documentExtractions = processDocument(testDocument, "application/pdf", "line-items.pdf",
                DocumentTaskManager.DocumentType.INVOICE, extractionsContainer -> {});
        final Document document = documentExtractions.keySet().iterator().next();
        final ExtractionsContainer extractionsContainer = documentExtractions.values().iterator().next();
        final Map<String, SpecificExtraction> specificExtractions = extractionsContainer.getSpecificExtractions();
        final Map<String, CompoundExtraction> compoundExtractions = extractionsContainer.getCompoundExtractions();

        // All specific extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        final Map<String, SpecificExtraction> feedback = new HashMap<>();
        feedback.put("iban", specificExtractions.get("iban"));
        feedback.put("amountToPay", specificExtractions.get("amountToPay"));
        feedback.put("bic", specificExtractions.get("bic"));
        feedback.put("paymentRecipient", specificExtractions.get("paymentRecipient"));

        // All compound extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        Map<String, CompoundExtraction> feedbackCompound = new HashMap<>();
        feedbackCompound.put("lineItems", compoundExtractions.get("lineItems"));

        final Task<Document> sendFeedback = gini.getDocumentTaskManager().sendFeedbackForExtractions(document, feedback, feedbackCompound);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }

    @Test
    public void documentUploadWorksAfterNewUserWasCreatedIfUserWasInvalid() throws IOException, JSONException, InterruptedException {
        EncryptedCredentialsStore credentialsStore = new EncryptedCredentialsStore(
                getApplicationContext().getSharedPreferences("GiniTests", Context.MODE_PRIVATE), getApplicationContext());
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        // Create invalid user credentials
        UserCredentials invalidUserCredentials = new UserCredentials("invalid@example.com", "1234");
        credentialsStore.storeUserCredentials(invalidUserCredentials);

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);

        // Verify that a new user was created
        assertNotSame(invalidUserCredentials.getUsername(), credentialsStore.getUserCredentials().getUsername());
    }

    @Test
    public void emailDomainIsUpdatedForExistingUserIfEmailDomainWasChanged() throws IOException, JSONException, InterruptedException {
        // Upload a document to make sure we have a valid user
        EncryptedCredentialsStore credentialsStore = new EncryptedCredentialsStore(
                getApplicationContext().getSharedPreferences("GiniTests", Context.MODE_PRIVATE), getApplicationContext());
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);

        // Create another Gini instance with a new email domain (to simulate an app update)
        // and verify that the new email domain is used
        String newEmailDomain = "beispiel.com";
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, newEmailDomain).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);

        UserCredentials newUserCredentials = credentialsStore.getUserCredentials();
        assertEquals(newEmailDomain, extractEmailDomain(newUserCredentials.getUsername()));
    }

    @Test
    public void publicKeyPinningWithMatchingPublicKey() throws Exception {
        resetTrustKit();
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);
    }

    @Test
    public void publicKeyPinningWithCustomCache() throws Exception {
        resetTrustKit();
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCache(new NoCache()).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void publicKeyPinningWithWrongPublicKey() throws Exception {
        resetTrustKit();
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.wrong_network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();

        final Task<Document> upload = documentTaskManager.createPartialDocument(testDocument, "image/jpeg", "test.jpeg", DocumentTaskManager.DocumentType.INVOICE);
        final Task<Document> processDocument = upload.onSuccessTask(new Continuation<Document, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Document> task) throws Exception {
                Document document = task.getResult();
                return documentTaskManager.pollDocument(document);
            }
        });

        final Task<ExtractionsContainer> retrieveExtractions = processDocument.onSuccessTask(
                new Continuation<Document, Task<ExtractionsContainer>>() {
                    @Override
                    public Task<ExtractionsContainer> then(Task<Document> task) throws Exception {
                        return documentTaskManager.getAllExtractions(task.getResult());
                    }
                });

        retrieveExtractions.waitForCompletion();
        if (retrieveExtractions.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(retrieveExtractions.getError()));
        }

        assertTrue("extractions shouldn't have succeeded", retrieveExtractions.isFaulted());
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void publicKeyPinningWithMultiplePublicKeys() throws Exception {
        resetTrustKit();
        gini = new GiniBuilder(getApplicationContext(), clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(net.gini.android.test.R.xml.multiple_keys_network_security_config).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);
    }

    @Test
    public void createPartialDocument() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);

        final Task<Document> task = gini.getDocumentTaskManager()
                .createPartialDocument(testDocument, "image/png", null, null);
        task.waitForCompletion();

        final Document partialDocument = task.getResult();
        assertNotNull(partialDocument);
    }

    @Test
    public void deletePartialDocumentWithoutParents() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);

        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(testDocument, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deleteDocument(task.getResult().getId());
                    }
                });
        task.waitForCompletion();

        assertNotNull(task.getResult());
    }

    @Test
    public void deletePartialDocumentWithParents() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);

        final AtomicReference<Document> partialDocument = new AtomicReference<>();
        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return gini.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deletePartialDocumentAndParents(partialDocument.get().getId());
                    }
                });
        task.waitForCompletion();

        assertNotNull(task.getResult());
    }

    @Test
    public void deletePartialDocumentFailsWhenNotDeletingParents() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);

        final AtomicReference<Document> partialDocument = new AtomicReference<>();
        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return gini.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deleteDocument(partialDocument.get().getId());
                    }
                });
        task.waitForCompletion();

        assertTrue(task.isFaulted());
    }

    @Test
    public void processCompositeDocument() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);
        final InputStream page2Stream = assetManager.open("multi-page-p2.png");
        assertNotNull("test image multi-page-p2.png could not be loaded", page2Stream);
        final InputStream page3Stream = assetManager.open("multi-page-p3.png");
        assertNotNull("test image multi-page-p3.png could not be loaded", page3Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);
        final byte[] page2 = TestUtils.createByteArray(page2Stream);
        final byte[] page3 = TestUtils.createByteArray(page3Stream);

        final List<Document> partialDocuments = new ArrayList<>();
        final AtomicReference<Document> compositeDocument = new AtomicReference<>();
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
        final Task<ExtractionsContainer> task = documentTaskManager
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        partialDocuments.add(task.getResult());
                        return documentTaskManager.createPartialDocument(page2, "image/png", null, null);
                    }
                })
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        partialDocuments.add(task.getResult());
                        return documentTaskManager.createPartialDocument(page3, "image/png", null, null);
                    }
                })
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        partialDocuments.add(task.getResult());
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        for (final Document partialDocument : partialDocuments) {
                            documentRotationDeltaMap.put(partialDocument, 0);
                        }
                        return documentTaskManager.createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        compositeDocument.set(task.getResult());
                        return documentTaskManager.pollDocument(task.getResult());
                    }
                }).onSuccessTask(new Continuation<Document, Task<ExtractionsContainer>>() {
                    @Override
                    public Task<ExtractionsContainer> then(final Task<Document> task) throws Exception {
                        return documentTaskManager.getAllExtractions(task.getResult());
                    }
                });
        task.waitForCompletion();

        assertEquals(3, partialDocuments.size());
        final Map<String, SpecificExtraction> extractions = task.getResult().getSpecificExtractions();
        assertNotNull(extractions);

        assertEquals("IBAN should be found", "DE96490501010082009697", extractions.get("iban").getValue());
        final String amountToPay = extractions.get("amountToPay").getValue();
        assertTrue("Amount to pay should be found: "
                        + "expected one of <[145.00:EUR, 77.00:EUR, 588.60:EUR, 700.43:EUR]> but was:<["
                        + amountToPay + "]>",
                amountToPay.equals("145.00:EUR")
                        || amountToPay.equals("77.00:EUR")
                        || amountToPay.equals("588.60:EUR")
                        || amountToPay.equals("700.43:EUR")
                        || amountToPay.equals("26.42:EUR"));
        assertEquals("BIC should be found", "WELADED1MIN", extractions.get("bic").getValue());
        assertTrue("Payement recipient should be found", extractions.get("paymentRecipient").getValue().startsWith("Mindener Stadtwerke"));
        assertTrue("Payment reference should be found", extractions.get("paymentPurpose").getValue().contains(
                "ReNr TST-00019, KdNr 765432"));

        // all extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        Map<String, SpecificExtraction> feedback = new HashMap<String, SpecificExtraction>();
        feedback.put("iban", extractions.get("iban"));
        feedback.put("amountToPay", extractions.get("amountToPay"));
        feedback.put("bic", extractions.get("bic"));
        feedback.put("paymentRecipient", extractions.get("paymentRecipient"));
        feedback.put("paymentPurpose", extractions.get("paymentPurpose"));

        Map<String, CompoundExtraction> feedbackCompound = new HashMap<>();

        final Task<Document> sendFeedback = documentTaskManager.sendFeedbackForExtractions(compositeDocument.get(), feedback, feedbackCompound);
        sendFeedback.waitForCompletion();
        if (sendFeedback.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.getError()));
        }
        assertTrue("Sending feedback should be completed", sendFeedback.isCompleted());
        assertFalse("Sending feedback should be successful", sendFeedback.isFaulted());
    }

    @Test
    public void testDeleteCompositeDocument() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);

        final AtomicReference<Document> partialDocument = new AtomicReference<>();
        final Task<String> task = gini.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return gini.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return gini.getDocumentTaskManager().deleteDocument(task.getResult().getId());
                    }
                });
        task.waitForCompletion();

        assertNotNull(task.getResult());
    }

    @Test
    public void testGetPaymentProviders() throws Exception {
        Task<List<PaymentProvider>> task = gini.getDocumentTaskManager().getPaymentProviders();
        task.waitForCompletion();
        assertNotNull(task.getResult());
    }

    @Test
    public void testGetPaymentProvider() throws Exception {
        Task<List<PaymentProvider>> listTask = gini.getDocumentTaskManager().getPaymentProviders();
        listTask.waitForCompletion();
        assertNotNull(listTask.getResult());

        final List<PaymentProvider> providers = listTask.getResult();

        Task<PaymentProvider> task = gini.getDocumentTaskManager().getPaymentProvider(providers.get(0).getId());
        task.waitForCompletion();
        assertEquals(providers.get(0), task.getResult());
    }

    @Test
    public void testCreatePaymentRequest() throws Exception {
        Task<String> task = createPaymentRequest();
        task.waitForCompletion();
        assertNotNull(task.getResult());
    }

    @Test
    public void testGetPaymentRequest() throws Exception {
        Task<String> createPaymentTask = createPaymentRequest();
        createPaymentTask.waitForCompletion();
        String id = createPaymentTask.getResult();

        Task<PaymentRequest> paymentRequestTask = gini.getDocumentTaskManager().getPaymentRequest(id);
        paymentRequestTask.waitForCompletion();
        assertNotNull(paymentRequestTask.getResult());
    }

    @Test
    public void testResolvePayment() throws Exception {
        Task<String> createPaymentTask = createPaymentRequest();
        createPaymentTask.waitForCompletion();
        String id = createPaymentTask.getResult();

        Task<PaymentRequest> paymentRequestTask = gini.getDocumentTaskManager().getPaymentRequest(id);
        paymentRequestTask.waitForCompletion();

        PaymentRequest paymentRequest = paymentRequestTask.getResult();
        final ResolvePaymentInput resolvePaymentInput = new ResolvePaymentInput(paymentRequest.getRecipient(), paymentRequest.getIban(), paymentRequest.getAmount(), paymentRequest.getPurpose(), null);

        Task<ResolvedPayment> resolvePaymentRequestTask = gini.getDocumentTaskManager().resolvePaymentRequest(id, resolvePaymentInput);
        resolvePaymentRequestTask.waitForCompletion();
        assertNotNull(resolvePaymentRequestTask.getResult());
    }

    @Test
    public void testGetPayment() throws Exception {
        Task<String> createPaymentTask = createPaymentRequest();
        createPaymentTask.waitForCompletion();
        String id = createPaymentTask.getResult();

        Task<PaymentRequest> paymentRequestTask = gini.getDocumentTaskManager().getPaymentRequest(id);
        paymentRequestTask.waitForCompletion();

        PaymentRequest paymentRequest = paymentRequestTask.getResult();
        final ResolvePaymentInput resolvePaymentInput = new ResolvePaymentInput(paymentRequest.getRecipient(), paymentRequest.getIban(), paymentRequest.getAmount(), paymentRequest.getPurpose(), null);

        Task<ResolvedPayment> resolvePaymentRequestTask = gini.getDocumentTaskManager().resolvePaymentRequest(id, resolvePaymentInput);
        resolvePaymentRequestTask.waitForCompletion();

        Task<Payment> getPaymentRequestTask = gini.getDocumentTaskManager().getPayment(id);
        getPaymentRequestTask.waitForCompletion();
        assertNotNull(getPaymentRequestTask.getResult());
        assertEquals(paymentRequest.getRecipient(), getPaymentRequestTask.getResult().getRecipient());
        assertEquals(paymentRequest.getIban(), getPaymentRequestTask.getResult().getIban());
        assertEquals(paymentRequest.getBic(), getPaymentRequestTask.getResult().getBic());
        assertEquals(paymentRequest.getAmount(), getPaymentRequestTask.getResult().getAmount());
        assertEquals(paymentRequest.getPurpose(), getPaymentRequestTask.getResult().getPurpose());
    }

    @Test
    public void testGetImage() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        Map<Document, Map<String, SpecificExtraction>> documentWithExtractions = processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);
        Document document = documentWithExtractions.keySet().iterator().next();

        Task<byte[]> task = gini.getDocumentTaskManager().getPageImage(document.getId(), 1);
        task.waitForCompletion();
        assertNotNull(task.getResult());
        byte[] bytes = task.getResult();
        assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
    }

    @Test
    public void logErrorEvent() throws Exception {
        final ErrorEvent errorEvent = new ErrorEvent(
                Build.MODEL, "Android", Build.VERSION.RELEASE,
                "not available", BuildConfig.VERSION_NAME, "Error logging integration test"
        );

        final Task<Void> requestTask = gini.getDocumentTaskManager().logErrorEvent(errorEvent);
        requestTask.waitForCompletion();

        assertNull(requestTask.getError());
    }

    private Task<String> createPaymentRequest() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        Map<Document, Map<String, SpecificExtraction>> documentWithExtractions = processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);
        Document document = documentWithExtractions.keySet().iterator().next();
        Map<String, SpecificExtraction> extractions = documentWithExtractions.get(document);

        Task<List<PaymentProvider>> listTask = gini.getDocumentTaskManager().getPaymentProviders();
        listTask.waitForCompletion();
        assertNotNull(listTask.getResult());
        final List<PaymentProvider> providers = listTask.getResult();

        final PaymentRequestInput paymentRequest = new PaymentRequestInput(
                providers.get(0).getId(),
                Objects.requireNonNull(Objects.requireNonNull(extractions).get("paymentRecipient")).getValue(),
                Objects.requireNonNull(extractions.get("iban")).getValue(),
                Objects.requireNonNull(extractions.get("amountToPay")).getValue(),
                Objects.requireNonNull(extractions.get("paymentPurpose")).getValue(),
                null, // We make bic optional for now
//                Objects.requireNonNull(extractions.get("bic")).getValue(),
                document.getUri().toString()
        );
        return gini.getDocumentTaskManager().createPaymentRequest(paymentRequest);
    }

    private String extractEmailDomain(String email) {
        String[] components = email.split("@");
        if (components.length > 1) {
            return components[1];
        }
        return "";
    }

    private Map<Document, Map<String, SpecificExtraction>> processDocument(byte[] documentBytes, String contentType, String filename, DocumentTaskManager.DocumentType documentType)
            throws InterruptedException {
        final Map<Document, ExtractionsContainer> result = processDocument(documentBytes, contentType, filename, documentType, extractionsContainer -> {
            final Map<String, SpecificExtraction> extractions = extractionsContainer.getSpecificExtractions();
            assertEquals("IBAN should be found", "DE78370501980020008850", extractions.get("iban").getValue());
            assertEquals("Amount to pay should be found", "1.00:EUR", extractions.get("amountToPay").getValue());
            assertEquals("BIC should be found", "COLSDE33", extractions.get("bic").getValue());
            assertEquals("Payee should be found", "Uno Flüchtlingshilfe", extractions.get("paymentRecipient").getValue());
        });
        final Document document = result.keySet().iterator().next();
        final Map<String, SpecificExtraction> specificExtractions = result.values().iterator().next().getSpecificExtractions();
        return Collections.singletonMap(document, specificExtractions);
    }

    private Map<Document, ExtractionsContainer> processDocument(byte[] documentBytes, String contentType, String filename, DocumentTaskManager.DocumentType documentType,
                                                                ExtractionsCallback extractionsCallback)
            throws InterruptedException {
        final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();

        final Task<Document> uploadPartial = documentTaskManager.createPartialDocument(documentBytes, contentType, filename, documentType);
        final Task<Document> createComposite = uploadPartial.onSuccessTask(task -> {
           Document document = task.getResult();
           return documentTaskManager.createCompositeDocument(Collections.singletonList(document), null);
        });
        final Task<Document> processDocument = createComposite.onSuccessTask(task -> {
            Document document = task.getResult();
            return documentTaskManager.pollDocument(document);
        });

        final Task<ExtractionsContainer> retrieveExtractions = processDocument.onSuccessTask(
                task -> documentTaskManager.getAllExtractions(task.getResult())
        );

        retrieveExtractions.waitForCompletion();
        if (retrieveExtractions.isFaulted()) {
            Log.e("TEST", Log.getStackTraceString(retrieveExtractions.getError()));
        }

        assertFalse("extractions should have succeeded", retrieveExtractions.isFaulted());

        extractionsCallback.onExtractionsAvailable(retrieveExtractions.getResult());

        return Collections.singletonMap(createComposite.getResult(), retrieveExtractions.getResult());
    }

    private interface ExtractionsCallback {
        void onExtractionsAvailable(@NonNull final ExtractionsContainer extractionsContainer);
    }
}
