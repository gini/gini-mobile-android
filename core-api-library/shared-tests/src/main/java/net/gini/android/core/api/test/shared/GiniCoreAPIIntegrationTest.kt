package net.gini.android.core.api.test.shared

import android.content.Context
import android.util.Log
import androidx.annotation.XmlRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.EncryptedCredentialsStore
import net.gini.android.core.api.authorization.UserCredentials
import net.gini.android.core.api.internal.GiniCoreAPI
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.test.shared.helpers.TestUtils
import net.gini.android.core.api.test.shared.helpers.TrustKitHelper
import okhttp3.Cache
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@LargeTest
abstract class GiniCoreAPIIntegrationTest<DM: DocumentManager<DR, E>, DR: DocumentRepository<E>, G: GiniCoreAPI<DM, DR, E>, E: ExtractionsContainer>{

    protected lateinit var giniCoreApi: G
    private lateinit var clientId: String
    private lateinit var clientSecret: String
    private lateinit var apiUri: String
    protected lateinit var userCenterUri: String
    private lateinit var credentialsStore: InMemoryCredentialsStore

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testPropertiesInput = assetManager.open("test.properties")
        Assert.assertNotNull("test.properties not found", testPropertiesInput)

        val testProperties = Properties()
        testProperties.load(testPropertiesInput)

        clientId = getProperty(testProperties, "testClientId")
        clientSecret = getProperty(testProperties, "testClientSecret")
        apiUri = getProperty(testProperties, "testApiUri")
        userCenterUri = getProperty(testProperties, "testUserCenterUri")

        onTestPropertiesAvailable(testProperties)

        TrustKitHelper.resetTrustKit()

