package net.gini.android.bank.api

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import bolts.Task
import junit.framework.Assert.assertNull
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.core.api.DocumentTaskManager
import net.gini.android.core.api.test.shared.helpers.TestUtils
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.test.shared.GiniCoreAPIIntegrationTest
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.models.Box
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.core.api.test.shared.BuildConfig
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import java.util.*

/**
 * Created by Alpár Szotyori on 24.01.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class GiniBankAPIIntegrationTest: GiniCoreAPIIntegrationTest<BankApiDocumentTaskManager, BankApiDocumentManager, GiniBankAPI, BankApiCommunicator, ExtractionsContainer>() {

    private lateinit var giniHealthAPI: GiniHealthAPI

    @Test
    @Throws(Exception::class)
    fun sendFeedback_withCompoundExtractions_forDocument_withoutLineItems() {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(
            testDocument, "image/jpeg", "test.jpg",
            DocumentTaskManager.DocumentType.INVOICE
        )
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedback: MutableMap<String, SpecificExtraction?> = HashMap()
        feedback["iban"] = getIban(extractionsContainer!!)
        feedback["amountToPay"] = getAmountToPay(extractionsContainer)
        feedback["bic"] = getBic(extractionsContainer)
        feedback["paymentRecipient"] = getPaymentRecipient(extractionsContainer)

        // The document had no compound extractions but we create some and send them back as feedback
        val box = Box(1, 2.0, 3.0, 4.0, 5.0)
        val rows: MutableList<Map<String, SpecificExtraction>> = ArrayList()
        val firstRowColumns: MutableMap<String, SpecificExtraction> = HashMap()
        firstRowColumns["description"] =
            SpecificExtraction("description", "CORE ICON - Sweatjacke - emerald", "text", box, emptyList())
        firstRowColumns["grossPrice"] = SpecificExtraction("grossPrice", "39.99:EUR", "amount", box, emptyList())
        rows.add(firstRowColumns)
        val secondRowColumns: MutableMap<String, SpecificExtraction> = HashMap()
        secondRowColumns["description"] =
            SpecificExtraction("description", "Strickpullover - yellow", "text", box, emptyList())
        secondRowColumns["grossPrice"] = SpecificExtraction("grossPrice", "59.99:EUR", "amount", box, emptyList())
        rows.add(secondRowColumns)
        val compoundExtraction = CompoundExtraction("lineItems", rows)
        val feedbackCompound: MutableMap<String, CompoundExtraction> = HashMap()
        feedbackCompound["lineItems"] = compoundExtraction
        val sendFeedback =
            giniCoreAPI.documentTaskManager.sendFeedbackForExtractions(document, feedback, feedbackCompound)
        sendFeedback.waitForCompletion()
        if (sendFeedback.isFaulted) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.error))
        }
        Assert.assertTrue("Sending feedback should be completed", sendFeedback.isCompleted)
        Assert.assertFalse("Sending feedback should be successful", sendFeedback.isFaulted)
    }

    @Test
    @Throws(Exception::class)
    fun sendFeedback_withoutCompoundExtractions_forDocument_withoutLineItems() {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("test.jpg")
        Assert.assertNotNull("test image test.jpg could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(
            testDocument, "image/jpeg", "test.jpg",
            DocumentTaskManager.DocumentType.INVOICE
        )
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedback: MutableMap<String, SpecificExtraction?> = HashMap()
        feedback["iban"] = getIban(extractionsContainer!!)
        feedback["amountToPay"] = getAmountToPay(extractionsContainer)
        feedback["bic"] = getBic(extractionsContainer)
        feedback["paymentRecipient"] = getPaymentRecipient(extractionsContainer)
        val sendFeedback = giniCoreAPI.documentTaskManager.sendFeedbackForExtractions(document, feedback)
        sendFeedback.waitForCompletion()
        if (sendFeedback.isFaulted) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.error))
        }
        Assert.assertTrue("Sending feedback should be completed", sendFeedback.isCompleted)
        Assert.assertFalse("Sending feedback should be successful", sendFeedback.isFaulted)
    }

    @Test
    @Throws(Exception::class)
    fun sendFeedback_withoutCompoundExtractions_forDocument_withLineItems() {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("line-items.pdf")
        Assert.assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(
            testDocument, "application/pdf", "line-items.pdf",
            DocumentTaskManager.DocumentType.INVOICE
        ) { extractionsContainer: ExtractionsContainer? -> }
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]

        // All extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedback: MutableMap<String, SpecificExtraction?> = HashMap()
        feedback["iban"] = getIban(extractionsContainer!!)
        feedback["amountToPay"] = getAmountToPay(extractionsContainer)
        feedback["bic"] = getBic(extractionsContainer)
        feedback["paymentRecipient"] = getPaymentRecipient(extractionsContainer)
        val sendFeedback = giniCoreAPI.documentTaskManager.sendFeedbackForExtractions(document, feedback)
        sendFeedback.waitForCompletion()
        if (sendFeedback.isFaulted) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.error))
        }
        Assert.assertTrue("Sending feedback should be completed", sendFeedback.isCompleted)
        Assert.assertFalse("Sending feedback should be successful", sendFeedback.isFaulted)
    }

    @Test
    @Ignore("compound extractions are not working (07.10.2021)")
    @Throws(Exception::class)
    fun sendFeedback_withCompoundExtractions_forDocument_withLineItems() {
        val assetManager = getApplicationContext<Context>().resources.assets
        val testDocumentAsStream = assetManager.open("line-items.pdf")
        Assert.assertNotNull("test pdf line-items.pdf could not be loaded", testDocumentAsStream)
        val testDocument = TestUtils.createByteArray(testDocumentAsStream)
        val documentExtractions = processDocument(
            testDocument, "application/pdf", "line-items.pdf",
            DocumentTaskManager.DocumentType.INVOICE
        ) { extractionsContainer: ExtractionsContainer? -> }
        val document = documentExtractions.keys.iterator().next()
        val extractionsContainer = documentExtractions[document]
        val compoundExtractions = extractionsContainer!!.compoundExtractions

        // All specific extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedback: MutableMap<String, SpecificExtraction?> = HashMap()
        feedback["iban"] = getIban(extractionsContainer)
        feedback["amountToPay"] = getAmountToPay(extractionsContainer)
        feedback["bic"] = getBic(extractionsContainer)
        feedback["paymentRecipient"] = getPaymentRecipient(extractionsContainer)

        // All compound extractions are correct, that means we have nothing to correct and will only send positive feedback
        // we should only send feedback for extractions we have seen and accepted
        val feedbackCompound: MutableMap<String, CompoundExtraction?> = HashMap()
        feedbackCompound["lineItems"] = compoundExtractions["lineItems"]
        val sendFeedback =
            giniCoreAPI.documentTaskManager.sendFeedbackForExtractions(document, feedback, feedbackCompound)
        sendFeedback.waitForCompletion()
        if (sendFeedback.isFaulted) {
            Log.e("TEST", Log.getStackTraceString(sendFeedback.error))
        }
        Assert.assertTrue("Sending feedback should be completed", sendFeedback.isCompleted)
        Assert.assertFalse("Sending feedback should be successful", sendFeedback.isFaulted)
    }

    @Test
    @Throws(Exception::class)
    fun testResolvePayment() {
        val createPaymentTask = createPaymentRequest()
        createPaymentTask.waitForCompletion()
        val id = createPaymentTask.result
        val paymentRequestTask = giniCoreAPI.documentTaskManager.getPaymentRequest(id)
        paymentRequestTask.waitForCompletion()
        val (_, _, recipient, iban, _, amount, purpose) = paymentRequestTask.result
        val resolvePaymentInput = ResolvePaymentInput(
            recipient,
            iban, amount, purpose, null
        )
        val resolvePaymentRequestTask = giniCoreAPI.documentTaskManager.resolvePaymentRequest(id, resolvePaymentInput)
        resolvePaymentRequestTask.waitForCompletion()
        Assert.assertNotNull(resolvePaymentRequestTask.result)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPayment() {
        val createPaymentTask = createPaymentRequest()
        createPaymentTask.waitForCompletion()
        val id = createPaymentTask.result
        val paymentRequestTask = giniCoreAPI.documentTaskManager.getPaymentRequest(id)
        paymentRequestTask.waitForCompletion()
        val (_, _, recipient, iban, bic, amount, purpose) = paymentRequestTask.result
        val resolvePaymentInput = ResolvePaymentInput(
            recipient,
            iban, amount, purpose, null
        )
        val resolvePaymentRequestTask = giniCoreAPI.documentTaskManager.resolvePaymentRequest(id, resolvePaymentInput)
        resolvePaymentRequestTask.waitForCompletion()
        val getPaymentRequestTask = giniCoreAPI.documentTaskManager.getPayment(id)
        getPaymentRequestTask.waitForCompletion()
        Assert.assertNotNull(getPaymentRequestTask.result)
        Assert.assertEquals(recipient, getPaymentRequestTask.result.recipient)
        Assert.assertEquals(iban, getPaymentRequestTask.result.iban)
        Assert.assertEquals(bic, getPaymentRequestTask.result.bic)
        Assert.assertEquals(amount, getPaymentRequestTask.result.amount)
        Assert.assertEquals(purpose, getPaymentRequestTask.result.purpose)
    }

    @Test
    @Throws(Exception::class)
    fun logErrorEvent() {
        val errorEvent = ErrorEvent(
            Build.MODEL, "Android", Build.VERSION.RELEASE,
            "not available", BuildConfig.VERSION_NAME, "Error logging integration test"
        )
        val requestTask = giniCoreAPI.documentTaskManager.logErrorEvent(errorEvent)
        requestTask.waitForCompletion()
        assertNull(requestTask.error)
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
        val listTask = giniHealthAPI.documentTaskManager.paymentProviders
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
        return giniHealthAPI.documentTaskManager.createPaymentRequest(paymentRequest)
    }

    override fun createGiniCoreAPIBuilder(
        clientId: String,
        clientSecret: String,
        emailDomain: String
    ): GiniCoreAPIBuilder<BankApiDocumentTaskManager, BankApiDocumentManager, GiniBankAPI, BankApiCommunicator, ExtractionsContainer> {
        giniHealthAPI = GiniHealthAPIBuilder(getApplicationContext(), clientId, clientSecret, emailDomain).build()
        return GiniBankAPIBuilder(getApplicationContext(), clientId, clientSecret, emailDomain)
    }

    override fun getNetworkSecurityConfigResId(): Int = net.gini.android.bank.api.test.R.xml.network_security_config

    override fun getIban(extractionsContainer: ExtractionsContainer): SpecificExtraction? =
        extractionsContainer.specificExtractions["iban"]

    override fun getBic(extractionsContainer: ExtractionsContainer): SpecificExtraction? =
        extractionsContainer.specificExtractions["bic"]

    override fun getAmountToPay(extractionsContainer: ExtractionsContainer): SpecificExtraction? {
        return extractionsContainer.specificExtractions["amountToPay"]
    }

    override fun getPaymentRecipient(extractionsContainer: ExtractionsContainer): SpecificExtraction? {
        return extractionsContainer.specificExtractions["paymentRecipient"]
    }

    override fun getPaymentPurpose(extractionsContainer: ExtractionsContainer): SpecificExtraction? {
        return extractionsContainer.specificExtractions["paymentPurpose"]
    }

}
