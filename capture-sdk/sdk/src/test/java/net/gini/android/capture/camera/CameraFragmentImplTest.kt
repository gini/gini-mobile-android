package net.gini.android.capture.camera

import android.app.Activity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

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

    @Test
    fun `does not reset camera frame color when no IBANs are detected while unsupported QR popup is shown`() {
        // Given: the unsupported QR code popup is shown (e.g. restored after a rotation)
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn true }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: a camera frame without IBANs is processed
        fragmentImpl.handleIBANsDetected(emptyList())

        // Then: the frame color set by the popup is left untouched, but the IBAN label is hidden
        verify(fragmentImpl.mImageFrame, never()).setImageTintList(any())
        verify(fragmentImpl.mIbanDetectedTextView).visibility = View.GONE
    }

    @Test
    fun `resets camera frame color when no IBANs are detected and no QR popup is shown`() {
        // Given: no QR code popup is shown
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val fragmentCallback = mock<FragmentImplCallback> {
            on { activity } doReturn activityController.get()
        }
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(fragmentCallback, mock<CancelListener>(), false)
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mPaymentQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: a camera frame without IBANs is processed
        fragmentImpl.handleIBANsDetected(emptyList())

        // Then: the frame color is reset to the default and the IBAN label is hidden
        verify(fragmentImpl.mImageFrame).setImageTintList(any())
        verify(fragmentImpl.mIbanDetectedTextView).visibility = View.GONE
    }

    @Test
    fun `does not dismiss unsupported QR dialog when IBANs are detected while it is shown`() {
        // Given: the unsupported QR code dialog (new warning) is visible
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        fragmentImpl.mIsUnsupportedQRDialogShowing = true
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn true }
        fragmentImpl.mPaymentQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: an in-flight recognition result with IBANs arrives
        fragmentImpl.handleIBANsDetected(listOf("DE75 5121 0800 1245 1261 99"))

        // Then: the dialog stays visible and the IBAN overlay is not drawn over it
        verify(fragmentImpl.mUnsupportedQRCodePopup, never()).hide()
        verify(fragmentImpl.mIbanDetectedTextView, never()).visibility = View.VISIBLE
        verify(fragmentImpl.mImageFrame, never()).setImageTintList(any())
    }

    @Test
    fun `dismisses unsupported QR popup when IBANs are detected while legacy warning is shown`() {
        // Given: the legacy unsupported QR banner is visible (no dialog)
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        val fragmentCallback = mock<FragmentImplCallback> {
            on { activity } doReturn activityController.get()
        }
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(fragmentCallback, mock<CancelListener>(), false)
        fragmentImpl.mIsUnsupportedQRDialogShowing = false
        fragmentImpl.mUnsupportedQRCodePopup = mock { on { isShown } doReturn true }
        fragmentImpl.mPaymentQRCodePopup = mock { on { isShown } doReturn false }
        fragmentImpl.mImageFrame = mock()
        fragmentImpl.mIbanDetectedTextView = mock()

        // When: IBANs are detected
        fragmentImpl.handleIBANsDetected(listOf("DE75 5121 0800 1245 1261 99"))

        // Then: the banner is hidden and the IBAN overlay is shown (pre-existing behavior)
        verify(fragmentImpl.mUnsupportedQRCodePopup).hide()
        verify(fragmentImpl.mIbanDetectedTextView).visibility = View.VISIBLE
    }

    @Test
    fun `resets QR code state when unsupported popup hides so scanning resumes`() {
        // Given: state is set as if an unsupported QR code was shown and blocked further scans
        val fragmentImpl = CameraFragmentImplWithoutQRCodeReader(mock(), mock<CancelListener>(), false)
        fragmentImpl.mQRCodeContent = "unsupported-qr-content"
        fragmentImpl.mInterfaceHidden = true

        // When: the unsupported QR popup hides
        fragmentImpl.onUnsupportedQRCodePopupHidden()

        // Then: state is reset so subsequent QR codes can be detected
        assertNull(fragmentImpl.mQRCodeContent)
        assertFalse(fragmentImpl.mInterfaceHidden)
    }

    private open class CameraFragmentImplWithoutQRCodeReader(fragment: FragmentImplCallback,
                                                        cancelListener: CancelListener, addPages: Boolean
    ) : CameraFragmentImpl(fragment, cancelListener, addPages) {
            override fun initQRCodeReader() {
                // Do nothing, because no QR code reader is available in JVM tests
            }
    }
}