package net.gini.android.bank.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.GiniBankAPIBuilder
import net.gini.android.bank.sdk.capture.CaptureConfiguration
import net.gini.android.bank.sdk.capture.CaptureResult
import net.gini.android.bank.sdk.capture.ResultError
import net.gini.android.bank.sdk.test.ExtractionsFixture
import net.gini.android.bank.sdk.test.bankAPIDocumentWithId
import net.gini.android.bank.sdk.test.fromJsonAsset
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.Document
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.internal.util.MimeType
import net.gini.android.capture.network.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Alpár Szotyori on 18.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class TransferSummaryIntegrationTest {

    private lateinit var networkService: GiniCaptureDefaultNetworkService
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

        GiniBank.setCaptureConfiguration(
            getApplicationContext(),
            CaptureConfiguration(
                networkService = networkService
            )
        )

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

    @Test
    fun sendExtractionFeedbackWithoutPaymentReference() = runBlocking {
        // 1. Analyze a test document
        val result = getExtractionsFromBankSDK(TEST_DOCUMENT_WITHOUT_PAYMENT_REFERENCE)

        if (result !is CaptureResult.Success) {
            Assert.fail(result.toString())
            return@runBlocking
        }

        //    Verify we received the correct extractions for this test
        val extractionsFixture = moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example.json")!!
        Assert.assertTrue(extractionsFixture.equals(result.specificExtractions))

        // 3. Assuming the user saw the following extractions:
        //    amountToPay, iban, bic, paymentPurpose and paymentRecipient

        //    Before releasing capture we need to provide the values the user has used for
        //    creating the transaction.
        //    Supposing the user changed the amountToPay from "995.00:EUR" to "950.00:EUR"
        //    we need to pass in the changed value. For the other extractions we can pass in
        //    the original values since the user did not edit them.
        GiniBank.sendTransferSummary(
            result.specificExtractions["paymentRecipient"]!!.value,
            "", // Payment reference was not shown to the user and can be left empty
            result.specificExtractions["paymentPurpose"]!!.value,
            result.specificExtractions["iban"]!!.value,
            result.specificExtractions["bic"]!!.value,
            Amount(BigDecimal("950.00"), AmountCurrency.EUR)
        )

        // Now we can release capture
        GiniBank.releaseCapture(getApplicationContext()
        )

        //    Wait a little for the feedback sending to complete
        delay(2_000)

        // 4. Verify that the extractions were updated using the Gini Bank API
        val extractionsAfterFeedback =
            giniBankAPI.documentManager.getAllExtractionsWithPolling(analyzedGiniApiDocument!!)

        val extractionsAfterFeedbackFixture =
            moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example_after_feedback.json")!!
        Assert.assertTrue(extractionsAfterFeedbackFixture.equals(extractionsAfterFeedback.data))
    }

//    @Test
//    fun sendExtractionFeedbackWithPaymentReference() = runBlocking {
//        // 1. Analyze a test document
//        val result = getExtractionsFromBankSDK(TEST_DOCUMENT_WITH_PAYMENT_REFERENCE)
//
//        if (result !is CaptureResult.Success) {
//            Assert.fail(result.toString())
//            return@runBlocking
//        }
//
//        //    Verify we received the correct extractions for this test
//        val extractionsFixture = moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example_payment_reference.json")!!
//        Assert.assertTrue(extractionsFixture.equals(result.specificExtractions))
//
//        // 3. Assuming the user saw the following extractions:
//        //    amountToPay, iban, bic, paymentPurpose, paymentReference and paymentRecipient
//
//        //    When releasing capture we need to provide the values the user has used for
//        //    creating the transaction.
//        //    Supposing the user changed the amountToPay from "995.00:EUR" to "950.00:EUR"
//        //    we need to pass in the changed value. For the other extractions we can pass in
//        //    the original values since the user did not edit them.
//        GiniBank.releaseCapture(getApplicationContext(),
//            result.specificExtractions["paymentRecipient"]!!.value,
//            result.specificExtractions["paymentReference"]!!.value,
//            result.specificExtractions["paymentPurpose"]!!.value,
//            result.specificExtractions["iban"]!!.value,
//            result.specificExtractions["bic"]!!.value,
//            Amount(BigDecimal("950.00"), AmountCurrency.EUR)
//        )
//
//        //    Wait a little for the feedback sending to complete
//        delay(2_000)
//
//        // 4. Verify that the extractions were updated using the Gini Bank API
//        val extractionsAfterFeedback =
//            giniBankAPI.documentManager.getAllExtractionsWithPolling(analyzedGiniApiDocument!!)
//
//        val extractionsAfterFeedbackFixture =
//            moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example_payment_reference_after_feedback.json")!!
//        Assert.assertTrue(extractionsAfterFeedbackFixture.equals(extractionsAfterFeedback.data))
//    }

    /**
     * This method reproduces the document upload and analysis done by the Bank SDK.
     *
     * The intent of this method is to create a [CaptureResult] like the one your app
     * receives after a user analysed a document with the Bank SDK.
     *
     * In your production code you should not call [GiniCaptureDefaultNetworkService] methods.
     * Interaction with the network service is handled by the Bank SDK internally.
     */
    private suspend fun getExtractionsFromBankSDK(testDocument: TestDocument): CaptureResult {
        try {
            // Upload a test document
            val uploadResult = suspendCancellableCoroutine<Result> { continuation ->
                networkService.upload(testDocument, object : GiniCaptureNetworkCallback<Result, Error> {
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

            return CaptureResult.Success(
                specificExtractions = analysisResult.extractions,
                compoundExtractions = analysisResult.compoundExtractions,
                returnReasons = analysisResult.returnReasons
            )

        } catch (e: Exception) {
            return CaptureResult.Error(ResultError.Capture(GiniCaptureError(GiniCaptureError.ErrorCode.ANALYSIS, e.message)))
        }
    }

    companion object {
        val TEST_DOCUMENT_WITHOUT_PAYMENT_REFERENCE = TestDocument("Gini_invoice_example.pdf")

        val TEST_DOCUMENT_WITH_PAYMENT_REFERENCE = TestDocument("Gini_invoice_example_payment_reference.pdf")
    }

    class TestDocument(assetFileName: String): Document {
        private val pdfBytes = getApplicationContext<Context>().resources.assets
            .open(assetFileName).use { it.readBytes() }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {}

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

        override fun generateUploadMetadata(context: Context?): String = ""
    }
}