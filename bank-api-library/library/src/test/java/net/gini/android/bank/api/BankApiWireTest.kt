package net.gini.android.bank.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.gini.android.bank.api.models.AmplitudeEvent
import net.gini.android.bank.api.models.AmplitudeRoot
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.SpecificExtraction
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date

/**
 * Wire-level characterization tests for the Bank API specific endpoints, exercised through
 * [BankApiDocumentRepository] with a real Retrofit + OkHttp stack against a local [MockWebServer].
 *
 * They pin down the exact requests (method, path, headers, body) and response mappings which
 * must remain identical while the token handling is moved into an OkHttp interceptor (PP-2363).
 */
@RunWith(AndroidJUnit4::class)
class BankApiWireTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: BankApiDocumentRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiType = GiniBankApiType(apiVersion = 1)
        val sessionManager = SessionManager {
            Resource.Success(Session(ACCESS_TOKEN, Date(Date().time + 60_000)))
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            // Stands in for the production session interceptor (GiniSessionInterceptor,
            // internal to core and tested there): authenticates the requests with the
            // access token, which is all these wire tests need from the client composition.
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("Authorization", "Bearer $ACCESS_TOKEN")
                                .build()
                        )
                    }
                    .build()
            )
            .build()
        val remoteSource = BankApiDocumentRemoteSource(
            Dispatchers.Unconfined,
            retrofit.create(BankApiDocumentService::class.java),
            apiType,
            server.url("/").toString()
        )
        val trackingAnalysisRemoteSource = TrackingAnalysisRemoteSource(
            Dispatchers.Unconfined,
            retrofit.create(TrackingAnalysisService::class.java)
        )
        repository = BankApiDocumentRepository(remoteSource, sessionManager, apiType, trackingAnalysisRemoteSource)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `resolvePaymentRequest sends POST with payment details as json body and parses the resolved payment`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(RESOLVE_PAYMENT_RESPONSE_JSON))

        val resource = repository.resolvePaymentRequest(
            "payment-request-id-42",
            ResolvePaymentInput(
                recipient = "Dr. Test GmbH",
                iban = "DE02300209000106531065",
                amount = "335.50:EUR",
                purpose = "Invoice 123",
                bic = "CMCIDEDD"
            )
        )

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-42/payment")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        val body = JSONObject(request.body.readUtf8())
        assertThat(body.getString("recipient")).isEqualTo("Dr. Test GmbH")
        assertThat(body.getString("iban")).isEqualTo("DE02300209000106531065")
        assertThat(body.getString("bic")).isEqualTo("CMCIDEDD")
        assertThat(body.getString("amount")).isEqualTo("335.50:EUR")
        assertThat(body.getString("purpose")).isEqualTo("Invoice 123")

        val resolvedPayment = (resource as Resource.Success).data
        assertThat(resolvedPayment.requesterUri).isEqualTo("https://requester.example.org")
        assertThat(resolvedPayment.recipient).isEqualTo("Dr. Test GmbH")
        assertThat(resolvedPayment.status).isEqualTo(ResolvedPayment.Status.PAID)
    }

    @Test
    fun `resolvePaymentRequest maps unknown payment status to INVALID`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(RESOLVE_PAYMENT_RESPONSE_JSON.replace("\"paid\"", "\"something-new\""))
        )

        val resource = repository.resolvePaymentRequest(
            "payment-request-id-42",
            ResolvePaymentInput("Dr. Test GmbH", "DE02300209000106531065", "335.50:EUR", "Invoice 123")
        )

        assertThat((resource as Resource.Success).data.status).isEqualTo(ResolvedPayment.Status.INVALID)
    }

    @Test
    fun `logErrorEvent sends POST with snake case json body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val resource = repository.logErrorEvent(
            ErrorEvent(
                deviceModel = "Pixel 8",
                osName = "Android",
                osVersion = "14",
                captureSdkVersion = "3.4.0",
                apiLibVersion = "4.3.0",
                description = "Something went wrong",
                documentId = "document-id-13",
                originalRequestId = "request-id-99"
            )
        )

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/events/error")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")

        val body = JSONObject(request.body.readUtf8())
        assertThat(body.getString("device_model")).isEqualTo("Pixel 8")
        assertThat(body.getString("os_name")).isEqualTo("Android")
        assertThat(body.getString("os_version")).isEqualTo("14")
        assertThat(body.getString("capture_sdk_version")).isEqualTo("3.4.0")
        assertThat(body.getString("api_lib_version")).isEqualTo("4.3.0")
        assertThat(body.getString("description")).isEqualTo("Something went wrong")
        assertThat(body.getString("document_id")).isEqualTo("document-id-13")
        assertThat(body.getString("original_request_id")).isEqualTo("request-id-99")
    }

    @Test
    fun `getConfigurations sends GET and maps the configuration response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(CONFIGURATION_JSON))

        val resource = repository.getConfigurations()

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/configurations")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")

        val configuration = (resource as Resource.Success).data
        assertThat(configuration.clientID).isEqualTo("client-id-1")
        assertThat(configuration.isUserJourneyAnalyticsEnabled).isTrue()
        assertThat(configuration.isSkontoEnabled).isTrue()
        assertThat(configuration.isReturnAssistantEnabled).isFalse()
        assertThat(configuration.amplitudeApiKey).isEqualTo("amplitude-key-1")
    }

    @Test
    fun `getConfigurations maps missing configuration fields to defaults`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"clientID":"client-id-1"}"""))

        val resource = repository.getConfigurations()

        val configuration = (resource as Resource.Success).data
        assertThat(configuration.clientID).isEqualTo("client-id-1")
        // Missing booleans default to false, missing amplitude key stays null
        assertThat(configuration.isUserJourneyAnalyticsEnabled).isFalse()
        assertThat(configuration.isSkontoEnabled).isFalse()
        assertThat(configuration.amplitudeApiKey).isNull()
    }

    @Test
    fun `sendEvents sends POST with amplitude media type and snake case events body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val resource = repository.sendEvents(
            AmplitudeRoot(
                apiKey = "amplitude-key-1",
                events = listOf(
                    AmplitudeEvent(
                        userId = "user-1",
                        deviceId = "device-1",
                        eventType = "screen_shown",
                        sessionId = "1234",
                        eventId = "1",
                        time = 1700000000000,
                        platform = "Android",
                        osVersion = "14",
                        deviceManufacturer = "Google",
                        deviceBrand = "google",
                        deviceModel = "Pixel 8",
                        versionName = "3.4.0",
                        osName = "android",
                        carrier = "Test Carrier",
                        language = "en"
                    )
                )
            )
        )

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/events/batch")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1.events.amplitude")
        assertThat(request.getHeader("Accept")).isEqualTo("application/json")

        val body = JSONObject(request.body.readUtf8())
        val event = body.getJSONArray("events").getJSONObject(0)
        assertThat(event.getString("user_id")).isEqualTo("user-1")
        assertThat(event.getString("device_id")).isEqualTo("device-1")
        assertThat(event.getString("event_type")).isEqualTo("screen_shown")
        assertThat(event.getLong("time")).isEqualTo(1700000000000)
        assertThat(event.getString("ip")).isEqualTo("\$remote")
    }

    @Test
    fun `sendFeedbackForExtractions sends POST with feedback json body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val extraction = SpecificExtraction("amountToPay", "335.50:EUR", "amount", null, emptyList())
        val resource = repository.sendFeedbackForExtractions(document(), mapOf("amountToPay" to extraction))

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/documents/document-id-13/extractions/feedback")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        val body = JSONObject(request.body.readUtf8())
        val feedback = body.getJSONObject("feedback")
        assertThat(feedback.getJSONObject("amountToPay").getString("value")).isEqualTo("335.50:EUR")
        assertThat(feedback.getJSONObject("amountToPay").getString("entity")).isEqualTo("amount")
    }

    @Test
    fun `getAllExtractions parses bank specific return reasons`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(EXTRACTIONS_WITH_RETURN_REASONS_JSON))

        val resource = repository.getAllExtractions(document())

        val container = (resource as Resource.Success).data
        assertThat(container.specificExtractions["amountToPay"]?.value).isEqualTo("335.50:EUR")
        assertThat(container.returnReasons).hasSize(1)
        assertThat(container.returnReasons[0].id).isEqualTo("r1")
        assertThat(container.returnReasons[0].localizedLabels["de"]).isEqualTo("Beschädigt")
    }

    private fun document(id: String = "document-id-13") = Document.fromApiResponse(
        JSONObject(
            """
            {
                "id": "$id",
                "progress": "COMPLETED",
                "pageCount": 1,
                "name": "invoice.jpg",
                "creationDate": 1515932941283,
                "sourceClassification": "NATIVE",
                "_links": {
                    "document": "https://pay-api.gini.net/documents/$id",
                    "extractions": "https://pay-api.gini.net/documents/$id/extractions"
                }
            }
            """.trimIndent()
        )
    )

    companion object {
        private const val ACCESS_TOKEN = "test-access-token-1234"

        private val RESOLVE_PAYMENT_RESPONSE_JSON = """
            {
                "requesterUri": "https://requester.example.org",
                "recipient": "Dr. Test GmbH",
                "iban": "DE02300209000106531065",
                "bic": "CMCIDEDD",
                "amount": "335.50:EUR",
                "purpose": "Invoice 123",
                "status": "paid"
            }
        """.trimIndent()

        private val CONFIGURATION_JSON = """
            {
                "clientID": "client-id-1",
                "userJourneyAnalyticsEnabled": true,
                "skontoEnabled": true,
                "returnAssistantEnabled": false,
                "amplitudeApiKey": "amplitude-key-1",
                "transactionDocsEnabled": true,
                "qrCodeEducationEnabled": false,
                "instantPaymentEnabled": true,
                "eInvoiceEnabled": false,
                "alreadyPaidHintEnabled": true,
                "paymentDueHintEnabled": false,
                "savePhotosLocallyEnabled": true,
                "unsupportedQRCodeWarningEnabled": false
            }
        """.trimIndent()

        private val EXTRACTIONS_WITH_RETURN_REASONS_JSON = """
            {
                "extractions": {
                    "amountToPay": {
                        "entity": "amount",
                        "value": "335.50:EUR"
                    }
                },
                "candidates": {},
                "returnReasons": [
                    {
                        "id": "r1",
                        "de": "Beschädigt"
                    }
                ]
            }
        """.trimIndent()
    }
}
