package net.gini.android.capture.onboarding

import android.content.Context
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
import android.os.Parcel
import org.mockito.Mockito
import net.gini.android.capture.onboarding.OnboardingPage
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.parcelize.Parcelize
import net.gini.android.capture.test.Helpers
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingPageTest {

    @Test
    fun `is parcelable`() {
        // Given
        val toParcel = OnboardingPage(314, 42, OnboardingIllustrationAdapterFixture())
        // When
        val fromParcel = Helpers.doParcelingRoundTrip(toParcel, OnboardingPage.CREATOR)
        // Then
        Truth.assertThat(toParcel.titleResId).isEqualTo(fromParcel.titleResId)
        Truth.assertThat(toParcel.messageResId).isEqualTo(fromParcel.messageResId)
        Truth.assertThat(toParcel.illustrationAdapter?.getView(mock())?.id)
            .isEqualTo(fromParcel.illustrationAdapter?.getView(mock())?.id)
    }

    @Parcelize
    class OnboardingIllustrationAdapterFixture: OnboardingIllustrationAdapter {
        override fun onVisible() {}
        override fun onHidden() {}
        override fun getView(container: ViewGroup): View = mock<View>().also { view ->
            whenever(view.id).thenReturn(42)
        }
        override fun onDestroy() {}
    }
}