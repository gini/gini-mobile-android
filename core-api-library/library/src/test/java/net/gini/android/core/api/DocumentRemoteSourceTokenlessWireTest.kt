package net.gini.android.core.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.test.DocumentRemoteSourceForTests
import net.gini.android.core.api.test.TestDocumentService
import net.gini.android.core.api.test.WireTestGiniApiType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Wire-level characterization tests for the tokenless [DocumentRemoteSource] overloads which
 * production code uses since the token handling moved into an OkHttp interceptor (PP-2363).
 *
 * They mirror [DocumentRemoteSourceWireTest] (which pins the deprecated accessToken overloads
 * until they are removed): each request must look exactly like its deprecated counterpart's,
 * except that the Authorization header comes from the OkHttp layer. In particular they pin the
 * headerMap arguments of each overload (e.g. getDocument sends no Content-Type, getFile no
 * Accept header) which would otherwise only be checked by the bank and health wire suites.
 */
@RunWith(AndroidJUnit4::class)
class DocumentRemoteSourceTokenlessWireTest {

    private lateinit var server: MockWebServer
    private lateinit var remoteSource: DocumentRemoteSourceForTests

    private val apiType = WireTestGiniApiType()
    private val accessToken = "test-access-token-1234"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            // Stands in for the production session interceptor (GiniSessionInterceptor):
            // authenticates the requests with the access token, which is all these wire
            // tests need from the client composition.
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("Authorization", "Bearer $accessToken")
                                .build()
                        )
                    }
                    .build()
            )
            .build()
        remoteSource = DocumentRemoteSourceForTests(
            Dispatchers.Unconfined,
            retrofit.create(TestDocumentService::class.java),
            apiType,
            server.url("/").toString()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `uploadDocument sends POST with auth, media type, metadata headers, query params and body, returns location header`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Location", "https://api.gini.net/documents/document-id-13")
        )

        val documentData = byteArrayOf(1, 2, 3)
        val uri = remoteSource.uploadDocument(
            documentData,
            "application/vnd.gini.v1.partial+jpeg",
            "invoice.jpg",
            "Invoice",
            mapOf("GiniCaptureVersion" to "3.4.0")
        )

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/documents/?filename=invoice.jpg&doctype=Invoice")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1.partial+jpeg")
        assertThat(request.getHeader("GiniCaptureVersion")).isEqualTo("3.4.0")
        assertThat(request.body.readByteArray()).isEqualTo(documentData)

        assertThat(uri).isEqualTo(Uri.parse("https://api.gini.net/documents/document-id-13"))
    }

    @Test
    fun `uploadDocument omits filename and doctype query params when not given`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Location", "https://api.gini.net/documents/document-id-13")
        )

        remoteSource.uploadDocument(byteArrayOf(1), "image/jpeg", null, null, null)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/documents/")
    }

    @Test
    fun `getDocument sends GET with accept header but no content type and returns raw response body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DocumentRemoteSourceWireTest.DOCUMENT_JSON))

        val body = remoteSource.getDocument("document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isNull()

        assertThat(body).isEqualTo(DocumentRemoteSourceWireTest.DOCUMENT_JSON)
    }

    @Test
    fun `getDocumentFromUri resolves the uri against the base uri keeping path and query`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DocumentRemoteSourceWireTest.DOCUMENT_JSON))

        val body = remoteSource.getDocumentFromUri(
            Uri.parse("https://some-other-host.example.org/documents/document-id-13?param=1")
        )

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        // Path and query are taken from the given uri, but the request goes to the configured base uri host.
        // Note: Uri.Builder.query() treats the query string as decoded and percent-encodes '=' when
        // rebasing, so query params arrive re-encoded ("param%3D1"). Pinned as-is.
        assertThat(request.path).isEqualTo("/documents/document-id-13?param%3D1")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(body).isEqualTo(DocumentRemoteSourceWireTest.DOCUMENT_JSON)
    }

    @Test
    fun `getExtractions sends GET with accept and content type headers and returns raw response body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DocumentRemoteSourceWireTest.EXTRACTIONS_JSON))

        val body = remoteSource.getExtractions("document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13/extractions")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(body).isEqualTo(DocumentRemoteSourceWireTest.EXTRACTIONS_JSON)
    }

    @Test
    fun `deleteDocument by id sends DELETE with accept and content type headers`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        remoteSource.deleteDocument("document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("DELETE")
        assertThat(request.path).isEqualTo("/documents/document-id-13")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")
    }

    @Test
    fun `deleteDocument by uri sends DELETE to the given uri`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        remoteSource.deleteDocument(Uri.parse(server.url("/documents/document-id-13").toString()))

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("DELETE")
        assertThat(request.path).isEqualTo("/documents/document-id-13")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")
    }

    @Test
    fun `getDocumentLayout sends GET with accept and content type headers and parses the layout response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DocumentRemoteSourceWireTest.LAYOUT_JSON))

        val layout = remoteSource.getDocumentLayout("document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13/layout")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(layout.pages).hasSize(1)
        assertThat(layout.pages[0].number).isEqualTo(1)
        assertThat(layout.pages[0].textZones[0].paragraphs[0].lines[0].words[0].text).isEqualTo("Invoice")
    }

    @Test
    fun `getDocumentPages sends GET with accept and content type headers and parses the pages response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DocumentRemoteSourceWireTest.PAGES_JSON))

        val pages = remoteSource.getDocumentPages("document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13/pages")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(pages).hasSize(1)
        assertThat(pages[0].pageNumber).isEqualTo(1)
        assertThat(pages[0].images.medium).isEqualTo("https://api.gini.net/documents/document-id-13/pages/1/medium")
    }

    @Test
    fun `sendFeedback sends POST with accept and content type headers and the given body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val feedbackJson = """{"feedback":{"amountToPay":{"value":"335.50:EUR"}}}"""
        remoteSource.sendFeedback("document-id-13", feedbackJson.toRequestBody())

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/documents/document-id-13/extractions")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.body.readUtf8()).isEqualTo(feedbackJson)
    }

    @Test
    fun `getPaymentRequest sends GET with accept and content type headers and parses the payment request response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DocumentRemoteSourceWireTest.PAYMENT_REQUEST_JSON))

        val paymentRequest = remoteSource.getPaymentRequest("payment-request-id-42")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-42")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(paymentRequest.paymentProvider).isEqualTo("payment-provider-id-1")
        assertThat(paymentRequest.recipient).isEqualTo("Dr. Test GmbH")
        assertThat(paymentRequest.iban).isEqualTo("DE02300209000106531065")
        assertThat(paymentRequest.status).isEqualTo("open")
    }

    @Test
    fun `getPaymentRequests sends GET with accept and content type headers and parses the payment request list response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("[${DocumentRemoteSourceWireTest.PAYMENT_REQUEST_JSON}]")
        )

        val paymentRequests = remoteSource.getPaymentRequests()

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(paymentRequests).hasSize(1)
        assertThat(paymentRequests[0].recipient).isEqualTo("Dr. Test GmbH")
    }

    @Test
    fun `getPayment sends GET with accept and content type headers and parses the payment response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DocumentRemoteSourceWireTest.PAYMENT_JSON))

        val payment = remoteSource.getPayment("payment-request-id-42")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-42/payment")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(payment.paidAt).isEqualTo("2024-01-15T10:00:00")
        assertThat(payment.recipient).isEqualTo("Dr. Test GmbH")
        assertThat(payment.amount).isEqualTo("335.50:EUR")
    }

    @Test
    fun `getFile sends GET without accept header and returns raw bytes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(okio.Buffer().write(byteArrayOf(9, 8, 7))))

        val bytes = remoteSource.getFile(server.url("/some/file/location").toString())

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/some/file/location")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isNull()
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(bytes).isEqualTo(byteArrayOf(9, 8, 7))
    }
}
