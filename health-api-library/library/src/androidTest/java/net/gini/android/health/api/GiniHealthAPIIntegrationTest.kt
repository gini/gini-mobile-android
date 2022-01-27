package net.gini.android.health.api

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import bolts.Task
import net.gini.android.core.api.DocumentTaskManager
import net.gini.android.core.api.test.shared.helpers.TestUtils
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.test.shared.GiniCoreAPIIntegrationTest
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.models.PaymentRequestInput
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import java.util.*
import java.util.Collections.singletonList

/**
 * Created by Alpár Szotyori on 24.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class GiniHealthAPIIntegrationTest: GiniCoreAPIIntegrationTest<HealthApiDocumentTaskManager, HealthApiDocumentManager, GiniHealthAPI, HealthApiCommunicator>() {

    @Test
    @Throws(Exception::class)
    fun sendFeedback_withoutCompoundExtractions_forDocument_withLineItems() {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("line-items.pdf")
        Assert.assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(testDocument, "application/pdf", "line-items.pdf",
            DocumentTaskManager.DocumentType.INVOICE
        ) { extractionsContainer: ExtractionsContainer? -> }
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]!!

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedbackSpecific: MutableMap<String, SpecificExtraction?> = HashMap()
        feedbackSpecific["amount_to_pay"] = extractionsContainer.specificExtractions["amount_to_pay"]

        val sendFeedback = giniCoreAPI.documentTaskManager.sendFeedbackForExtractions(document, feedbackSpecific)
        sendFeedback.waitForCompletion()
        if (sendFeedback.isFaulted) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.error))
        }
        Assert.assertTrue("Sending feedback should be completed", sendFeedback.isCompleted)
        Assert.assertFalse("Sending feedback should be successful", sendFeedback.isFaulted)
    }

    @Test
    @Throws(Exception::class)
    fun sendFeedback_withCompoundExtractions_forDocument_withLineItems() {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("line-items.pdf")
        Assert.assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(testDocument, "application/pdf", "line-items.pdf",
            DocumentTaskManager.DocumentType.INVOICE
        ) { extractionsContainer: ExtractionsContainer? -> }
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]
        val compoundExtractions: Map<String, CompoundExtraction> = extractionsContainer!!.compoundExtractions

        val feedbackCompound: MutableMap<String, CompoundExtraction> = HashMap()

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedbackSpecific: MutableMap<String, SpecificExtraction?> = HashMap()
        feedbackSpecific["amount_to_pay"] = extractionsContainer.specificExtractions["amount_to_pay"]

        val feedbackPayment: MutableMap<String, SpecificExtraction?> = HashMap()
        feedbackPayment["iban"] = getIban(extractionsContainer)
        feedbackPayment["amount_to_pay"] = getAmountToPay(extractionsContainer)
        feedbackPayment["bic"] = getBic(extractionsContainer)
        feedbackPayment["payment_recipient"] = getPaymentRecipient(extractionsContainer)

        feedbackCompound["payment"] = CompoundExtraction("payment", singletonList(feedbackPayment))

        // All compound extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        feedbackCompound["line_items"] = compoundExtractions["line_items"]!!
        val sendFeedback =
            giniCoreAPI.documentTaskManager.sendFeedbackForExtractions(document, feedbackSpecific, feedbackCompound)
        sendFeedback.waitForCompletion()
        if (sendFeedback.isFaulted) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.error))
        }
        Assert.assertTrue("Sending feedback should be completed", sendFeedback.isCompleted)
        Assert.assertFalse("Sending feedback should be successful", sendFeedback.isFaulted)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPaymentProviders() {
        val task = giniCoreAPI.documentTaskManager.paymentProviders
        task.waitForCompletion()
        Assert.assertNotNull(task.result)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPaymentProvider() {
        val listTask = giniCoreAPI.documentTaskManager.paymentProviders
        listTask.waitForCompletion()
        Assert.assertNotNull(listTask.result)
        val providers = listTask.result
        val task = giniCoreAPI.documentTaskManager.getPaymentProvider(providers[0].id)
        task.waitForCompletion()
        Assert.assertEquals(providers[0], task.result)
    }

    @Test
    @Throws(Exception::class)
    fun testCreatePaymentRequest() {
        val task = createPaymentRequest()
        task.waitForCompletion()
        Assert.assertNotNull(task.result)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPaymentRequest() {
        val createPaymentTask = createPaymentRequest()
        createPaymentTask.waitForCompletion()
        val id = createPaymentTask.result
        val paymentRequestTask = giniCoreAPI.documentTaskManager.getPaymentRequest(id)
        paymentRequestTask.waitForCompletion()
        Assert.assertNotNull(paymentRequestTask.result)
    }

    @Throws(Exception::class)
    private fun createPaymentRequest(): Task<String?> {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentWithExtractions =
            processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE)
        val document = documentWithExtractions.keys.iterator().next()
        val extractionsContainer = documentWithExtractions[document]!!
        val listTask = giniCoreAPI.documentTaskManager.paymentProviders
        listTask.waitForCompletion()
        Assert.assertNotNull(listTask.result)
        val providers = listTask.result
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
        return giniCoreAPI.documentTaskManager.createPaymentRequest(paymentRequest)
    }

    @Test
    @Throws(Exception::class)
    fun testGetImage() {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentWithExtractions =
            processDocument(testDocument, "image/jpeg", "test.jpg", DocumentTaskManager.DocumentType.INVOICE)
        val document = documentWithExtractions.keys.iterator().next()
        val task = giniCoreAPI.documentTaskManager.getPageImage(document.id, 1)
        task.waitForCompletion()
        Assert.assertNotNull(task.result)
        val bytes = task.result
        Assert.assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }

    override fun createGiniCoreAPIBuilder(
        clientId: String,
        clientSecret: String,
        emailDomain: String
    ): GiniCoreAPIBuilder<HealthApiDocumentTaskManager, HealthApiDocumentManager, GiniHealthAPI, HealthApiCommunicator> =
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
