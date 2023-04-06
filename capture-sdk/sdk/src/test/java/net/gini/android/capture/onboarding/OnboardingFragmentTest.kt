package net.gini.android.capture.onboarding

import android.content.pm.ActivityInfo
import androidx.fragment.app.testing.launchFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.mock
import net.gini.android.capture.R
import net.gini.android.capture.test.Helpers
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Created by Alpar Szotyori on 21.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFragmentTest {

    @Test(expected = IllegalStateException::class)
    fun `throws exception when listener was not set`() {
        // Given
        val pages = ArrayList<OnboardingPage>()
        pages.add(
            OnboardingPage(
                R.string.gc_onboarding_align_corners_title,
                R.string.gc_onboarding_align_corners_message,
                null
            )
        )

        launchFragment {
            OnboardingFragment.createInstance(pages)
        }
    }

    @Test
    @Config(qualifiers = "sw400dp-xxhdpi")
    fun `force portrait orientation on phones`() {
        // Given
        Assume.assumeTrue(!Helpers.isTablet())

        // When
        launchOnboardingFragment().onFragment { onboardingFragment ->
            // Then
            Truth.assertThat(onboardingFragment.activity?.requestedOrientation)
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    private fun launchOnboardingFragment() = launchFragment(themeResId = R.style.GiniCaptureTheme) {
        OnboardingFragment().apply {
            setListener(mock())
        }
    }
}