package net.gini.android.capture.tracking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.*
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.tracking.EventTrackingHelper.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class EventTrackingHelperTest {

    @After
    fun after() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `track Onboarding Screen events when GiniCapture instance is available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        trackOnboardingScreenEvent(OnboardingScreenEvent.START, mapOf("detail" to "Event detail"))

        // Then
        verify(eventTracker).onOnboardingScreenEvent(eq(Event(OnboardingScreenEvent.START, mapOf("detail" to "Event detail"))))
    }

    @Test
    fun `don't track Onboarding Screen events when GiniCapture instance is not available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        GiniCaptureHelper.setGiniCaptureInstance(null)

        trackOnboardingScreenEvent(OnboardingScreenEvent.START)

        // Then
        verify(eventTracker, never()).onOnboardingScreenEvent(any())
    }

    @Test
    fun `track Camera Screen events when GiniCapture instance is available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        trackCameraScreenEvent(CameraScreenEvent.TAKE_PICTURE, mapOf("detail" to "Event detail"))

        // Then
        verify(eventTracker).onCameraScreenEvent(eq(Event(CameraScreenEvent.TAKE_PICTURE, mapOf("detail" to "Event detail"))))
    }

    @Test
    fun `don't track Camera Screen events when GiniCapture instance is not available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        GiniCaptureHelper.setGiniCaptureInstance(null)

        trackCameraScreenEvent(CameraScreenEvent.TAKE_PICTURE)

        // Then
        verify(eventTracker, never()).onCameraScreenEvent(any())
    }

    @Test
    fun `track Review Screen events when GiniCapture instance is available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        trackReviewScreenEvent(ReviewScreenEvent.NEXT, mapOf("detail" to "Event detail"))

        // Then
        verify(eventTracker).onReviewScreenEvent(eq(Event(ReviewScreenEvent.NEXT, mapOf("detail" to "Event detail"))))
    }

    @Test
    fun `don't track Review Screen events when GiniCapture instance is not available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        GiniCaptureHelper.setGiniCaptureInstance(null)

        trackReviewScreenEvent(ReviewScreenEvent.NEXT)

        // Then
        verify(eventTracker, never()).onReviewScreenEvent(any())
    }

    @Test
    fun `track Analysis Screen events when GiniCapture instance is available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        trackAnalysisScreenEvent(AnalysisScreenEvent.RETRY, mapOf("detail" to "Event detail"))

        // Then
        verify(eventTracker).onAnalysisScreenEvent(eq(Event(AnalysisScreenEvent.RETRY, mapOf("detail" to "Event detail"))))
    }

    @Test
    fun `don't track Analysis Screen events when GiniCapture instance is not available`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        // When
        GiniCaptureHelper.setGiniCaptureInstance(null)

        trackAnalysisScreenEvent(AnalysisScreenEvent.RETRY)

        // Then
        verify(eventTracker, never()).onAnalysisScreenEvent(any())
    }
}