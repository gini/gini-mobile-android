package net.gini.android.bank.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import net.gini.android.bank.api.test.ExtractionsFixture
import net.gini.android.bank.api.test.fromJsonAsset
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Created by Alpár Szotyori on 16.11.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class ExtractionFeedbackIntegrationTest {

    private lateinit var giniBankAPI: GiniBankAPI
    private val moshi = Moshi.Builder().build()

    @Before
    fun setUp() {
        val testProperties = Properties().apply {
            getApplicationContext<Context>().resources.assets
                .open("test.properties").use { load(it) }
        }
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
    fun sendExtzractionFeedback() = runBlocking {
        val documentManager = giniBankAPI.documentManager

        // 1. Upload a test document
        val pdfBytes = getApplicationContext<Context>().resources.assets
            .open("Gini_invoice_example.pdf").use { it.readBytes() }

        val partialDocument = documentManager.createPartialDocument(pdfBytes, "application/pdf")
        val compositeDocument = documentManager.createCompositeDocument(listOf(partialDocument))

        // 2. Request the extractions
        val extractions = documentManager.getExtractions(compositeDocument)

        //    Verify we received the correct extractions for this test
        val extractionsFixture = moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example.json")!!
        assertTrue(extractionsFixture.equals(extractions))

        // 3. Assuming the user saw the following extractions:
        //    amountToPay, iban, bic, paymentPurpose and paymentRecipient

        //    Supposing the user changed the amountToPay from "995.00:EUR" to "950.00:EUR"
        //    we need to update that extraction
        extractions.specificExtractions["amountToPay"]!!.value = "950.00:EUR"

        //    Send feedback for the extractions the user saw
        //    with the final (user confirmed or updated) extraction values
        documentManager.sendFeedback(
            compositeDocument,
            mapOf(
                "amountToPay" to extractions.specificExtractions["amountToPay"]!!,
                "iban" to extractions.specificExtractions["iban"]!!,
                "bic" to extractions.specificExtractions["bic"]!!,
                "paymentPurpose" to extractions.specificExtractions["paymentPurpose"]!!,
                "paymentRecipient" to extractions.specificExtractions["paymentRecipient"]!!
            ), emptyMap()
        )

        // 4. Verify that the extractions were updated
        val extractionsAfterFeedback = documentManager.getExtractions(compositeDocument)

        val extractionsAfterFeedbackFixture =
            moshi.fromJsonAsset<ExtractionsFixture>("result_Gini_invoice_example_after_feedback.json")!!
        assertTrue(extractionsAfterFeedbackFixture.equals(extractionsAfterFeedback))
    }
}