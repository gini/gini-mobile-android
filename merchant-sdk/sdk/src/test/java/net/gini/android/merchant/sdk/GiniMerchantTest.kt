package net.gini.android.merchant.sdk

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.review.error.NoPaymentDataExtracted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

val document =
    Document("1234", Document.ProcessingState.COMPLETED, "", 1, Date(124), Date(124), Document.SourceClassification.COMPOSITE, Uri.EMPTY, emptyList(), emptyList())

val extractions = ExtractionsContainer(
    emptyMap(),
    mapOf(
        "payment" to CompoundExtraction("payment", listOf(mutableMapOf(
            "payment_recipient" to SpecificExtraction("payment_recipient", "recipient", "", null, listOf()),
            "iban" to SpecificExtraction("iban", "iban", "", null, listOf()),
            "amount_to_pay" to SpecificExtraction("amount_tp_pay", "123.56", "", null, listOf()),
            "payment_purpose" to SpecificExtraction("payment_purpose", "purpose", "", null, listOf()),
        )))
    )
)

fun copyExtractions(extractions: ExtractionsContainer) = ExtractionsContainer(
    extractions.specificExtractions.toMap(),
    extractions.compoundExtractions.map { (name, compoundExtraction) ->
        name to CompoundExtraction(name, compoundExtraction.specificExtractionMaps.map { it.toMap() })
    }.toMap()
)

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class GiniMerchantTest {

    private var context: Context? = null
    private lateinit var giniMerchant: GiniMerchant
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk(relaxed = true) { HealthApiDocumentManager::class.java }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context!!.setTheme(R.style.GiniMerchantTheme)
        every { giniHealthAPI.documentManager } returns documentManager
        giniMerchant = GiniMerchant(mockk(relaxed = true)).apply {
            replaceHealthApiInstance(this@GiniMerchantTest.giniHealthAPI)
        }
        val paymentComponent = PaymentComponent(context!!, giniHealthAPI)
        giniMerchant.paymentComponent = paymentComponent
    }

    @Test
    fun `Document is payable if it has an IBAN extraction`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertTrue(giniMerchant.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has no IBAN extraction`() = runTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.remove("iban")
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithoutIBAN)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniMerchant.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has an empty IBAN extraction`() = runTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            compoundExtractions["payment"]?.specificExtractionMaps?.get(0)
                ?.set("iban", SpecificExtraction("iban", "", "", null, listOf()))
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithoutIBAN)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniMerchant.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document payable check throws an exception if get document API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(exception = Exception("Failed to get document"))

        var exception: Exception? = null

        try {
            giniMerchant.checkIfDocumentIsPayable(document.id)
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        Truth.assertThat(exception!!.message).contains("Failed to get document")
    }

    @Test
    fun `Document payable check throws an exception if get extractions API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Error(exception = Exception("Failed to get extractions"))
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        var exception: Exception? = null

        try {
            giniMerchant.checkIfDocumentIsPayable(document.id)
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        Truth.assertThat(exception!!.message).contains("Failed to get extractions")
    }
}