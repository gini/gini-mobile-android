package net.gini.android.core.api.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.Dispatchers
import net.gini.android.core.api.*
import net.gini.android.core.api.authorization.CredentialsStore
import net.gini.android.core.api.authorization.SessionManager
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
class GiniCoreAPIBuilderTest {

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
        val sessionManager: SessionManager = NullSessionManager()
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

    class CoreAPIBuilder(
        private val context: Context,
        private val clientId: String,
        private val clientSecret: String,
        private val emailDomain: String,
        sessionManager: SessionManager? = null
    ) : GiniCoreAPIBuilder<TestDocumentManager, TestGiniCoreAPI, TestDocumentRepository, ExtractionsContainer>(context, clientId, clientSecret, emailDomain, sessionManager) {
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
            return TestDocumentRepository(createDocumentRemoteSource(), getSessionManager(), getGiniApiType())
        }

        private fun createDocumentRemoteSource(): TestDocumentRemoteSource {
            return TestDocumentRemoteSource(Dispatchers.IO, getApiRetrofit().create(TestService::class.java), getGiniApiType(), getApiBaseUrl() ?: "")
        }
    }

    class TestDocumentManager(documentRepository: TestDocumentRepository) :
        DocumentManager<TestDocumentRepository, ExtractionsContainer>(documentRepository)

    class TestDocumentRepository(
        documentRemoteSource: DocumentRemoteSource,
        sessionManager: SessionManager,
        giniApiType: GiniApiType
    ) : DocumentRepository<ExtractionsContainer>(documentRemoteSource, sessionManager, giniApiType) {
        override fun createExtractionsContainer(specificExtractions: Map<String, SpecificExtraction>, compoundExtractions: Map<String, CompoundExtraction>, responseJSON: JSONObject
        ): ExtractionsContainer {
            return ExtractionsContainer(specificExtractions, compoundExtractions)
        }
    }

    class TestGiniCoreAPI(
        documentManager: TestDocumentManager,
        credentialsStore: CredentialsStore? = null
    ): GiniCoreAPI<TestDocumentManager, TestDocumentRepository, ExtractionsContainer>(documentManager, credentialsStore)

    class TestDocumentRemoteSource(
        override val coroutineContext: CoroutineContext,
        private val documentService: DocumentService,
        private val giniApiType: GiniApiType,
        baseUriString: String
    ): DocumentRemoteSource(coroutineContext, documentService, giniApiType, baseUriString)

    class NullSessionManager: SessionManager {
        override suspend fun getSession(): Resource<SessionToken> {
            return Resource.Error("NullSessionManager can't create sessions")
        }
    }

    interface TestService: DocumentService
}