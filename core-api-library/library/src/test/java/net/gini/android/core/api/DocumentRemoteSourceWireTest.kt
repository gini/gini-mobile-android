package net.gini.android.core.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.test.DocumentRemoteSourceForTests
import net.gini.android.core.api.test.WireTestGiniApiType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Wire-level characterization tests for the core document API.
 *
 * These tests pin down the exact HTTP requests (method, path, headers, body) produced by
 * [DocumentRemoteSource] via a real Retrofit + OkHttp stack against a local [MockWebServer],
 * and the mapping of HTTP responses to return values and exceptions.
 *
 * They protect against unintended changes to the request format while the token handling is
 * moved from repositories/remote sources into an OkHttp interceptor (PP-2363). They assert
 * what is sent over the wire, not how it is produced, so they must pass unchanged after the
 * refactoring.
 */
@RunWith(AndroidJUnit4::class)
class DocumentRemoteSourceWireTest {

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
            .client(OkHttpClient())
            .build()
        remoteSource = DocumentRemoteSourceForTests(
            Dispatchers.Unconfined,
            retrofit.create(DocumentService::class.java),
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
            accessToken,
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

        remoteSource.uploadDocument(accessToken, byteArrayOf(1), "image/jpeg", null, null, null)

        val request = server.takeRequest()
        assertThat(request.path).isEqualTo("/documents/")
    }

