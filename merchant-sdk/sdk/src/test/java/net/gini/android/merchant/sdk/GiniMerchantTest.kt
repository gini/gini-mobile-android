package net.gini.android.merchant.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

    @Test(expected = IllegalStateException::class)
    fun `throws IllegalStateException when trying to create PaymentFragment with incomplete information`() = runTest {
        giniMerchant.createFragment("", "", "30", "")
    }

    @Test
    fun `creates PaymentFragment when non-empty values are used`() = runTest {
        // When
        val paymentFragment = giniMerchant.createFragment("1234", "recipient", "30", "purpose")

        // Then
        assertThat(paymentFragment).isNotNull()
    }
}