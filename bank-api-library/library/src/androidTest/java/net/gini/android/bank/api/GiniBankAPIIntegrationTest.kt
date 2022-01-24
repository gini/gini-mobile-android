package net.gini.android.bank.api

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import bolts.Task
import net.gini.android.core.api.DocumentTaskManager
import net.gini.android.core.api.test.shared.helpers.TestUtils
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.core.api.test.shared.GiniCoreAPIIntegrationTest
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.PaymentRequestInput
import net.gini.android.core.api.models.ResolvePaymentInput
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.GiniHealthAPIBuilder
import org.junit.Assert
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
class GiniBankAPIIntegrationTest: GiniCoreAPIIntegrationTest<GiniBankAPI>() {

    private lateinit var giniHealthAPI: GiniHealthAPI

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
    ): GiniCoreAPIBuilder<GiniBankAPI> {
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