    @Test
    fun `getDocument sends GET with auth and accept headers and returns raw response body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DOCUMENT_JSON))

        val body = remoteSource.getDocument(accessToken, "document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(body).isEqualTo(DOCUMENT_JSON)
    }

    @Test
    fun `getDocumentFromUri resolves the uri against the base uri keeping path and query`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(DOCUMENT_JSON))

        val body = remoteSource.getDocumentFromUri(
            accessToken,
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

        assertThat(body).isEqualTo(DOCUMENT_JSON)
    }

    @Test
    fun `getExtractions sends GET with auth, accept and content type headers and returns raw response body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(EXTRACTIONS_JSON))

        val body = remoteSource.getExtractions(accessToken, "document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13/extractions")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(body).isEqualTo(EXTRACTIONS_JSON)
    }

    @Test
    fun `deleteDocument by id sends DELETE with auth, accept and content type headers`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        remoteSource.deleteDocument(accessToken, "document-id-13")

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

        remoteSource.deleteDocument(accessToken, Uri.parse(server.url("/documents/document-id-13").toString()))

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("DELETE")
        assertThat(request.path).isEqualTo("/documents/document-id-13")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
    }

    @Test
    fun `getDocumentLayout sends GET and parses the layout response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(LAYOUT_JSON))

        val layout = remoteSource.getDocumentLayout(accessToken, "document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13/layout")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(layout.pages).hasSize(1)
        val page = layout.pages[0]
        assertThat(page.number).isEqualTo(1)
        assertThat(page.sizeX).isEqualTo(595.3f)
        assertThat(page.sizeY).isEqualTo(841.9f)
        assertThat(page.textZones).hasSize(1)
        val word = page.textZones[0].paragraphs[0].lines[0].words[0]
        assertThat(word.text).isEqualTo("Invoice")
        assertThat(word.fontSize).isEqualTo(12.0f)
        assertThat(word.bold).isFalse()
        assertThat(page.regions).hasSize(1)
        assertThat(page.regions[0].type).isEqualTo("RemittanceSlip")
    }

    @Test
    fun `getDocumentPages sends GET and parses the pages response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAGES_JSON))

        val pages = remoteSource.getDocumentPages(accessToken, "document-id-13")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/documents/document-id-13/pages")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")

        assertThat(pages).hasSize(1)
        assertThat(pages[0].pageNumber).isEqualTo(1)
        assertThat(pages[0].images.medium).isEqualTo("https://api.gini.net/documents/document-id-13/pages/1/medium")
        assertThat(pages[0].images.large).isEqualTo("https://api.gini.net/documents/document-id-13/pages/1/large")
    }

    @Test
    fun `getPaymentRequest sends GET and parses the payment request response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAYMENT_REQUEST_JSON))

        val paymentRequest = remoteSource.getPaymentRequest(accessToken, "payment-request-id-42")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-42")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")

        assertThat(paymentRequest.paymentProvider).isEqualTo("payment-provider-id-1")
        assertThat(paymentRequest.recipient).isEqualTo("Dr. Test GmbH")
        assertThat(paymentRequest.iban).isEqualTo("DE02300209000106531065")
        assertThat(paymentRequest.bic).isEqualTo("CMCIDEDD")
        assertThat(paymentRequest.amount).isEqualTo("335.50:EUR")
        assertThat(paymentRequest.purpose).isEqualTo("Invoice 123")
        assertThat(paymentRequest.status).isEqualTo("open")
    }

    @Test
    fun `getPaymentRequests sends GET and parses the payment request list response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[$PAYMENT_REQUEST_JSON]"))

        val paymentRequests = remoteSource.getPaymentRequests(accessToken)

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")

        assertThat(paymentRequests).hasSize(1)
        assertThat(paymentRequests[0].recipient).isEqualTo("Dr. Test GmbH")
    }

    @Test
    fun `getPayment sends GET and parses the payment response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(PAYMENT_JSON))

        val payment = remoteSource.getPayment(accessToken, "payment-request-id-42")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/paymentRequests/payment-request-id-42/payment")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")

        assertThat(payment.paidAt).isEqualTo("2024-01-15T10:00:00")
        assertThat(payment.recipient).isEqualTo("Dr. Test GmbH")
        assertThat(payment.iban).isEqualTo("DE02300209000106531065")
        assertThat(payment.amount).isEqualTo("335.50:EUR")
        assertThat(payment.purpose).isEqualTo("Invoice 123")
    }

    @Test
    fun `getFile sends GET without accept header and returns raw bytes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(okio.Buffer().write(byteArrayOf(9, 8, 7))))

        val bytes = remoteSource.getFile(accessToken, server.url("/some/file/location").toString())

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/some/file/location")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $accessToken")
        assertThat(request.getHeader("Accept")).isNull()
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/vnd.gini.v1+json")

        assertThat(bytes).isEqualTo(byteArrayOf(9, 8, 7))
    }

    @Test
    fun `error response is thrown as ApiException with status code, body and headers`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("X-Request-Id", "request-id-99")
                .setBody(ERROR_JSON)
        )

        try {
            remoteSource.getDocument(accessToken, "document-id-13")
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertThat(e.responseStatusCode).isEqualTo(404)
            assertThat(e.responseBody).isEqualTo(ERROR_JSON)
            assertThat(e.message).isEqualTo(ERROR_JSON)
            assertThat(e.responseHeaders?.get("x-request-id")).containsExactly("request-id-99")
        }
    }

    @Test
    fun `server error response is thrown as ApiException with status code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))

        try {
            remoteSource.getExtractions(accessToken, "document-id-13")
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertThat(e.responseStatusCode).isEqualTo(503)
            assertThat(e.responseBody).isEqualTo("Service Unavailable")
        }
    }

    @Test
    fun `successful response with empty body returns an empty string`() = runTest {
        // A 200 response with an empty body: Retrofit maps it to an empty (non-null) ResponseBody,
        // so getDocument returns "" instead of throwing. Characterizes current behavior.
        server.enqueue(MockResponse().setResponseCode(200))

        val body = remoteSource.getDocument(accessToken, "document-id-13")

        assertThat(body).isEmpty()
    }

    companion object {

        val DOCUMENT_JSON = """
            {
                "id": "document-id-13",
                "progress": "COMPLETED",
                "pageCount": 1,
                "name": "invoice.jpg",
                "creationDate": 1515932941.2839999,
                "sourceClassification": "NATIVE",
                "_links": {
                    "document": "https://api.gini.net/documents/document-id-13",
                    "extractions": "https://api.gini.net/documents/document-id-13/extractions"
                }
            }
        """.trimIndent()

        val EXTRACTIONS_JSON = """
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
                        }
                    ]
                }
            }
        """.trimIndent()

        val LAYOUT_JSON = """
            {
                "pages": [
                    {
                        "number": 1,
                        "sizeX": 595.3,
                        "sizeY": 841.9,
                        "textZones": [
                            {
                                "paragraphs": [
                                    {
                                        "w": 100.0, "h": 20.0, "t": 50.0, "l": 60.0,
                                        "lines": [
                                            {
                                                "w": 100.0, "h": 10.0, "t": 50.0, "l": 60.0,
                                                "wds": [
                                                    {
                                                        "w": 40.0, "h": 10.0, "t": 50.0, "l": 60.0,
                                                        "fontSize": 12.0,
                                                        "fontFamily": "Arial",
                                                        "bold": false,
                                                        "text": "Invoice"
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
                        ],
                        "regions": [
                            { "w": 500.0, "h": 300.0, "t": 400.0, "l": 50.0, "type": "RemittanceSlip" }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val PAGES_JSON = """
            [
                {
                    "pageNumber": 1,
                    "images": {
                        "medium": "https://api.gini.net/documents/document-id-13/pages/1/medium",
                        "large": "https://api.gini.net/documents/document-id-13/pages/1/large"
                    }
                }
            ]
        """.trimIndent()

        val PAYMENT_REQUEST_JSON = """
            {
                "paymentProvider": "payment-provider-id-1",
                "requesterUri": "https://requester.example.org",
                "recipient": "Dr. Test GmbH",
                "iban": "DE02300209000106531065",
                "bic": "CMCIDEDD",
                "amount": "335.50:EUR",
                "purpose": "Invoice 123",
                "status": "open",
                "createdAt": "2024-01-15T09:00:00",
                "expirationDate": "2024-02-15T09:00:00"
            }
        """.trimIndent()

        val PAYMENT_JSON = """
            {
                "paidAt": "2024-01-15T10:00:00",
                "recipient": "Dr. Test GmbH",
                "iban": "DE02300209000106531065",
                "bic": "CMCIDEDD",
                "amount": "335.50:EUR",
                "purpose": "Invoice 123"
            }
        """.trimIndent()

        val ERROR_JSON = """{"message":"Document not found","requestId":"request-id-99"}"""
    }
}
