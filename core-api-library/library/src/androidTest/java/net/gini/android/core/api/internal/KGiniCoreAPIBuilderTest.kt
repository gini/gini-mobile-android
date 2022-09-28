package net.gini.android.core.api.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.volley.Cache
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.*
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.test.TestGiniApiType
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.CoroutineContext

@SmallTest
@RunWith(AndroidJUnit4::class)
class KGiniCoreAPIBuilderTest {

    @Test
    fun testBuilderReturnsGiniInstance() {
        val builder = CoreAPIBuilder(ApplicationProvider.getApplicationContext(), "clientId", "clientSecret", "@example.com")
        Assert.assertNotNull(builder.build())
    }

    @Test
    fun testBuilderReturnsCorrectConfiguredGiniInstance() {
        val builder = CoreAPIBuilder(ApplicationProvider.getApplicationContext(), "clientId", "clientSecret", "@example.com")
        val giniCoreAPI: TestGiniCoreAPI = builder.build()
        Assert.assertNotNull(giniCoreAPI.documentManager)
        Assert.assertNotNull(giniCoreAPI.getCredentialsStore())
    }

    @Test
    fun testBuilderWorksWithAlternativeSessionManager() {
        val sessionManager: KSessionManager = NullSessionManager()
        val builder = CoreAPIBuilder(ApplicationProvider.getApplicationContext(), "clientId", "clientSecret", "@example.com", sessionManager)
        val giniCoreAPI: TestGiniCoreAPI = builder.build()
        Assert.assertNotNull(giniCoreAPI)
        Assert.assertNotNull(giniCoreAPI.documentManager)
        Assert.assertNotNull(giniCoreAPI.getCredentialsStore())
    }

    @Test
    fun testSetWrongConnectionTimeout() {
        val builder = CoreAPIBuilder(ApplicationProvider.getApplicationContext(), "clientId", "clientSecret", "@example.com")
        try {
            builder.setConnectionTimeoutInMs(-1)
            Assert.fail("IllegalArgumentException should be thrown")
        } catch (exc: IllegalArgumentException) {
        }
    }

    @Test
    fun testSetWrongConnectionMaxNumberOfRetries() {
        val builder = CoreAPIBuilder(ApplicationProvider.getApplicationContext(), "clientId", "clientSecret", "@example.com")
        try {
            builder.setMaxNumberOfRetries(-1)
            Assert.fail("IllegalArgumentException should be thrown")
        } catch (exc: java.lang.IllegalArgumentException) {
        }
    }

    @Test
    fun testSetWrongConnectionBackOffMultiplier() {
        val builder = CoreAPIBuilder(ApplicationProvider.getApplicationContext(), "clientId", "clientSecret", "@example.com")
        try {
            builder.setConnectionBackOffMultiplier(-1f)
            Assert.fail("IllegalArgumentException should be thrown")
        } catch (exc: java.lang.IllegalArgumentException) {
        }
    }

    class CoreAPIBuilder(
        private val context: Context,
        private val clientId: String,
        private val clientSecret: String,
        private val emailDomain: String,
        sessionManager: KSessionManager? = null
    ) : KGiniCoreAPIBuilder<TestDocumentManager, TestGiniCoreAPI, TestDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {
        override fun getGiniApiType(): GiniApiType {
            return TestGiniApiType()
        }

        override fun build(): TestGiniCoreAPI {
            return TestGiniCoreAPI(getDocumentManager(),getCredentialsStore())
        }

        override fun createDocumentManager(): TestDocumentManager {
            return TestDocumentManager(getDocumentRepository())
        }

        override fun createDocumentRepository(): TestDocumentRepository {
            return TestDocumentRepository(Dispatchers.IO, createDocumentRemoteSource(), getGiniApiType())
        }

        private fun createDocumentRemoteSource(): TestDocumentRemoteSource {
            return TestDocumentRemoteSource(Dispatchers.IO, getApiRetrofit().create(TestService::class.java), getGiniApiType(), getSessionManager(), getApiBaseUrl() ?: "")
        }
    }

    class TestDocumentManager(documentRepository: TestDocumentRepository) :
        DocumentManager<TestDocumentRepository, ExtractionsContainer>(documentRepository)

    class TestDocumentRepository(
        override val coroutineContext: CoroutineContext,
        documentRemoteSource: DocumentRemoteSource,
        giniApiType: GiniApiType
    ) : DocumentRepository<ExtractionsContainer>(coroutineContext, documentRemoteSource, giniApiType) {
        override fun createExtractionsContainer(specificExtractions: Map<String, SpecificExtraction>, compoundExtractions: Map<String, CompoundExtraction>, responseJSON: JSONObject
        ): ExtractionsContainer {
            return ExtractionsContainer(specificExtractions, compoundExtractions)
        }
    }

    class TestGiniCoreAPI(
        documentManager: TestDocumentManager,
        credentialsStore: CredentialsStore? = null
    ): KGiniCoreAPI<TestDocumentManager, TestDocumentRepository, ExtractionsContainer>(documentManager, credentialsStore)

    class TestDocumentRemoteSource(
        override val coroutineContext: CoroutineContext,
        private val documentService: DocumentService,
        private val giniApiType: GiniApiType,
        private val sessionManager: KSessionManager,
        baseUriString: String
    ): DocumentRemoteSource(coroutineContext, documentService, giniApiType, sessionManager, baseUriString)

    class NullSessionManager: KSessionManager {
        override suspend fun getSession(): Resource<SessionToken?> {
            return Resource.Error("NullSessionManager can't create sessions")
        }
    }

    interface TestService: DocumentService
}