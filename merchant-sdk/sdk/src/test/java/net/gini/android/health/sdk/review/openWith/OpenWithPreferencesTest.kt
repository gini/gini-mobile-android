package net.gini.android.merchant.sdk.review.openWith

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import org.junit.Test


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class OpenWithPreferencesTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `increments open with counter`() = runTest {
        // Given
        val openWithPreferences = OpenWithPreferences(context)
        val paymentProviderId = "test_payment_provider"

        openWithPreferences.getLiveCountForPaymentProviderId(paymentProviderId).test {
            val initialCount = awaitItem()
            assertThat(initialCount).isNull()

            // When
            openWithPreferences.incrementCountForPaymentProviderId(paymentProviderId)

            // Then
            val updatedCount = awaitItem()
            assertThat(updatedCount).isEqualTo(1)
        }
    }
}