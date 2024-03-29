package net.gini.android.health.sdk.paymentComponent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentPreferences
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PaymentComponentPreferencesTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `sets selected payment provider app`() = runTest {
        // Given
        val paymentComponentPreferences = PaymentComponentPreferences(context)

        assertThat(paymentComponentPreferences.getSelectedPaymentProviderId()).isNull()

        // When
        paymentComponentPreferences.saveSelectedPaymentProviderId("123")

        // Then
        assertThat(paymentComponentPreferences.getSelectedPaymentProviderId()).isEqualTo("123")
    }

    @Test
    fun `deletes selected payment provider app`() = runTest {
        // Given
        val paymentComponentPreferences = PaymentComponentPreferences(context)

        assertThat(paymentComponentPreferences.getSelectedPaymentProviderId()).isNull()
        paymentComponentPreferences.saveSelectedPaymentProviderId("123")
        assertThat(paymentComponentPreferences.getSelectedPaymentProviderId()).isEqualTo("123")

        // When
        paymentComponentPreferences.deleteSelectedPaymentProviderId()

        // Then
        assertThat(paymentComponentPreferences.getSelectedPaymentProviderId()).isNull()
    }
}