package net.gini.android.core.api.test.shared;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static net.gini.android.core.api.test.shared.helpers.TrustKitHelper.resetTrustKit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import com.android.volley.toolbox.NoCache;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.authorization.EncryptedCredentialsStore;
import net.gini.android.core.api.authorization.UserCredentials;
import net.gini.android.core.api.internal.GiniCoreAPI;
import net.gini.android.core.api.internal.GiniCoreAPIBuilder;
import net.gini.android.core.api.models.ExtractionsContainer;
import net.gini.android.core.api.test.shared.helpers.TestUtils;
import net.gini.android.core.api.models.Document;
import net.gini.android.core.api.models.SpecificExtraction;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import bolts.Continuation;
import bolts.Task;

@LargeTest
public abstract class GiniCoreAPIIntegrationTest<DTM extends DocumentTaskManager<A, E>, DM extends DocumentManager<A, DTM, E>, G extends GiniCoreAPI<DTM,DM, A, E>, A extends ApiCommunicator, E extends ExtractionsContainer> {

    protected G giniCoreAPI;
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

        resetTrustKit();

        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();
    }

    protected abstract GiniCoreAPIBuilder<DTM, DM, G, A, E> createGiniCoreAPIBuilder(@NonNull final String clientId,
                                                                      @NonNull final String clientSecret,
                                                                      @NonNull final String emailDomain);

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
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
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
    public void documentUploadWorksAfterNewUserWasCreatedIfUserWasInvalid() throws IOException, JSONException, InterruptedException {
        EncryptedCredentialsStore credentialsStore = new EncryptedCredentialsStore(
                getApplicationContext().getSharedPreferences("GiniTests", Context.MODE_PRIVATE), getApplicationContext());
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
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
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
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
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, newEmailDomain).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setCredentialsStore(credentialsStore).
                build();

        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE);

        UserCredentials newUserCredentials = credentialsStore.getUserCredentials();
        assertEquals(newEmailDomain, extractEmailDomain(newUserCredentials.getUsername()));
    }

    @XmlRes
    protected abstract int getNetworkSecurityConfigResId();

    @Test
    public void publicKeyPinningWithMatchingPublicKey() throws Exception {
        resetTrustKit();
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(getNetworkSecurityConfigResId()).
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
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(getNetworkSecurityConfigResId()).
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
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(getNetworkSecurityConfigResId()).
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);
        final DocumentTaskManager<A, E> documentTaskManager = giniCoreAPI.getDocumentTaskManager();

        final Task<Document> upload = documentTaskManager.createPartialDocument(testDocument, "image/jpeg", "test.jpeg", DocumentTaskManager.DocumentType.INVOICE);
        final Task<Document> processDocument = upload.onSuccessTask(new Continuation<Document, Task<Document>>() {
            @Override
            public Task<Document> then(Task<Document> task) throws Exception {
                Document document = task.getResult();
                return documentTaskManager.pollDocument(document);
            }
        });

        final Task<E> retrieveExtractions = processDocument.onSuccessTask(
                new Continuation<Document, Task<E>>() {
                    @Override
                    public Task<E> then(Task<Document> task) throws Exception {
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
        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
                setNetworkSecurityConfigResId(getNetworkSecurityConfigResId()).
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

        final Task<Document> task = giniCoreAPI.getDocumentTaskManager()
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

        final Task<String> task = giniCoreAPI.getDocumentTaskManager()
                .createPartialDocument(testDocument, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return giniCoreAPI.getDocumentTaskManager().deleteDocument(task.getResult().getId());
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
        final Task<String> task = giniCoreAPI.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return giniCoreAPI.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return giniCoreAPI.getDocumentTaskManager().deletePartialDocumentAndParents(partialDocument.get().getId());
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
        final Task<String> task = giniCoreAPI.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return giniCoreAPI.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return giniCoreAPI.getDocumentTaskManager().deleteDocument(partialDocument.get().getId());
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
        final DocumentTaskManager<A, E> documentTaskManager = giniCoreAPI.getDocumentTaskManager();
        final Task<E> task = documentTaskManager
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
                }).onSuccessTask(new Continuation<Document, Task<E>>() {
                    @Override
                    public Task<E> then(final Task<Document> task) throws Exception {
                        return documentTaskManager.getAllExtractions(task.getResult());
                    }
                });
        task.waitForCompletion();

        assertEquals(3, partialDocuments.size());
        final E extractionsContainer = task.getResult();
        assertNotNull(extractionsContainer);

        final String iban = getIban(extractionsContainer).getValue();
        assertEquals("IBAN should be found, but was: " + iban, "DE96490501010082009697", iban);
        final String amountToPay = getAmountToPay(extractionsContainer).getValue();
        assertNotNull("Amount to pay should be found.", amountToPay);
        final String bic = getBic(extractionsContainer).getValue();
        assertEquals("BIC should be found, but was: " + bic, "WELADED1MIN", bic);
        final String paymentRecipient = getPaymentRecipient(extractionsContainer).getValue();
        assertTrue("Payement recipient should be found", paymentRecipient.startsWith("Mindener Stadtwerke"));
        final String paymentPurpose = getPaymentPurpose(extractionsContainer).getValue();
        assertTrue("Payment reference should be found, but was: " + paymentPurpose, paymentPurpose.contains("765432"));
    }

    @Test
    public void testDeleteCompositeDocument() throws Exception {
        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream page1Stream = assetManager.open("multi-page-p1.png");
        assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream);

        final byte[] page1 = TestUtils.createByteArray(page1Stream);

        final AtomicReference<Document> partialDocument = new AtomicReference<>();
        final Task<String> task = giniCoreAPI.getDocumentTaskManager()
                .createPartialDocument(page1, "image/png", null, null)
                .onSuccessTask(new Continuation<Document, Task<Document>>() {
                    @Override
                    public Task<Document> then(final Task<Document> task) throws Exception {
                        final Document document = task.getResult();
                        partialDocument.set(document);
                        final LinkedHashMap<Document, Integer> documentRotationDeltaMap = new LinkedHashMap<>();
                        documentRotationDeltaMap.put(document, 0);
                        return giniCoreAPI.getDocumentTaskManager().createCompositeDocument(documentRotationDeltaMap, null);
                    }
                }).onSuccessTask(new Continuation<Document, Task<String>>() {
                    @Override
                    public Task<String> then(final Task<Document> task) throws Exception {
                        return giniCoreAPI.getDocumentTaskManager().deleteDocument(task.getResult().getId());
                    }
                });
        task.waitForCompletion();

        assertNotNull(task.getResult());
    }

    @Test
    public void allowsUsingCustomTrustManager() throws IOException, InterruptedException {
        final AtomicBoolean customTrustManagerWasCalled = new AtomicBoolean(false);

        // Don't trust any certificates: blocks all network calls
        final TrustManager blockingTrustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                customTrustManagerWasCalled.set(true);
                throw new CertificateException();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                customTrustManagerWasCalled.set(true);
                throw new CertificateException();
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                customTrustManagerWasCalled.set(true);
                return new X509Certificate[0];
            }
        };

        giniCoreAPI = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com").
                setApiBaseUrl(apiUri).
                setUserCenterApiBaseUrl(userCenterUri).
                setConnectionTimeoutInMs(60000).
                setTrustManager(blockingTrustManager).
                build();

        final AssetManager assetManager = getApplicationContext().getResources().getAssets();
        final InputStream testDocumentAsStream = assetManager.open("test.jpg");
        assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream);

        final byte[] testDocument = TestUtils.createByteArray(testDocumentAsStream);

        final DocumentTaskManager<A, E> documentTaskManager = giniCoreAPI.getDocumentTaskManager();

        final Task<Document> uploadPartial = documentTaskManager.createPartialDocument(testDocument, "image/jpeg", null, null);
        final Task<Document> createComposite = uploadPartial.onSuccessTask(task -> {
            Document document = task.getResult();
            return documentTaskManager.createCompositeDocument(Collections.singletonList(document), null);
        });
        final Task<Document> processDocument = createComposite.onSuccessTask(task -> {
            Document document = task.getResult();
            return documentTaskManager.pollDocument(document);
        });

        final Task<E> retrieveExtractions = processDocument.onSuccessTask(
                task -> documentTaskManager.getAllExtractions(task.getResult())
        );

        retrieveExtractions.waitForCompletion();

        // Custom TrustManager should have been called and all requests should have failed
        assertTrue(customTrustManagerWasCalled.get());
        assertTrue(retrieveExtractions.isFaulted());
    }

    private String extractEmailDomain(String email) {
        String[] components = email.split("@");
        if (components.length > 1) {
            return components[1];
        }
        return "";
    }

    protected Map<Document, E> processDocument(byte[] documentBytes, String contentType, String filename, DocumentTaskManager.DocumentType documentType)
            throws InterruptedException {
        final Map<Document, E> result = processDocument(documentBytes, contentType, filename, documentType, extractionsContainer -> {
            assertEquals("IBAN should be found", "DE78370501980020008850", getIban(extractionsContainer).getValue());
            assertEquals("Amount to pay should be found", "1.00:EUR", getAmountToPay(extractionsContainer).getValue());
            assertEquals("BIC should be found", "COLSDE33", getBic(extractionsContainer).getValue());
            assertEquals("Payee should be found", "Uno Fl??chtlingshilfe", getPaymentRecipient(extractionsContainer).getValue());
        });
        return result;
    }

    @Nullable
    protected abstract SpecificExtraction getIban(@NonNull final E extractionsContainer);

    @Nullable
    protected abstract SpecificExtraction getBic(@NonNull final E extractionsContainer);

    @Nullable
    protected abstract SpecificExtraction getAmountToPay(@NonNull final E extractionsContainer);

    @Nullable
    protected abstract SpecificExtraction getPaymentRecipient(@NonNull final E extractionsContainer);

    @Nullable
    protected abstract SpecificExtraction getPaymentPurpose(@NonNull final E extractionsContainer);

    protected Map<Document, E> processDocument(byte[] documentBytes, String contentType, String filename, DocumentTaskManager.DocumentType documentType,
                                                                ExtractionsCallback<E> extractionsCallback)
            throws InterruptedException {
        final DocumentTaskManager<A, E> documentTaskManager = giniCoreAPI.getDocumentTaskManager();

        final Task<Document> uploadPartial = documentTaskManager.createPartialDocument(documentBytes, contentType, filename, documentType);
        final Task<Document> createComposite = uploadPartial.onSuccessTask(task -> {
           Document document = task.getResult();
           return documentTaskManager.createCompositeDocument(Collections.singletonList(document), null);
        });
        final Task<Document> processDocument = createComposite.onSuccessTask(task -> {
            Document document = task.getResult();
            return documentTaskManager.pollDocument(document);
        });

        final Task<E> retrieveExtractions = processDocument.onSuccessTask(
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

    protected interface ExtractionsCallback<E extends ExtractionsContainer> {
        void onExtractionsAvailable(@NonNull final E extractionsContainer);
    }

}
