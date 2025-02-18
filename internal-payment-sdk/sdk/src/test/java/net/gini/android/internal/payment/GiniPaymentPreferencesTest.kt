package net.gini.android.internal.payment

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.response.CommunicationTone
import net.gini.android.internal.payment.utils.GiniLocalization
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GiniPaymentPreferencesTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = getApplicationContext()
    }

    @Test
    fun `sets returning user`() = runTest {
        // Given
        val giniPaymentPreferencesTest = GiniInternalPaymentModule.GiniPaymentPreferences(context)

        assertThat(giniPaymentPreferencesTest.getReturningUser()).isFalse()

        // When
        giniPaymentPreferencesTest.saveReturningUser()

        // Then
        assertThat(giniPaymentPreferencesTest.getReturningUser()).isTrue()
    }

    @Test
    fun `sets SDK language`() = runTest {
        // Given
        val giniPaymentPreferencesTest = GiniInternalPaymentModule.GiniPaymentPreferences(context)

        assertThat(giniPaymentPreferencesTest.getSDKLanguage()).isNull()

        // When
        giniPaymentPreferencesTest.saveSDKLanguage(GiniLocalization.GERMAN)

        // Then
        assertThat(giniPaymentPreferencesTest.getSDKLanguage()).isEqualTo(GiniLocalization.GERMAN)
    }

    @Test
    fun `sets SDK communication tone`() = runTest {
        // Given
        val giniPaymentPreferencesTest = GiniInternalPaymentModule.GiniPaymentPreferences(context)

        // When
        giniPaymentPreferencesTest.saveSDKCommunicationTone(CommunicationTone.FORMAL.name)

        // Then
        assertThat(giniPaymentPreferencesTest.getSDKCommunicationTone()).isEqualTo(CommunicationTone.FORMAL)
    }

    @Test
    fun `sets the language is overridden `() = runTest {
        // Given
        val giniPaymentPreferencesTest = GiniInternalPaymentModule.GiniPaymentPreferences(context)

        // When
        giniPaymentPreferencesTest.saveLanguageOverriddenByUser(true)

        // Then
        assertThat(giniPaymentPreferencesTest.getLanguageOverriddenByUser()).isEqualTo(true)
    }
}