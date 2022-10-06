package net.gini.android.core.api.test.shared

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.XmlRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.android.volley.toolbox.NoCache
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRemoteSource
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.EncryptedCredentialsStore
import net.gini.android.core.api.authorization.UserCredentials
import net.gini.android.core.api.internal.KGiniCoreAPI
import net.gini.android.core.api.internal.KGiniCoreAPIBuilder
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.test.shared.helpers.TestUtils
import net.gini.android.core.api.test.shared.helpers.TrustKitHelper
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@LargeTest
abstract class KGiniCoreAPIIntegrationTest<DM: DocumentManager<DR, E>, DR: DocumentRepository<E>, G: KGiniCoreAPI<DM, DR, E>, E: ExtractionsContainer>{

    private var giniCoreApi: G? = null
    private var clientId: String? = null
    private var clientSecret: String? = null
    private var apiUri: String? = null
    private var userCenterUri: String? = null

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

        TrustKitHelper.resetTrustKit()

        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")?.setApiBaseUrl(apiUri!!)
            ?.setUserCenterApiBaseUrl(userCenterUri!!)?.setConnectionTimeoutInMs(60000)?.build()
    }

    protected abstract fun createGiniCoreAPIBuilder(clientId: String, clientSecret: String, emailDomain: String): KGiniCoreAPIBuilder<DM, G, DR, E>?

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    open fun processDocumentByteArray() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    open fun processDocumentWithCustomCache() = runTest {
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")!!.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.setCache(NoCache())!!.build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    open fun documentUploadWorksAfterNewUserWasCreatedIfUserWasInvalid() = runTest {
        val credentialsStore = EncryptedCredentialsStore(ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("GiniTests", Context.MODE_PRIVATE), ApplicationProvider.getApplicationContext())
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")!!.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.setCredentialsStore(credentialsStore)!!.build()

        // Create invalid user credentials
        val invalidUserCredentials = UserCredentials("invalid@example.com", "1234")
        credentialsStore.storeUserCredentials(invalidUserCredentials)

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)

        // Verify that a new user was created
        Assert.assertNotSame(invalidUserCredentials.username, credentialsStore.userCredentials.username)
    }

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    open fun emailDomainIsUpdatedForExistingUserIfEmailDomainWasChanged() = runTest {

        // Upload a document to make sure we have a valid user
        val credentialsStore = EncryptedCredentialsStore(ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("GiniTests", Context.MODE_PRIVATE), ApplicationProvider.getApplicationContext())
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")!!.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.setCredentialsStore(credentialsStore)!!.build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)


        // Create another Gini instance with a new email domain (to simulate an app update)
        // and verify that the new email domain is used
        val newEmailDomain = "beispiel.com"
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, newEmailDomain)!!.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.setCredentialsStore(credentialsStore)!!.build()
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)

        val newUserCredentials = credentialsStore.userCredentials
        Assert.assertEquals(newEmailDomain, extractEmailDomain(newUserCredentials.username))
    }

    @XmlRes
    protected abstract fun getNetworkSecurityConfigResId(): Int

    @Test
    @Throws(Exception::class)
    open fun publicKeyPinningWithMatchingPublicKey() = runTest {
        TrustKitHelper.resetTrustKit()
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")!!
            .setNetworkSecurityConfigResId(getNetworkSecurityConfigResId())?.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)
    }

    @Test
    @Throws(Exception::class)
    open fun publicKeyPinningWithCustomCache() = runTest {
        TrustKitHelper.resetTrustKit()
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")!!
            .setNetworkSecurityConfigResId(getNetworkSecurityConfigResId())?.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.setCache(NoCache())!!.build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Throws(Exception::class)
    open fun publicKeyPinningWithWrongPublicKey() = runTest {
        TrustKitHelper.resetTrustKit()
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")!!
            .setNetworkSecurityConfigResId(getNetworkSecurityConfigResId())?.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val upload = giniCoreApi?.documentManager?.createPartialDocument(testDocument, "image/jpeg", "test.jpeg", DocumentRemoteSource.Companion.DocumentType.INVOICE)?.data
        val processDocument = giniCoreApi?.documentManager?.pollDocument(upload!!)
        val retrieveExtractions = giniCoreApi?.documentManager?.getAllExtractions(processDocument?.data!!)

        if (retrieveExtractions is Resource.Error) {
            Log.e("TEST", retrieveExtractions.responseBody ?: "")
        }

        Assert.assertTrue("extractions shouldn't have succeeded", retrieveExtractions is Resource.Error)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Throws(Exception::class)
    open fun publicKeyPinningWithMultiplePublicKeys() = runTest {
        TrustKitHelper.resetTrustKit()
        giniCoreApi = createGiniCoreAPIBuilder(clientId!!, clientSecret!!, "example.com")!!
            .setNetworkSecurityConfigResId(getNetworkSecurityConfigResId())?.setApiBaseUrl(apiUri!!)!!
            .setUserCenterApiBaseUrl(userCenterUri!!)!!.setConnectionTimeoutInMs(60000)!!.build()

        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentRemoteSource.Companion.DocumentType.INVOICE)
    }

    @Test
    @Throws(Exception::class)
    open fun createPartialDocument() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("multi-page-p1.png")
        Assert.assertNotNull("test image multi-page-p1.png could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)

        val documentCall = giniCoreApi?.documentManager?.createPartialDocument(testDocument, "image/png", null, null)
        Assert.assertNotNull(documentCall?.data)
    }

    @Test
    @Throws(Exception::class)
    open fun deletePartialDocumentWithoutParents() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("multi-page-p1.png")
        Assert.assertNotNull("test image multi-page-p1.png could not be loaded", testDocumentAsStream)

        val testDocument = TestUtils.createByteArray(testDocumentAsStream)

        val document = giniCoreApi?.documentManager?.createPartialDocument(testDocument, "image/png", null, null)?.data
        val deleteResult = giniCoreApi?.documentManager?.deleteDocument(document?.id!!)
        Assert.assertNotNull(deleteResult?.data)
    }

    @Test
    @Throws(Exception::class)
    open fun deletePartialDocumentWithParents() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val page1Stream = assetManager.open("multi-page-p1.png")
        Assert.assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream)

        val page1 = TestUtils.createByteArray(page1Stream)
        val partialDocument = AtomicReference<Document>()

        val document = giniCoreApi?.documentManager?.createPartialDocument(page1, "image/png", null, null)?.data
        partialDocument.set(document)
        val documentRotationDeltaMap = LinkedHashMap<Document, Int>()
        documentRotationDeltaMap[document!!] = 0
        val compositeDocument = giniCoreApi?.documentManager?.createCompositeDocument(documentRotationDeltaMap, null)?.data
        val deleteResource = giniCoreApi?.documentManager?.deletePartialDocumentAndParents(partialDocument.get().id)

        Assert.assertNotNull(deleteResource?.data)
    }

    @Test
    @Throws(Exception::class)
    open fun deletePartialDocumentFailsWhenNotDeletingParents() = runTest {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val page1Stream = assetManager.open("multi-page-p1.png")
        Assert.assertNotNull("test image multi-page-p1.png could not be loaded", page1Stream)

        val page1 = TestUtils.createByteArray(page1Stream)
        val partialDocument = AtomicReference<Document>()

        val document = giniCoreApi?.documentManager?.createPartialDocument(page1, "image/png", null, null)?.data
        partialDocument.set(document)
        val documentRotationDeltaMap = LinkedHashMap<Document, Int>()
        documentRotationDeltaMap[document!!] = 0
        val compositeDocument = giniCoreApi?.documentManager?.createCompositeDocument(documentRotationDeltaMap, null)?.data
        val deleteResource = giniCoreApi?.documentManager?.deleteDocument(partialDocument.get().id)

        Assert.assertTrue(deleteResource is Resource.Error)
    }

    // TODO: Implement these functions from old test class
    @Test
    @Throws(java.lang.Exception::class)
    open fun processCompositeDocument() {

    }

    @Test
    @Throws(java.lang.Exception::class)
    open fun testDeleteCompositeDocument() {

    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    open fun allowsUsingCustomTrustManager() {

    }

    @Throws(InterruptedException::class)
    protected suspend fun processDocument(documentBytes: ByteArray, contentType: String, filename: String, documentType: DocumentRemoteSource.Companion.DocumentType
    ): Map<Document, E> {
        return processDocument(documentBytes, contentType, filename, documentType,
            object : ExtractionsCallback<E> {
                override fun onExtractionsAvailable(extractionsContainer: E) {
                    Assert.assertEquals("IBAN should be found", "DE78370501980020008850", getIban(extractionsContainer)!!.value)
                    Assert.assertEquals("Amount to pay should be found", "1.00:EUR", getAmountToPay(extractionsContainer)!!.value)
                    Assert.assertEquals("BIC should be found", "COLSDE33", getBic(extractionsContainer)!!.value)
                    Assert.assertEquals("Payee should be found", "Uno Flüchtlingshilfe", getPaymentRecipient(extractionsContainer)!!.value)
                }
            })
    }

    protected abstract fun getIban(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getBic(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getAmountToPay(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getPaymentRecipient(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getPaymentPurpose(extractionsContainer: E): SpecificExtraction?

    @Throws(InterruptedException::class)
    protected suspend fun processDocument(documentBytes: ByteArray, contentType: String, filename: String, documentType: DocumentRemoteSource.Companion.DocumentType, extractionsCallback: ExtractionsCallback<E>): Map<Document, E> {
        val documentManager = giniCoreApi?.documentManager
        val uploadPartial = documentManager?.createPartialDocument(documentBytes, contentType, filename, documentType)
        val createComposite = documentManager?.createCompositeDocument(listOf(uploadPartial?.data!!), null)
        val processDocument = documentManager?.pollDocument(createComposite?.data!!)

        val retrieveExtractions = documentManager?.getAllExtractions(processDocument?.data!!)

        if (retrieveExtractions is Resource.Error) {
            Log.e("TEST", retrieveExtractions.responseBody ?: "")
        }

        Assert.assertFalse("extractions should have succeeded", retrieveExtractions is Resource.Error)
        retrieveExtractions?.data?.let { extractionsCallback.onExtractionsAvailable(it) }

        return Collections.singletonMap(createComposite?.data!!, retrieveExtractions?.data!!);
    }

    private fun getProperty(properties: Properties, propertyName: String): String {
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
}