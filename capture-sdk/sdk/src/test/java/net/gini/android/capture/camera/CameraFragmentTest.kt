package net.gini.android.capture.camera

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class CameraFragmentTest {

    @Test
    fun `triggers Exit event when back was pressed`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        FragmentScenario.launchInContainer(fragmentClass = CameraFragment::class.java,
            themeResId = R.style.GiniCaptureTheme,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return CameraFragment().apply {
                        setListener(mock())
                    }
                }
            }).use { scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            // When
            scenario.onFragment { fragment ->
                fragment.requireActivity().onBackPressedDispatcher.onBackPressed()

                // Then
                verify(eventTracker).onCameraScreenEvent(Event(CameraScreenEvent.EXIT))
            }
        }
    }

}
