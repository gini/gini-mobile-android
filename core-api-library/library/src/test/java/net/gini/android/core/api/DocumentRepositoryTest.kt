package net.gini.android.core.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.http.GiniSessionInterceptor
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.Extraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.test.DocumentRemoteSourceForTests
import net.gini.android.core.api.test.TestDocumentService
import net.gini.android.core.api.test.WireTestGiniApiType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

/**
 * Behavior characterization tests for [DocumentRepository]: how the session manager's access
 * token is propagated, how session errors and cancellations map to [Resource] shapes, and the
 * mutex serialization of API calls.
 *
 * These pin down the observable behavior which must remain identical when the token handling
 * moves from `withAccessToken` into an OkHttp interceptor (PP-2363).
 */
@RunWith(AndroidJUnit4::class)
class DocumentRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun createRepository(sessionManager: SessionManager): TestDocumentRepository =
        TestDocumentRepository(createRemoteSource(sessionManager), sessionManager, WireTestGiniApiType())

    private fun createRemoteSource(sessionManager: SessionManager): DocumentRemoteSourceForTests {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            // Mirrors the production client composition (GiniCoreAPIBuilder): the session
            // interceptor authenticates the requests with the session manager's access token
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(GiniSessionInterceptor { sessionManager })
                    .build()
            )
            .build()
        return DocumentRemoteSourceForTests(
            Dispatchers.Unconfined,
            retrofit.create(TestDocumentService::class.java),
            WireTestGiniApiType(),
            server.url("/").toString()
        )
    }

    private fun successfulSessionManager(accessToken: String = ACCESS_TOKEN) =
        CountingSessionManager { Resource.Success(Session(accessToken, Date(Date().time + 60_000))) }

    // --- Token propagation ---

    @Test
    fun `access token from the session manager is sent as bearer authorization header`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson()))
        val repository = createRepository(successfulSessionManager("session-token-xyz"))

        val resource = repository.getDocument("document-id-13")

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer session-token-xyz")
    }

    @Test
    fun `getDocument maps the response json to a Document`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson()))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getDocument("document-id-13")

        val document = (resource as Resource.Success).data
        assertThat(document.id).isEqualTo("document-id-13")
        assertThat(document.state).isEqualTo(Document.ProcessingState.COMPLETED)
        assertThat(document.filename).isEqualTo("invoice.jpg")
        assertThat(document.pageCount).isEqualTo(1)
        assertThat(document.sourceClassification).isEqualTo(Document.SourceClassification.NATIVE)
    }

    // --- Session failure shapes (must stay identical after the interceptor refactoring) ---

    @Test
    fun `session manager error is returned as an error resource with the same fields and no request is sent`() = runTest {
        val repository = createRepository(CountingSessionManager {
            Resource.Error(
                message = "user authentication failed",
                responseStatusCode = 401,
                responseHeaders = mapOf("www-authenticate" to listOf("Bearer")),
                responseBody = """{"error":"invalid_grant"}"""
            )
        })

        val resource = repository.getDocument("document-id-13")

        val error = resource as Resource.Error
        assertThat(error.message).isEqualTo("user authentication failed")
        assertThat(error.responseStatusCode).isEqualTo(401)
        assertThat(error.responseHeaders).isEqualTo(mapOf("www-authenticate" to listOf("Bearer")))
        assertThat(error.responseBody).isEqualTo("""{"error":"invalid_grant"}""")
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `session manager cancellation is returned as a cancelled resource and no request is sent`() = runTest {
        val repository = createRepository(CountingSessionManager { Resource.Cancelled() })

        val resource = repository.getDocument("document-id-13")

        assertThat(resource).isInstanceOf(Resource.Cancelled::class.java)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `deprecated withAccessToken surfaces the session error and never runs the block`() = runTest {
        // Safety net for subclasses still calling the deprecated withAccessToken in
        // self-managed mode: they must receive the session manager's explanatory error
        // instead of a token. (That the builder's sentinel produces this error shape is
        // pinned in the bank api library's SelfManagedAuthenticationTest.)
        val repository = createRepository(CountingSessionManager {
            Resource.Error(message = "No session available: authentication is self-managed.")
        })

        var blockRan = false
        val resource = repository.callWithAccessToken {
            blockRan = true
            Resource.Success(Unit)
        }

        val error = resource as Resource.Error
        assertThat(error.message).isEqualTo("No session available: authentication is self-managed.")
        assertThat(blockRan).isFalse()
    }

    @Test
    fun `http error response is returned as an error resource with status code, body, parsed error and ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"message":"Document not found","requestId":"request-id-99"}""")
        )
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getDocument("document-id-13")

        val error = resource as Resource.Error
        assertThat(error.responseStatusCode).isEqualTo(404)
        assertThat(error.responseBody).isEqualTo("""{"message":"Document not found","requestId":"request-id-99"}""")
        assertThat(error.exception).isInstanceOf(ApiException::class.java)
        assertThat(error.errorResponse?.message).isEqualTo("Document not found")
        assertThat(error.errorResponse?.requestId).isEqualTo("request-id-99")
    }

    // --- Session manager call pattern ---

    @Test
    fun `every api call requests a session from the session manager`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson()))
        server.enqueue(MockResponse().setResponseCode(204))
        val sessionManager = successfulSessionManager()
        val repository = createRepository(sessionManager)

        repository.getDocument("document-id-13")
        repository.deleteDocument("document-id-13")

        assertThat(sessionManager.getSessionCallCount.get()).isEqualTo(2)
    }

    @Test
    fun `parallel api calls are serialized by the access token mutex`() = runTest {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson())) }
        val concurrentSessions = AtomicInteger(0)
        val maxConcurrentSessions = AtomicInteger(0)
        val sessionManager = CountingSessionManager {
            val current = concurrentSessions.incrementAndGet()
            maxConcurrentSessions.updateAndGet { max -> maxOf(max, current) }
            delay(100)
            concurrentSessions.decrementAndGet()
            Resource.Success(Session(ACCESS_TOKEN, Date(Date().time + 60_000)))
        }
        val repository = createRepository(sessionManager)

        val results = listOf(
            async { repository.getDocument("document-id-1") },
            async { repository.getDocument("document-id-2") },
            async { repository.getDocument("document-id-3") },
        ).map { it.await() }

        assertThat(results).hasSize(3)
        results.forEach { assertThat(it).isInstanceOf(Resource.Success::class.java) }
        // The accessTokenMutex guarantees only one session request runs at a time. This prevents
        // creating multiple anonymous users on first use with parallel uploads.
        assertThat(maxConcurrentSessions.get()).isEqualTo(1)
        assertThat(sessionManager.getSessionCallCount.get()).isEqualTo(3)
    }

    // --- Extractions parsing ---

    @Test
    fun `getAllExtractions parses specific extractions, candidates and compound extractions`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(EXTRACTIONS_JSON))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getAllExtractions(document())

        val container = (resource as Resource.Success).data
        val amountToPay = container.specificExtractions["amountToPay"]
        assertThat(amountToPay).isNotNull()
        assertThat(amountToPay!!.value).isEqualTo("335.50:EUR")
        assertThat(amountToPay.entity).isEqualTo("amount")
        assertThat(amountToPay.box).isNotNull()
        assertThat(amountToPay.box!!.pageNumber).isEqualTo(1)
        assertThat(amountToPay.candidate).hasSize(2)

        val lineItems = container.compoundExtractions["lineItems"]
        assertThat(lineItems).isNotNull()
        assertThat(lineItems!!.specificExtractionMaps).hasSize(1)
        assertThat(lineItems.specificExtractionMaps[0]["description"]?.value).isEqualTo("Shoes")
    }

    @Test
    fun `overridden extractionFromApiResponse is applied to specific, compound and candidate extractions`() = runTest {
        // Pins the contract that justified plumbing extractionFromApiResponse into the parser
        // as a function parameter: subclasses customizing how a single extraction is parsed
        // must see their override applied everywhere extractions are created.
        server.enqueue(MockResponse().setResponseCode(200).setBody(EXTRACTIONS_JSON))
        val sessionManager = successfulSessionManager()
        val repository = TaggingDocumentRepository(
            createRemoteSource(sessionManager), sessionManager, WireTestGiniApiType()
        )

        val resource = repository.getAllExtractions(document())

        val container = (resource as Resource.Success).data
        val amountToPay = container.specificExtractions["amountToPay"]!!
        assertThat(amountToPay.entity).isEqualTo("amount-tagged")
        amountToPay.candidate.forEach { candidate ->
            assertThat(candidate.entity).isEqualTo("amount-tagged")
        }
        val description = container.compoundExtractions["lineItems"]!!.specificExtractionMaps[0]["description"]!!
        assertThat(description.entity).isEqualTo("text-tagged")
    }

    // --- Composite document JSON (request body the client asked a public mapper for) ---

    @Test
    fun `createCompositeDocument sends partial document uris and normalized rotations as composite json`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Location", server.url("/documents/composite-1").toString())
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson("composite-1")))
        val repository = createRepository(successfulSessionManager())

        val documentRotationMap = linkedMapOf(
            document("partial-1") to 450,
            document("partial-2") to -90
        )
        val resource = repository.createCompositeDocument(documentRotationMap, null)

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val uploadRequest = server.takeRequest()
        assertThat(uploadRequest.method).isEqualTo("POST")
        assertThat(uploadRequest.getHeader("Content-Type"))
            .isEqualTo("application/vnd.gini.v1.composite+json")

        val body = JSONObject(uploadRequest.body.readUtf8())
        val partialDocuments = body.getJSONArray("partialDocuments")
        assertThat(partialDocuments.length()).isEqualTo(2)
        assertThat(partialDocuments.getJSONObject(0).getString("document"))
            .isEqualTo("https://api.gini.net/documents/partial-1")
        // Rotations are normalized to 0-359 degrees
        assertThat(partialDocuments.getJSONObject(0).getInt("rotationDelta")).isEqualTo(90)
        assertThat(partialDocuments.getJSONObject(1).getInt("rotationDelta")).isEqualTo(270)

        // The composite document is fetched from the returned location header
        val getRequest = server.takeRequest()
        assertThat(getRequest.method).isEqualTo("GET")
        assertThat(getRequest.path).isEqualTo("/documents/composite-1")
        assertThat((resource as Resource.Success).data.id).isEqualTo("composite-1")
    }

    @Test
    fun `createPartialDocument sends the partial media type derived from the content type`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Location", server.url("/documents/partial-1").toString())
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson("partial-1")))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.createPartialDocument(byteArrayOf(1, 2, 3), "image/jpeg")

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val uploadRequest = server.takeRequest()
        assertThat(uploadRequest.getHeader("Content-Type"))
            .isEqualTo("application/vnd.gini.v1.partial+jpeg")
    }

    // Note: the feedback request body is characterized in the bank and health api library test
    // suites because core's DocumentService.sendFeedback has no HTTP endpoint (bank/health
    // override it with their own annotated endpoints).

    // --- Polling ---

    @Test
    fun `pollDocument polls until the document processing is no longer pending`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson(progress = "PENDING")))
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson(progress = "COMPLETED")))
        val repository = createRepository(successfulSessionManager())

        val pendingDocument = Document.fromApiResponse(JSONObject(documentJson(progress = "PENDING")))
        val resource = repository.pollDocument(pendingDocument)

        val document = (resource as Resource.Success).data
        assertThat(document.state).isEqualTo(Document.ProcessingState.COMPLETED)
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `pollDocument returns the document without requests when it is not pending`() = runTest {
        val repository = createRepository(successfulSessionManager())

        val completedDocument = document()
        val resource = repository.pollDocument(completedDocument)

        assertThat((resource as Resource.Success).data).isSameInstanceAs(completedDocument)
        assertThat(server.requestCount).isEqualTo(0)
    }

    // --- Request/response mapping of the remaining repository methods ---

    @Test
    fun `deletePartialDocumentAndParents deletes the composite parents and then the partial document`() = runTest {
        val compositeUri = server.url("/documents/composite-id-1").toString()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                documentJson(id = "partial-id-1", compositeDocuments = listOf(compositeUri))
            )
        )
        repeat(2) { server.enqueue(MockResponse().setResponseCode(204)) }
        val repository = createRepository(successfulSessionManager())

        val resource = repository.deletePartialDocumentAndParents("partial-id-1")

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        assertThat(server.takeRequest().path).isEqualTo("/documents/partial-id-1")
        val deleteParent = server.takeRequest()
        assertThat(deleteParent.method).isEqualTo("DELETE")
        assertThat(deleteParent.path).isEqualTo("/documents/composite-id-1")
        val deletePartial = server.takeRequest()
        assertThat(deletePartial.method).isEqualTo("DELETE")
        assertThat(deletePartial.path).isEqualTo("/documents/partial-id-1")
    }

    @Test
    fun `createCompositeDocument from a document list defaults every rotation to zero`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Location", server.url("/documents/composite-id-7"))
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson(id = "composite-id-7")))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.createCompositeDocument(listOf(document("partial-id-1")), null, null)

        assertThat((resource as Resource.Success).data.id).isEqualTo("composite-id-7")
        val uploadBody = JSONObject(server.takeRequest().body.readUtf8())
        val partialDocuments = uploadBody.getJSONArray("partialDocuments")
        assertThat(partialDocuments.length()).isEqualTo(1)
        assertThat(partialDocuments.getJSONObject(0).getString("document"))
            .isEqualTo("https://api.gini.net/documents/partial-id-1")
        assertThat(partialDocuments.getJSONObject(0).getInt("rotationDelta")).isEqualTo(0)
    }

    @Test
    fun `getDocument by uri fetches the document from the uri`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(documentJson()))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getDocument(Uri.parse(server.url("/documents/document-id-13").toString()))

        assertThat((resource as Resource.Success).data.id).isEqualTo("document-id-13")
        assertThat(server.takeRequest().path).isEqualTo("/documents/document-id-13")
    }

    @Test
    fun `sendFeedbackForExtractions sends the extraction values as feedback`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.sendFeedbackForExtractions(document(), mapOf("amountToPay" to specificExtraction()))

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertThat(
            body.getJSONObject("feedback").getJSONObject("amountToPay").getString("value")
        ).isEqualTo("335.50:EUR")
    }

    @Test
    fun `sendFeedbackWithSpecificExtractions sends the extraction values as extractions`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = createRepository(successfulSessionManager())

        val resource =
            repository.sendFeedbackWithSpecificExtractions(document(), mapOf("amountToPay" to specificExtraction()))

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertThat(
            body.getJSONObject("extractions").getJSONObject("amountToPay").getString("value")
        ).isEqualTo("335.50:EUR")
    }

    @Test
    fun `sendFeedbackForExtractions sends specific and compound extraction feedback`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.sendFeedbackForExtractions(
            document(),
            mapOf("amountToPay" to specificExtraction()),
            mapOf("lineItems" to CompoundExtraction("lineItems", listOf(mapOf("description" to specificExtraction()))))
        )

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertThat(body.has("extractions")).isTrue()
        assertThat(body.has("compoundExtractions")).isTrue()
    }

    @Test
    fun `getDocumentLayout maps the layout response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"pages":[{"number":1,"sizeX":595.0,"sizeY":842.0,"textZones":[],"regions":[]}]}"""
            )
        )
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getDocumentLayout("document-id-13")

        val layout = (resource as Resource.Success).data
        assertThat(layout.pages).hasSize(1)
        assertThat(layout.pages.first().number).isEqualTo(1)
    }

    @Test
    fun `getDocumentPages maps the page responses`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"pageNumber":1,"images":{"medium":"https://api.gini.net/image/medium","large":null}}]"""
            )
        )
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getDocumentPages("document-id-13")

        val pages = (resource as Resource.Success).data
        assertThat(pages).hasSize(1)
        assertThat(pages.first().pageNumber).isEqualTo(1)
    }

    @Test
    fun `getFile returns the raw response bytes`() = runTest {
        val fileBytes = byteArrayOf(1, 2, 3, 4)
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(fileBytes)))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getFile(server.url("/files/file-1").toString())

        assertThat((resource as Resource.Success).data).isEqualTo(fileBytes)
    }

    @Test
    fun `getPaymentRequest maps the payment request response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(paymentRequestJson()))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getPaymentRequest("payment-request-id-1")

        val paymentRequest = (resource as Resource.Success).data
        assertThat(paymentRequest.recipient).isEqualTo("Dr. Fake")
        assertThat(paymentRequest.status).isEqualTo(PaymentRequest.Status.OPEN)
    }

    @Test
    fun `getPaymentRequests maps the payment request list response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[${paymentRequestJson()}]"))
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getPaymentRequests()

        val paymentRequests = (resource as Resource.Success).data
        assertThat(paymentRequests).hasSize(1)
        assertThat(paymentRequests.first().iban).isEqualTo("DE02300209000106531065")
    }

    @Test
    fun `getPayment maps the payment response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "paidAt": "2024-07-30T10:20:30",
                    "recipient": "Dr. Fake",
                    "iban": "DE02300209000106531065",
                    "amount": "335.50:EUR",
                    "purpose": "invoice 1"
                }
                """.trimIndent()
            )
        )
        val repository = createRepository(successfulSessionManager())

        val resource = repository.getPayment("payment-id-1")

        val payment = (resource as Resource.Success).data
        assertThat(payment.recipient).isEqualTo("Dr. Fake")
        assertThat(payment.paidAt).isEqualTo("2024-07-30T10:20:30")
    }

    // --- Helpers ---

    private class CountingSessionManager(
        private val provideSession: suspend () -> Resource<Session>
    ) : SessionManager {
        val getSessionCallCount = AtomicInteger(0)
        override suspend fun getSession(): Resource<Session> {
            getSessionCallCount.incrementAndGet()
            return provideSession()
        }
    }

    private class TestDocumentRepository(
        remoteSource: DocumentRemoteSource,
        sessionManager: SessionManager,
        apiType: GiniApiType
    ) : DocumentRepository<ExtractionsContainer>(remoteSource, sessionManager, apiType) {

        override fun createExtractionsContainer(
            specificExtractions: Map<String, SpecificExtraction>,
            compoundExtractions: Map<String, CompoundExtraction>,
            responseJSON: JSONObject
        ): ExtractionsContainer = ExtractionsContainer(specificExtractions, compoundExtractions)

        // Exposes the protected deprecated withAccessToken for pinning its error contract
        @Suppress("DEPRECATION")
        suspend fun callWithAccessToken(block: suspend (String) -> Resource<Unit>): Resource<Unit> =
            withAccessToken { accessToken -> block(accessToken) }
    }

    // A consumer subclass customizing how a single extraction is parsed
    private class TaggingDocumentRepository(
        remoteSource: DocumentRemoteSource,
        sessionManager: SessionManager,
        apiType: GiniApiType
    ) : DocumentRepository<ExtractionsContainer>(remoteSource, sessionManager, apiType) {

        override fun createExtractionsContainer(
            specificExtractions: Map<String, SpecificExtraction>,
            compoundExtractions: Map<String, CompoundExtraction>,
            responseJSON: JSONObject
        ): ExtractionsContainer = ExtractionsContainer(specificExtractions, compoundExtractions)

        override fun extractionFromApiResponse(responseData: JSONObject): Extraction {
            val extraction = super.extractionFromApiResponse(responseData)
            return Extraction(extraction.value, extraction.entity + "-tagged", extraction.box)
        }
    }

    private fun document(id: String = "document-id-13") =
        Document.fromApiResponse(JSONObject(documentJson(id)))

    private fun documentJson(
        id: String = "document-id-13",
        progress: String = "COMPLETED",
        compositeDocuments: List<String> = emptyList()
    ) = """
        {
            "id": "$id",
            "progress": "$progress",
            "pageCount": 1,
            "name": "invoice.jpg",
            "creationDate": 1515932941283,
            "sourceClassification": "NATIVE",
            "compositeDocuments": [${compositeDocuments.joinToString(",") { """{"document": "$it"}""" }}],
            "_links": {
                "document": "https://api.gini.net/documents/$id",
                "extractions": "https://api.gini.net/documents/$id/extractions"
            }
        }
    """.trimIndent()

    private fun specificExtraction() =
        SpecificExtraction("amountToPay", "335.50:EUR", "amount", null, emptyList())

    private fun paymentRequestJson() = """
        {
            "paymentProvider": "payment-provider-id-1",
            "requesterUri": "https://requester.example.com",
            "recipient": "Dr. Fake",
            "iban": "DE02300209000106531065",
            "amount": "335.50:EUR",
            "purpose": "invoice 1",
            "status": "open",
            "createdAt": "2024-07-30T10:20:30",
            "expirationDate": "2024-08-30T10:20:30"
        }
    """.trimIndent()

    companion object {
        private const val ACCESS_TOKEN = "test-access-token-1234"

        private val EXTRACTIONS_JSON = """
            {
                "extractions": {
                    "amountToPay": {
                        "box": { "height": 9.0, "left": 516.0, "page": 1, "top": 588.0, "width": 42.0 },
                        "entity": "amount",
                        "value": "335.50:EUR",
                        "candidates": "amounts"
                    }
                },
                "candidates": {
                    "amounts": [
                        {
                            "box": { "height": 9.0, "left": 516.0, "page": 1, "top": 588.0, "width": 42.0 },
                            "entity": "amount",
                            "value": "335.50:EUR"
                        },
                        {
                            "box": { "height": 9.0, "left": 241.0, "page": 1, "top": 588.0, "width": 42.0 },
                            "entity": "amount",
                            "value": "23.00:EUR"
                        }
                    ]
                },
                "compoundExtractions": {
                    "lineItems": [
                        {
                            "description": {
                                "entity": "text",
                                "value": "Shoes"
                            }
                        }
                    ]
                }
            }
        """.trimIndent()
    }
}
