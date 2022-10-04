package net.gini.android.core.api.test.shared

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.DocumentRepository
import net.gini.android.core.api.DocumentTaskManager
import net.gini.android.core.api.internal.GiniCoreAPI
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.test.shared.GiniCoreAPIIntegrationTest.ExtractionsCallback
import net.gini.android.core.api.test.shared.helpers.TestUtils
import net.gini.android.core.api.test.shared.helpers.TrustKitHelper
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*

@LargeTest
abstract class KGiniCoreAPIIntegrationTest<DM: DocumentManager<DR, E>, DR: DocumentRepository<E>, G: GiniCoreAPI<DM, DR, E>, E: ExtractionsContainer>{

    protected var giniCoreApi: G? = null
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
            ?.setUserCenterApiBaseUrl(userCenterUri!!)?.setConnectionTimeoutInMs(60000).build()

    }

    protected abstract fun createGiniCoreAPIBuilder(clientId: String, clientSecret: String, emailDomain: String): GiniCoreAPIBuilder<DM, G, DR, E>?

    @Test
    @Throws(IOException::class, InterruptedException::class, JSONException::class)
    open fun processDocumentByteArray() {
        val assetManager = ApplicationProvider.getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE)
    }

    @Throws(InterruptedException::class)
    protected open fun processDocument(
        documentBytes: ByteArray?,
        contentType: String?,
        filename: String?,
        documentType: DocumentTaskManager.DocumentType?
    ): Map<Document, E> {
        return processDocument(documentBytes, contentType, filename, documentType,
            ExtractionsCallback { extractionsContainer: E ->
                Assert.assertEquals("IBAN should be found", "DE78370501980020008850", getIban(extractionsContainer)!!.value)
                Assert.assertEquals("Amount to pay should be found", "1.00:EUR", getAmountToPay(extractionsContainer)!!.value)
                Assert.assertEquals("BIC should be found", "COLSDE33", getBic(extractionsContainer)!!.value)
                Assert.assertEquals("Payee should be found", "Uno Flüchtlingshilfe", getPaymentRecipient(extractionsContainer)!!.value)
            })
    }

    protected abstract fun getIban(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getBic(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getAmountToPay(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getPaymentRecipient(extractionsContainer: E): SpecificExtraction?

    protected abstract fun getPaymentPurpose(extractionsContainer: E): SpecificExtraction?

    private fun getProperty(properties: Properties, propertyName: String): String {
        val value = properties[propertyName]
        Assert.assertNotNull("$propertyName not set!", value)
        return value.toString()
    }
}