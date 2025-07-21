package net.gini.android.capture.camera

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalytics
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
//TODO: remove after upgrading to robolectric to 4.16
@Config(
    maxSdk = 35,
)
class CameraFragmentTest {

    @Test
    fun `triggers Exit event when back was pressed`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()
        UserAnalytics.initialize(InstrumentationRegistry.getInstrumentation().context)

        val navController = mock<NavController>()

        FragmentScenario.launchInContainer(fragmentClass = CameraFragmentWithoutQRCodeReader::class.java,
            factory = object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return CameraFragmentWithoutQRCodeReader().apply {
                        setListener(mock())
                        setCancelListener(mock())
                    }.also { fragment ->
                        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                            if (viewLifecycleOwner != null) {
                                // The fragment’s view has just been created
                                Navigation.setViewNavController(fragment.requireView(), navController)
                            }
                        }
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

    internal class CameraFragmentWithoutQRCodeReader : CameraFragment() {
        override fun createFragmentImpl(cancelListener: CancelListener, addPages: Boolean): CameraFragmentImpl {
            return object : CameraFragmentImpl(this, cancelListener, false) {
                override fun initQRCodeReader() {
                    // Do nothing, because no QR code reader is available in JVM tests
                }
            }
        }
    }
}
