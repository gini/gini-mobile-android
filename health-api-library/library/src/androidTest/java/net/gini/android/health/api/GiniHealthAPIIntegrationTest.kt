package net.gini.android.health.api

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.Resource
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.test.shared.GiniCoreAPIIntegrationTest
import net.gini.android.core.api.test.shared.helpers.TestUtils
import net.gini.android.health.api.models.PaymentRequestInput
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections.singletonList

/**
 * Created by Alpár Szotyori on 24.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GiniHealthAPIIntegrationTest: GiniCoreAPIIntegrationTest<HealthApiDocumentManager, HealthApiDocumentRepository, GiniHealthAPI, ExtractionsContainer>() {

    @Test
    @Throws(Exception::class)
    fun sendFeedback_withoutCompoundExtractions_forDocument_withLineItems() = runTest {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("line-items.pdf")
        Assert.assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(testDocument, "application/pdf", "line-items.pdf",
            DocumentManager.DocumentType.INVOICE
        ) { }
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]!!

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedbackSpecific: MutableMap<String, SpecificExtraction> = HashMap()
        feedbackSpecific["amount_to_pay"] = extractionsContainer.specificExtractions["amount_to_pay"]!!

        val sendFeedback = giniCoreApi.documentManager.sendFeedbackForExtractions(document, feedbackSpecific)

        if (sendFeedback is Resource.Error) {
            Log.e("TEST", sendFeedback.toString())
        }
        Assert.assertTrue("Sending feedback should be successful", sendFeedback is Resource.Success)
    }

    @Test
    @Throws(Exception::class)
    fun sendFeedback_withCompoundExtractions_forDocument_withLineItems() = runTest {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("line-items.pdf")
        Assert.assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(testDocument, "application/pdf", "line-items.pdf",
            DocumentManager.DocumentType.INVOICE
        ) { }
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]
        val compoundExtractions: Map<String, CompoundExtraction> = extractionsContainer!!.compoundExtractions

        val feedbackCompound: MutableMap<String, CompoundExtraction> = HashMap()

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedbackSpecific: MutableMap<String, SpecificExtraction> = HashMap()
        feedbackSpecific["amount_to_pay"] = extractionsContainer.specificExtractions["amount_to_pay"]!!

        val feedbackPayment: MutableMap<String, SpecificExtraction> = HashMap()
        feedbackPayment["iban"] = getIban(extractionsContainer)!!
        feedbackPayment["amount_to_pay"] = getAmountToPay(extractionsContainer)!!
        feedbackPayment["bic"] = getBic(extractionsContainer)!!
        feedbackPayment["payment_recipient"] = getPaymentRecipient(extractionsContainer)!!

        feedbackCompound["payment"] = CompoundExtraction("payment", singletonList(feedbackPayment))

        // All compound extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        feedbackCompound["line_items"] = compoundExtractions["line_items"]!!
        val sendFeedback =
            giniCoreApi.documentManager.sendFeedbackForExtractions(document, feedbackSpecific, feedbackCompound)

        if (sendFeedback is Resource.Error) {
            Log.e("TEST", sendFeedback.toString())
        }
        Assert.assertTrue("Sending feedback should be successful", sendFeedback is Resource.Success)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPaymentProviders() = runTest {
        val paymentProviders = giniCoreApi.documentManager.getPaymentProviders().dataOrThrow

        Assert.assertTrue("Payment providers list should not be empty", paymentProviders.isNotEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGetPaymentProvider() = runTest {
        val paymentProviders = giniCoreApi.documentManager.getPaymentProviders().dataOrThrow

        val paymentProvider = giniCoreApi.documentManager.getPaymentProvider(paymentProviders[0].id).dataOrThrow

        Assert.assertEquals(paymentProviders[0], paymentProvider)
    }

    @Test
    @Throws(Exception::class)
    fun testCreatePaymentRequest() = runTest {
        val paymentRequestId = createPaymentRequest()
        Assert.assertTrue("Payment request id should not be empty string", paymentRequestId.isNotBlank())
    }

    @Test
    @Throws(Exception::class)
    fun testGetPaymentRequest() = runTest {
        val paymentRequestId = createPaymentRequest()
        val paymentRequest = giniCoreApi.documentManager.getPaymentRequest(paymentRequestId).dataOrThrow
        Assert.assertEquals(paymentRequest.status, PaymentRequest.Status.OPEN)
    }

    @Throws(Exception::class)
    private suspend fun createPaymentRequest(): String {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentWithExtractions =
            processDocument(testDocument, "image/jpeg", "test.jpg", DocumentManager.DocumentType.INVOICE)
        val document = documentWithExtractions.keys.iterator().next()
        val extractionsContainer = documentWithExtractions[document]!!
        val providers = giniCoreApi.documentManager.getPaymentProviders().dataOrThrow
        val paymentRequest = PaymentRequestInput(
            providers[0].id,
            getPaymentRecipient(extractionsContainer)!!.value,
            getIban(extractionsContainer)!!.value,
            getAmountToPay(extractionsContainer)!!.value,
            getPaymentPurpose(extractionsContainer)!!.value,
            null,  // We make bic optional for now
            //                Objects.requireNonNull(extractions.get("bic")).getValue(),
            document.uri.toString()
        )
        return giniCoreApi.documentManager.createPaymentRequest(paymentRequest).dataOrThrow
    }

    @Test
    @Throws(Exception::class)
    fun testGetImage() = runTest {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentWithExtractions =
            processDocument(testDocument, "image/jpeg", "test.jpg",
                DocumentManager.DocumentType.INVOICE) { }
        val document = documentWithExtractions.keys.iterator().next()
        val imageBytes = giniCoreApi.documentManager.getPageImage(document.id, 1).dataOrThrow
        Assert.assertNotNull(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size))
    }

    override fun createGiniCoreAPIBuilder(
        clientId: String,
        clientSecret: String,
        emailDomain: String
    ): GiniCoreAPIBuilder<HealthApiDocumentManager, GiniHealthAPI, HealthApiDocumentRepository, ExtractionsContainer> =
        GiniHealthAPIBuilder(getApplicationContext(), clientId, clientSecret, emailDomain)

    override fun getNetworkSecurityConfigResId(): Int = net.gini.android.health.api.test.R.xml.network_security_config

    override fun getIban(extractionsContainer: ExtractionsContainer): SpecificExtraction? =
        extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("iban")

    override fun getBic(extractionsContainer: ExtractionsContainer): SpecificExtraction? =
        extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("bic")

    override fun getAmountToPay(extractionsContainer: ExtractionsContainer): SpecificExtraction? {
        return extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("amount_to_pay")
    }

    override fun getPaymentRecipient(extractionsContainer: ExtractionsContainer): SpecificExtraction? {
        return extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("payment_recipient")
    }

    override fun getPaymentPurpose(extractionsContainer: ExtractionsContainer): SpecificExtraction? {
        return extractionsContainer.compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.get("payment_purpose")
    }

}