        credentialsStore = InMemoryCredentialsStore()

        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com")
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri)
            .setConnectionTimeoutInMs(60000)
            .setCredentialsStore(credentialsStore)
            .setDebuggingEnabled(true)
            .build()
    }

    abstract fun onTestPropertiesAvailable(properties: Properties)

    class InMemoryCredentialsStore: CredentialsStore {

        private var credentials: UserCredentials? = null

        override fun storeUserCredentials(userCredentials: UserCredentials?): Boolean {
            credentials = userCredentials
            return true
        }

        override fun getUserCredentials(): UserCredentials? {
            return credentials
        }

        override fun deleteUserCredentials(): Boolean {
            credentials = null
            return true
        }

    }

    protected abstract fun createGiniCoreAPIBuilder(clientId: String, clientSecret: String, emailDomain: String): GiniCoreAPIBuilder<DM, G, DR, E>

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    fun processDocumentByteArray() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    fun processDocumentWithCustomCache() = runTest {
        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com")
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri)
            .setConnectionTimeoutInMs(60000)
            .setCache(Cache(File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "no_cache"), 1))
            .build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    fun documentUploadWorksAfterNewUserWasCreatedIfUserWasInvalid() = runTest {
        val credentialsStore = EncryptedCredentialsStore(ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("GiniTests", Context.MODE_PRIVATE), ApplicationProvider.getApplicationContext())
        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com")
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri)
            .setConnectionTimeoutInMs(60000)
            .setCredentialsStore(credentialsStore)
            .build()

        // Create invalid user credentials
        val invalidUserCredentials = UserCredentials("invalid@example.com", "1234")
        credentialsStore.storeUserCredentials(invalidUserCredentials)

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)

        // Verify that a new user was created
        Assert.assertNotSame(invalidUserCredentials.username, credentialsStore.userCredentials.username)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    fun emailDomainIsUpdatedForExistingUserIfEmailDomainWasChanged() = runTest {

        // Upload a document to make sure we have a valid user
        val credentialsStore = EncryptedCredentialsStore(ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("GiniTests", Context.MODE_PRIVATE), ApplicationProvider.getApplicationContext())
        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com")
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri)
            .setConnectionTimeoutInMs(60000)
            .setCredentialsStore(credentialsStore)
            .build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)


        // Create another Gini instance with a new email domain (to simulate an app update)
        // and verify that the new email domain is used
        val newEmailDomain = "beispiel.com"
        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, newEmailDomain)
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri)
            .setConnectionTimeoutInMs(60000)
            .setCredentialsStore(credentialsStore)
            .build()
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)

        val newUserCredentials = credentialsStore.userCredentials
        Assert.assertEquals(newEmailDomain, extractEmailDomain(newUserCredentials.username))
    }

    @XmlRes
    protected abstract fun getNetworkSecurityConfigResId(): Int

    @Test
    @Throws(Exception::class)
    fun publicKeyPinningWithMatchingPublicKey() = runTest {
        TrustKitHelper.resetTrustKit()
        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com")
            .setNetworkSecurityConfigResId(getNetworkSecurityConfigResId())
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri)
            .setConnectionTimeoutInMs(60000)
            .build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)
    }

    @Test
    @Throws(Exception::class)
    fun publicKeyPinningWithCustomCache() = runTest {
        TrustKitHelper.resetTrustKit()
        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com")
            .setNetworkSecurityConfigResId(getNetworkSecurityConfigResId())
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri)
            .setConnectionTimeoutInMs(60000)
            .setCache(Cache(File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "no_cache"), 1))
            .build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)
    }

    @Test
    @Throws(Exception::class)
    fun createPartialDocument() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("multi-page-p1.jpg")
        Assert.assertNotNull("test image multi-page-p1.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)

        val documentCall = giniCoreApi.documentManager.createPartialDocument(testDocument, "image/png", null, null)
        Assert.assertNotNull(documentCall.dataOrThrow)
    }

    @Test
    @Throws(Exception::class)
    fun deletePartialDocumentWithoutParents() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("multi-page-p1.jpg")
        Assert.assertNotNull("test image multi-page-p1.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)

        val document = giniCoreApi.documentManager.createPartialDocument(testDocument, "image/png", null, null).dataOrThrow
        val deleteResult = giniCoreApi.documentManager.deleteDocument(document.id)
        Assert.assertNotNull(deleteResult.dataOrThrow)
    }

    @Test
    @Throws(Exception::class)
    fun deletePartialDocumentWithParents() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val page1Stream = assetManager.open("multi-page-p1.jpg")
        Assert.assertNotNull("test image multi-page-p1.jpg could not be loaded", page1Stream)

        val page1 = TestUtils.createByteArray(page1Stream)

        val partialDocument = giniCoreApi.documentManager.createPartialDocument(page1, "image/png", null, null).dataOrThrow

        val documentRotationDeltaMap = LinkedHashMap<Document, Int>()
        documentRotationDeltaMap[partialDocument] = 0
        val compositeDocument = giniCoreApi.documentManager.createCompositeDocument(documentRotationDeltaMap, null).dataOrThrow
        val deleteResource = giniCoreApi.documentManager.deletePartialDocumentAndParents(partialDocument.id)

        Assert.assertNotNull(deleteResource.dataOrThrow)
    }

    @Test
    @Throws(Exception::class)
    fun deletePartialDocumentFailsWhenNotDeletingParents() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val page1Stream = assetManager.open("multi-page-p1.jpg")
        Assert.assertNotNull("test image multi-page-p1.jpg could not be loaded", page1Stream)

        val page1 = TestUtils.createByteArray(page1Stream)
        val partialDocument = AtomicReference<Document>()

        val document = giniCoreApi.documentManager.createPartialDocument(page1, "image/png", null, null).dataOrThrow
        partialDocument.set(document)
        val documentRotationDeltaMap = LinkedHashMap<Document, Int>()
        documentRotationDeltaMap[document] = 0
        giniCoreApi.documentManager.createCompositeDocument(documentRotationDeltaMap, null).dataOrThrow
        val deleteResource = giniCoreApi.documentManager.deleteDocument(partialDocument.get().id)

        Assert.assertTrue(deleteResource is Resource.Error)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun processCompositeDocument() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val page1Stream = assetManager.open("multi-page-p1.jpg")
        Assert.assertNotNull("test image multi-page-p1.jpg could not be loaded", page1Stream)
        val page2Stream = assetManager.open("multi-page-p2.jpg")
        Assert.assertNotNull("test image multi-page-p2.jpg could not be loaded", page2Stream)

        val page1 = TestUtils.createByteArray(page1Stream)
        val page2 = TestUtils.createByteArray(page2Stream)

        // Create the partial documents in parallel as this is how it's done in our SDKs
        val partialDocumentsAsync = listOf(
            async { giniCoreApi.documentManager.createPartialDocument(page1, "image/png").dataOrThrow },
            async { giniCoreApi.documentManager.createPartialDocument(page2, "image/png").dataOrThrow }
        )
        val partialDocuments = partialDocumentsAsync.map { it.await() }

        val documentRotationDeltaMap = partialDocuments.associateWithTo(linkedMapOf()) { 0 }

        val compositeDocument = giniCoreApi.documentManager.createCompositeDocument(documentRotationDeltaMap)

        val processDocument = giniCoreApi.documentManager.pollDocument(compositeDocument.dataOrThrow)

        val extractionsContainer = giniCoreApi.documentManager.getAllExtractions(processDocument.dataOrThrow).dataOrThrow

        val iban = getIban(extractionsContainer)?.value
        Assert.assertNotNull("IBAN should be found", iban)
        val amountToPay = getAmountToPay(extractionsContainer)?.value
        Assert.assertNotNull("Amount to pay should be found.", amountToPay)
        val bic = getBic(extractionsContainer)?.value
        Assert.assertNotNull("BIC should be found", bic)
        val paymentRecipient = getPaymentRecipient(extractionsContainer)?.value
        Assert.assertNotNull("Payement recipient should be found", paymentRecipient)
        val paymentPurpose = getPaymentPurpose(extractionsContainer)?.value
        Assert.assertNotNull("Payment purpose should be found", paymentPurpose)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testDeleteCompositeDocument() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val page1Stream = assetManager.open("multi-page-p1.jpg")
        Assert.assertNotNull("test image multi-page-p1.jpg could not be loaded", page1Stream)

        val testDocument = TestUtils.createByteArray(page1Stream)

        val partialDocument = giniCoreApi.documentManager.createPartialDocument(testDocument, "image/png").dataOrThrow

        val compositeDocument = giniCoreApi.documentManager.createCompositeDocument(linkedMapOf(partialDocument to 0)).dataOrThrow

        val deleteResource = giniCoreApi.documentManager.deleteDocument(compositeDocument.id)

        Assert.assertTrue("Deletion should have succeeded", deleteResource is Resource.Success)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun allowsUsingCustomTrustManager() = runTest {
        val customTrustManagerWasCalled = AtomicBoolean(false)

        // Don't trust any certificates: blocks all network calls
        val blockingTrustManager: TrustManager = object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                customTrustManagerWasCalled.set(true)
                throw CertificateException()
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                customTrustManagerWasCalled.set(true)
                throw CertificateException()
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                customTrustManagerWasCalled.set(true)
                return arrayOf()
            }
        }

        giniCoreApi = createGiniCoreAPIBuilder(clientId, clientSecret, "example.com")
            .setApiBaseUrl(apiUri)
            .setUserCenterApiBaseUrl(userCenterUri).setConnectionTimeoutInMs(60000)
            .setTrustManager(blockingTrustManager)
            .build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)

        val partialDocument = giniCoreApi.documentManager.createPartialDocument(testDocument, "image/png")

        Assert.assertTrue(customTrustManagerWasCalled.get())
        Assert.assertTrue("Partial document upload should have failed", partialDocument is Resource.Error)
    }

    @Test
    fun getDocumentLayout() = runTest {
        // Given
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractionsMap = processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)

        // When
        val layout = giniCoreApi.documentManager.getLayout(documentExtractionsMap.keys.first())

        // Then
        Assert.assertNotNull(layout.dataOrThrow.optJSONArray("pages"))
    }

    @Throws(InterruptedException::class)
    protected suspend fun processDocument(documentBytes: ByteArray, contentType: String, filename: String, documentType: DocumentManager.DocumentType
    ): Map<Document, E> {
        return processDocument(documentBytes, contentType, filename, documentType) { extractionsContainer ->
            Assert.assertEquals("IBAN should be found", "DE78370501980020008850", getIban(extractionsContainer)?.value)
            Assert.assertEquals("Amount to pay should be found", "1.00:EUR", getAmountToPay(extractionsContainer)?.value)
            Assert.assertEquals("BIC should be found", "COLSDE33", getBic(extractionsContainer)?.value)
            Assert.assertEquals("Payee should be found", "Uno Flüchtlingshilfe", getPaymentRecipient(extractionsContainer)?.value)
        }
    }

    protected abstract fun getIban(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getBic(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getAmountToPay(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getPaymentRecipient(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getPaymentPurpose(extractionsContainer: E): SpecificExtraction?

    @Throws(InterruptedException::class)
    protected suspend fun processDocument(documentBytes: ByteArray, contentType: String, filename: String, documentType: DocumentManager.DocumentType, extractionsCallback: (E) -> Unit): Map<Document, E> {
        val documentManager = giniCoreApi.documentManager
        val uploadPartial = documentManager.createPartialDocument(documentBytes, contentType, filename, documentType)
        val createComposite = documentManager.createCompositeDocument(listOf(uploadPartial.dataOrThrow), null)
        val processDocument = documentManager.pollDocument(createComposite.dataOrThrow)

        val retrieveExtractions = documentManager.getAllExtractions(processDocument.dataOrThrow)

        if (retrieveExtractions is Resource.Error) {
            Log.e("TEST", retrieveExtractions.responseBody ?: "")
        }
        retrieveExtractions.throwIfNotSuccess()

        Assert.assertTrue("extractions should have succeeded", retrieveExtractions is Resource.Success)
        extractionsCallback(retrieveExtractions.dataOrThrow)

        return Collections.singletonMap(createComposite.dataOrThrow, retrieveExtractions.dataOrThrow);
    }

    protected fun getProperty(properties: Properties, propertyName: String): String {
        val value = properties[propertyName]
        Assert.assertNotNull("$propertyName not set!", value)
        return value.toString()
    }

    private fun extractEmailDomain(email: String): String? {
        val components = email.split("@").toTypedArray()
        return if (components.size > 1) {
            components[1]
        } else ""
    }

    protected interface ExtractionsCallback<E : ExtractionsContainer?> {
        fun onExtractionsAvailable(extractionsContainer: E)
    }

    protected val <T> Resource<T>.dataOrThrow: T
        get() {
            throwIfNotSuccess()
            return (this as? Resource.Success)?.data ?: throw Exception("Resource data is missing")
        }

    protected fun <T> Resource<T>.throwIfNotSuccess() {
        when (this) {
            is Resource.Cancelled -> throw CancellationException("Request was cancelled")
            is Resource.Error -> {
                this.exception?.let { e ->
                    Log.getStackTraceString(e)
                    throw e
                } ?: throw Exception(toString())
            }
            is Resource.Success -> {}
        }
    }
}