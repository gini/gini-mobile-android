package net.gini.android.capture.onboarding

import android.app.Application
import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.launchFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplication
import java.util.*

/**
 * Created by Alpar Szotyori on 21.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFragmentCompatTest {

    @Test(expected = IllegalStateException::class)
    fun should_throwException_whenListener_wasNotSet() {
        // Given
        val pages = ArrayList<OnboardingPage>()
        pages.add(
            OnboardingPage(
                R.string.gc_onboarding_flat,
                R.drawable.gc_onboarding_flat
            )
        )

        launchFragment {
            OnboardingFragmentCompat.createInstance(pages)
        }
    }
}