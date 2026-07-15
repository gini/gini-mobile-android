package net.gini.android.health.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.response.CommunicationTone
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Date

/**
 * Wire-level characterization tests for the Health API specific endpoints, exercised through
 * [HealthApiDocumentRepository] with a real Retrofit + OkHttp stack against a local [MockWebServer].
 *
 * They pin down the exact requests (method, path, headers, body) and response mappings which
 * must remain identical while the token handling is moved into an OkHttp interceptor (PP-2363).
 */
@RunWith(AndroidJUnit4::class)
class HealthApiWireTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: HealthApiDocumentRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiType = GiniHealthApiType(apiVersion = 3)
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .client(OkHttpClient())
            .build()
        val remoteSource = HealthApiDocumentRemoteSource(
            Dispatchers.Unconfined,
            retrofit.create(HealthApiDocumentService::class.java),
            apiType,
            server.url("/").toString()
        )
        val sessionManager = SessionManager {
            Resource.Success(Session(ACCESS_TOKEN, Date(Date().time + 60_000)))
        }
        repository = HealthApiDocumentRepository(remoteSource, sessionManager, apiType)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `createPaymentRequest sends POST with payment details as json body and returns the id from the location header`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Location", "https://health-api.gini.net/paymentRequests/payment-request-id-77")
        )

        val resource = repository.createPaymentRequest(
            PaymentRequestInput(
                paymentProvider = "payment-provider-id-1",
                recipient = "Dr. Test GmbH",
                iban = "DE02300209000106531065",
                amount = "335.50:EUR",
                purpose = "Invoice 123",
                bic = "CMCIDEDD",
                sourceDocumentLocation = "https://health-api.gini.net/documents/document-id-13"
            )
        )

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/paymentRequests")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v3+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v3+json")

        val body = JSONObject(request.body.readUtf8())
        assertThat(body.getString("paymentProvider")).isEqualTo("payment-provider-id-1")
        assertThat(body.getString("recipient")).isEqualTo("Dr. Test GmbH")
        assertThat(body.getString("iban")).isEqualTo("DE02300209000106531065")
        assertThat(body.getString("amount")).isEqualTo("335.50:EUR")
        assertThat(body.getString("purpose")).isEqualTo("Invoice 123")
        assertThat(body.getString("bic")).isEqualTo("CMCIDEDD")
        assertThat(body.getString("sourceDocumentLocation"))
            .isEqualTo("https://health-api.gini.net/documents/document-id-13")

        assertThat((resource as Resource.Success).data).isEqualTo("payment-request-id-77")
    }

    @Test
    fun `createPaymentRequest omits null optional fields from the json body`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Location", "https://health-api.gini.net/paymentRequests/payment-request-id-77")
        )

        repository.createPaymentRequest(
            PaymentRequestInput(
                paymentProvider = "payment-provider-id-1",
                recipient = "Dr. Test GmbH",
                iban = "DE02300209000106531065",
                amount = "335.50:EUR",
                purpose = "Invoice 123"
            )
        )

        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertThat(body.has("bic")).isFalse()
        assertThat(body.has("sourceDocumentLocation")).isFalse()
    }

    @Test
    fun `getPaymentProviders filters out disabled providers and fetches their icons`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/paymentProviders" -> MockResponse().setResponseCode(200).setBody(PAYMENT_PROVIDERS_JSON)
                "/icons/provider-1.png" -> MockResponse().setResponseCode(200)
                    .setBody(okio.Buffer().write(ICON_BYTES))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val resource = repository.getPaymentProviders()

        val providers = (resource as Resource.Success).data
        // provider-2 has no supported android platforms and is filtered out
        assertThat(providers).hasSize(1)
        val provider = providers[0]
        assertThat(provider.id).isEqualTo("provider-1")
        assertThat(provider.name).isEqualTo("Test Bank")
        assertThat(provider.packageName).isEqualTo("net.example.testbank")
        assertThat(provider.appVersion).isEqualTo("3.5.1")
        assertThat(provider.colors.backgroundColorRGBHex).isEqualTo("112233")
        assertThat(provider.colors.textColoRGBHex).isEqualTo("ffffff")
        assertThat(provider.icon).isEqualTo(ICON_BYTES)
        assertThat(provider.playStoreUrl).isEqualTo("https://play.google.com/store/apps/details?id=net.example.testbank")

        val providersRequest = server.takeRequest()
        assertThat(providersRequest.method).isEqualTo("GET")
        assertThat(providersRequest.path).isEqualTo("/paymentProviders")
        assertThat(providersRequest.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")

        val iconRequest = server.takeRequest()
        assertThat(iconRequest.method).isEqualTo("GET")
        assertThat(iconRequest.path).isEqualTo("/icons/provider-1.png")
        assertThat(iconRequest.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
    }

    @Test
    fun `getPaymentProvider fetches a single provider with its icon`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/paymentProviders/provider-1" -> MockResponse().setResponseCode(200).setBody(PAYMENT_PROVIDER_JSON)
                "/icons/provider-1.png" -> MockResponse().setResponseCode(200)
                    .setBody(okio.Buffer().write(ICON_BYTES))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val resource = repository.getPaymentProvider("provider-1")

        val provider = (resource as Resource.Success).data
        assertThat(provider.id).isEqualTo("provider-1")
        assertThat(provider.icon).isEqualTo(ICON_BYTES)
    }

    @Test
    fun `getConfigurations sends GET and parses the configuration response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "clientID": "client-id-1",
                    "communicationTone": "INFORMAL",
                    "ingredientBrandType": "PAYMENT_COMPONENT"
                }
                """.trimIndent()
            )
        )

        val resource = repository.getConfigurations()

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/configurations")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v3+json")

        val configuration = (resource as Resource.Success).data
        assertThat(configuration.clientId).isEqualTo("client-id-1")
        assertThat(configuration.communicationTone).isEqualTo(CommunicationTone.INFORMAL)
        assertThat(configuration.ingredientBrandType).isEqualTo("PAYMENT_COMPONENT")
    }

    @Test
    fun `deletePaymentRequest sends DELETE to the payment request path`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val resource = repository.deletePaymentRequest("payment-request-id-77")

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("DELETE")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-77")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
    }

    @Test
    fun `deleteDocuments sends DELETE with the document ids as json array body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val resource = repository.deleteDocuments(listOf("document-id-1", "document-id-2"))

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("DELETE")
        assertThat(request.path).isEqualTo("/documents")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $ACCESS_TOKEN")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v3+json")

        val body = JSONArray(request.body.readUtf8())
        assertThat(body.length()).isEqualTo(2)
        assertThat(body.getString(0)).isEqualTo("document-id-1")
        assertThat(body.getString(1)).isEqualTo("document-id-2")
    }

    @Test
    fun `deletePaymentRequests sends DELETE with the payment request ids as json array body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val resource = repository.deletePaymentRequests(listOf("payment-request-id-1"))

        assertThat(resource).isInstanceOf(Resource.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("DELETE")
        assertThat(request.path).isEqualTo("/paymentRequests")
        val body = JSONArray(request.body.readUtf8())
        assertThat(body.getString(0)).isEqualTo("payment-request-id-1")
    }

    @Test
    fun `getPaymentRequestDocument sends GET with the qr pdf media type as accept header and returns raw bytes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(okio.Buffer().write(ICON_BYTES)))

        val resource = repository.getPaymentRequestDocument("payment-request-id-77")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-77")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v3+qr+pdf")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v3+qr+pdf")

        assertThat((resource as Resource.Success).data).isEqualTo(ICON_BYTES)
    }

    @Test
    fun `getPaymentRequestImage sends GET with the qr png media type as accept header and returns raw bytes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(okio.Buffer().write(ICON_BYTES)))

        val resource = repository.getPaymentRequestImage("payment-request-id-77")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-77")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v3+qr+png")

        assertThat((resource as Resource.Success).data).isEqualTo(ICON_BYTES)
    }

    @Test
    fun `getPageImage fetches the largest page image smaller than 2000x2000`() = runTest {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/documents/document-id-13/pages" -> MockResponse().setResponseCode(200).setBody(PAGES_JSON)
                "/image-1280" -> MockResponse().setResponseCode(200).setBody(okio.Buffer().write(ICON_BYTES))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val resource = repository.getPageImage("document-id-13", 1)

        assertThat((resource as Resource.Success).data).isEqualTo(ICON_BYTES)
        val pagesRequest = server.takeRequest()
        assertThat(pagesRequest.path).isEqualTo("/documents/document-id-13/pages")
        val imageRequest = server.takeRequest()
        // 1280x1810 is the largest image smaller than 2000x2000 (2500x3000 is too large)
        assertThat(imageRequest.path).isEqualTo("/image-1280")
    }

    companion object {
        private const val ACCESS_TOKEN = "test-access-token-1234"

        private val ICON_BYTES = byteArrayOf(9, 8, 7, 6)

        private val PAYMENT_PROVIDER_JSON = """
            {
                "id": "provider-1",
                "name": "Test Bank",
                "packageNameAndroid": "net.example.testbank",
                "minAppVersion": { "android": "3.5.1" },
                "colors": { "background": "112233", "text": "ffffff" },
                "iconLocation": "/icons/provider-1.png",
                "playStoreUrlAndroid": "https://play.google.com/store/apps/details?id=net.example.testbank",
                "gpcSupportedPlatforms": ["android"],
                "openWithSupportedPlatforms": ["android"]
            }
        """.trimIndent()

        private val PAYMENT_PROVIDERS_JSON = """
            [
                $PAYMENT_PROVIDER_JSON,
                {
                    "id": "provider-2",
                    "name": "iOS Only Bank",
                    "packageNameAndroid": "net.example.iosonlybank",
                    "minAppVersion": { "android": "1.0.0" },
                    "colors": { "background": "445566", "text": "000000" },
                    "iconLocation": "/icons/provider-2.png",
                    "playStoreUrlAndroid": null,
                    "gpcSupportedPlatforms": ["ios"],
                    "openWithSupportedPlatforms": ["ios"]
                }
            ]
        """.trimIndent()

        private val PAGES_JSON = """
            [
                {
                    "pageNumber": 1,
                    "images": {
                        "750x900": "/image-750",
                        "1280x1810": "/image-1280",
                        "2500x3000": "/image-2500"
                    }
                }
            ]
        """.trimIndent()
    }
}
