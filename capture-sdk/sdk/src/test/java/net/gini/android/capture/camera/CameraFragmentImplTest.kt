package net.gini.android.capture.camera

import android.app.Activity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.*
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.internal.camera.api.CameraInterface
import net.gini.android.capture.internal.ui.FragmentImplCallback
import net.gini.android.capture.internal.util.CancelListener
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEventTracker
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class CameraFragmentImplTest {

    @Test
    fun `triggers Take Picture event`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        val fragmentImpl = object : CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false) {
            override fun createCameraController(activity: Activity?): CameraInterface {
                return mock<CameraInterface>().apply {
                    whenever(isPreviewRunning).thenReturn(true)
                    whenever(takePicture()).thenReturn(CompletableFuture.completedFuture(mock()))
                }
            }
        }
        fragmentImpl.initCameraController(mock())

        // When
        fragmentImpl.onCameraTriggerClicked()

        // Then
        verify(eventTracker).onCameraScreenEvent(Event(CameraScreenEvent.TAKE_PICTURE))
    }

    @Test
    fun `triggers Help event when help was started`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // Stub the fragment transaction related calls
        val fragmentCallbackStub = mock<FragmentImplCallback>()
        whenever(fragmentCallbackStub.childFragmentManager).thenReturn(object: FragmentManager() {
            override fun beginTransaction(): FragmentTransaction {
                return object: FragmentTransaction() {
                    override fun add(containerViewId: Int, fragment: Fragment, tag: String?): FragmentTransaction {
                        return this;
                    }

                    override fun addToBackStack(name: String?): FragmentTransaction {
                        return this
                    }

                    override fun commit(): Int {
                        return 0
                    }

                    override fun commitAllowingStateLoss(): Int {
                        return 0
                    }

                    override fun commitNow() {
                    }

                    override fun commitNowAllowingStateLoss() {
                    }
                }
            }
        })
        whenever(fragmentCallbackStub.findNavController()).thenReturn(mock())

        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(fragmentCallbackStub, mock(), false)

        val noPermissionLayoutMock = mock<ConstraintLayout> {
            on { visibility } doReturn View.INVISIBLE
        }
        val analyticsTrackerMock = mock<UserAnalyticsEventTracker> {
            on { trackEvent(any()) }.thenReturn(true)
            on { trackEvent(any(), any()) }.thenReturn(true)
        }

        fragmentImpl.mLayoutNoPermission = noPermissionLayoutMock
        fragmentImpl.mUserAnalyticsEventTracker = analyticsTrackerMock

        // When
        fragmentImpl.startHelpActivity()

        // Then
        verify(eventTracker).onCameraScreenEvent(Event(CameraScreenEvent.HELP))
    }

    private open class CameraFragmentImplWithoutQRCodeReader(fragment: FragmentImplCallback,
                                                        cancelListener: CancelListener, addPages: Boolean
    ) : CameraFragmentImpl(fragment, cancelListener, addPages) {
            override fun initQRCodeReader() {
                // Do nothing, because no QR code reader is available in JVM tests
            }
    }
}