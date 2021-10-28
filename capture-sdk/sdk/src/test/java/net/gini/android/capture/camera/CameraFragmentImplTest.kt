package net.gini.android.capture.camera

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.*
import jersey.repackaged.jsr166e.CompletableFuture
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureError
import net.gini.android.capture.internal.camera.api.CameraInterface
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class CameraFragmentImplTest {

    @After
    fun after() {
        GiniCapture.cleanup(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun `triggers Take Picture event`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        val fragmentImpl = object: CameraFragmentImpl(mock()) {
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
    fun `notifies listener of error when GiniInstance is missing`() {
        // Given
        val fragmentImpl = CameraFragmentImpl(mock())

        val listener = mock<CameraFragmentListener>()
        fragmentImpl.setListener(listener)

        // When
        fragmentImpl.onStart()

        // Then
        val args = argumentCaptor<GiniCaptureError>()
        Mockito.verify(listener).onError(args.capture())
        Truth.assertThat(args.firstValue.errorCode)
            .isEqualTo(GiniCaptureError.ErrorCode.MISSING_GINI_CAPTURE_INSTANCE)
    }
}