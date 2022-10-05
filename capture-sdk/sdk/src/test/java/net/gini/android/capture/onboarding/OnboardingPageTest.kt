package net.gini.android.capture.onboarding

import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
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
        Truth.assertThat(toParcel.illustrationAdapter?.onCreateView(mock())?.id)
            .isEqualTo(fromParcel.illustrationAdapter?.onCreateView(mock())?.id)
    }

    @Parcelize
    class OnboardingIllustrationAdapterFixture: OnboardingIllustrationAdapter {
        override fun onVisible() {}
        override fun onHidden() {}
        override fun onCreateView(container: ViewGroup): View = mock<View>().also { view ->
            whenever(view.id).thenReturn(42)
        }
        override fun onDestroy() {}
    }
}