package net.gini.android.capture.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.GiniBankAPIBuilder
import net.gini.android.capture.Document
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.capture.network.test.ExtractionsFixture
import net.gini.android.capture.network.test.bankAPIDocumentWithId
import net.gini.android.capture.network.test.fromJsonAsset
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Alpár Szotyori on 18.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class ExtractionFeedbackIntegrationTest {

    private lateinit var networkService: GiniCaptureDefaultNetworkService
    private lateinit var networkApi: GiniCaptureDefaultNetworkApi
    private lateinit var giniBankAPI: GiniBankAPI
    private var analyzedGiniApiDocument: net.gini.android.core.api.models.Document? = null

    private val moshi = Moshi.Builder().build()

    @Before
    fun setUp() {
        val testProperties = Properties().apply {
            getApplicationContext<Context>().resources.assets
                .open("test.properties").use { load(it) }
        }

        networkService = GiniCaptureDefaultNetworkService
            .builder(getApplicationContext())
            .setClientCredentials(
                testProperties["testClientId"] as String,
                testProperties["testClientSecret"] as String,
                "giniFeedback.test"
            )
            .setBaseUrl(testProperties["testApiUri"] as String)
            .setUserCenterBaseUrl(testProperties["testUserCenterUri"] as String)
            .build()

        networkApi = GiniCaptureDefaultNetworkApi
            .builder()
            .withGiniCaptureDefaultNetworkService(networkService)
            .build()

        giniBankAPI = GiniBankAPIBuilder(
            getApplicationContext(),
            testProperties["testClientId"] as String,
            testProperties["testClientSecret"] as String,
            "giniFeedback.test"
        )
            .setApiBaseUrl(testProperties["testApiUri"] as String)
            .setUserCenterApiBaseUrl(testProperties["testUserCenterUri"] as String)
            .setConnectionTimeoutInMs(60000)
            .build()
    }

    @After
    fun tearDown() {
        networkService.cleanup()
    }

    @Test
    fun sendExtractionFeedback() = runBlocking {
        // 1. Analyze a test document
        val extractionsBundle = getExtractionsFromCaptureSDK()

        //    Verify we received the correct extractions for this test
        val extractionsFixture = moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example.json")!!
        Assert.assertTrue(extractionsFixture.equals(extractionsBundle))

        // 3. Assuming the user saw the following extractions:
        //    amountToPay, iban, bic, paymentPurpose and paymentRecipient

        //    Supposing the user changed the amountToPay from "995.00:EUR" to "950.00:EUR"
        //    we need to update that extraction
        val amountToPay = extractionsBundle.getParcelable<GiniCaptureSpecificExtraction>("amountToPay")
        amountToPay!!.value = "950.00:EUR"

        //    Send feedback for the extractions the user saw
        //    with the final (user confirmed or updated) extraction values
        suspendCancellableCoroutine<Unit> { continuation ->
            networkApi.sendFeedback(
                mapOf(
                    "amountToPay" to amountToPay,
                    "iban" to extractionsBundle.getParcelable("iban")!!,
                    "bic" to extractionsBundle.getParcelable("bic")!!,
                    "paymentPurpose" to extractionsBundle.getParcelable("paymentPurpose")!!,
                    "paymentRecipient" to extractionsBundle.getParcelable("paymentRecipient")!!
                ),
                object : GiniCaptureNetworkCallback<Void?, Error> {
                    override fun failure(error: Error) {
                        continuation.resumeWithException(RuntimeException(error.message, error.cause))
                    }

                    override fun success(result: Void?) {
                        continuation.resume(Unit)
                    }

                    override fun cancelled() {
                        continuation.cancel()
                    }
                }
            )
        }

        // 4. Verify that the extractions were updated using the Gini Bank API (available in your app when using
        //    the GiniCaptureDefaultNetworkService).
        //    This is only done for testing purposes. In your production code you don't need to interact with the
        //    Gini Bank API directly if you use the GiniCaptureDefaultNetworkService.
        val extractionsAfterFeedback =
            giniBankAPI.documentManager.getExtractions(analyzedGiniApiDocument!!)

        val extractionsAfterFeedbackFixture =
            moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example_after_feedback.json")!!
        Assert.assertTrue(extractionsAfterFeedbackFixture.equals(extractionsAfterFeedback))
    }

    /**
     * This method reproduces the document upload and analysis done by the Capture SDK.
     *
     * The intent of this method is to create an extractions [Bundle] like the one your app
     * receives after a user analysed a document with the Capture SDK.
     *
     * In your production code you should not call [GiniCaptureDefaultNetworkService] methods.
     * Interaction with the network service is handled by the Capture SDK internally.
     */
    private suspend fun getExtractionsFromCaptureSDK(): Bundle {
        // Upload a test document
        val uploadResult = suspendCancellableCoroutine<Result> { continuation ->
            networkService.upload(TEST_DOCUMENT, object : GiniCaptureNetworkCallback<Result, Error> {
                override fun failure(error: Error) {
                    continuation.resumeWithException(RuntimeException(error.message, error.cause))
                }

                override fun success(result: Result) {
                    continuation.resume(result)
                }

                override fun cancelled() {
                    continuation.cancel()
                }
            })
        }

        // Analyze the uploaded test document
        val analysisResult = suspendCancellableCoroutine<AnalysisResult> { continuation ->
            networkService.analyze(linkedMapOf(uploadResult.giniApiDocumentId to 0), object :
                GiniCaptureNetworkCallback<AnalysisResult, Error> {
                override fun failure(error: Error) {
                    continuation.resumeWithException(RuntimeException(error.message, error.cause))
                }

                override fun success(result: AnalysisResult) {
                    continuation.resume(result)
                }

                override fun cancelled() {
                    continuation.cancel()
                }
            })
        }

        analyzedGiniApiDocument = bankAPIDocumentWithId(analysisResult.giniApiDocumentId)

        return analysisResult.extractions.entries.fold(Bundle()) { bundle, entry ->
            bundle.putParcelable(entry.key, entry.value)
            return@fold bundle
        }
    }

    companion object {
        val TEST_DOCUMENT = object : Document {
            private val pdfBytes = getApplicationContext<Context>().resources.assets
                .open("Gini_invoice_example.pdf").use { it.readBytes() }

            override fun describeContents(): Int = 0

            override fun writeToParcel(dest: Parcel?, flags: Int) {}

            override fun getId(): String = UUID.randomUUID().toString()

            override fun getType(): Document.Type = Document.Type.PDF

            override fun getMimeType(): String = MimeType.APPLICATION_PDF.asString()

            override fun getData(): ByteArray = pdfBytes

            override fun getIntent(): Intent? = null

            override fun getUri(): Uri? = null

            override fun isImported(): Boolean = false

            override fun getImportMethod(): Document.ImportMethod = Document.ImportMethod.NONE

            override fun getSource(): Document.Source = Document.Source.newSource("androidTest")

            override fun isReviewable(): Boolean = false
        }
    }
}