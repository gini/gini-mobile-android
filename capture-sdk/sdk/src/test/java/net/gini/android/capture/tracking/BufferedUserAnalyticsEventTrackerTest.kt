package net.gini.android.capture.tracking

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import net.gini.android.capture.tracking.useranalytics.BufferedUserAnalyticsEventTracker
import net.gini.android.capture.tracking.useranalytics.UserAnalyticsEvent
import net.gini.android.capture.tracking.useranalytics.properties.UserAnalyticsUserProperty
import net.gini.android.capture.tracking.useranalytics.tracker.AmplitudeUserAnalyticsEventTracker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Tests the behavior of [BufferedUserAnalyticsEventTracker].
 *
 * If userJourney is disabled, no tracker is added and events/properties return false.
 * If enabled, trackers are added and methods return true.
 *
 * Each test verifies this behavior by checking method results instead of internal queues.
 */

@RunWith(AndroidJUnit4::class)
class BufferedUserAnalyticsEventTrackerTest {

    private lateinit var tracker: BufferedUserAnalyticsEventTracker
    private val mockContext: Context = mockk(relaxed = true)

    // Random testSessionId for testing
    private val testSessionId = "test-session"

    // Random api key for testing
    private val testApiKey = "test-api-key"

    // This is an arbitrary version for testing
    private val testCaptureVersion = "3.10.1"

    @Before
    fun setup() {
        tracker = BufferedUserAnalyticsEventTracker(mockContext, testSessionId)
    }

    @Test
    fun `trackEvent returns false when userJourney is disabled`() {
        setupTrackerWithUserJourney(isUserJourneyEnabled = false)

        val result = tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED)

        assertFalse(result)
    }


    @Test
    fun `trackEvent returns true when userJourney is enabled`() {
        setupTrackerWithUserJourney(isUserJourneyEnabled = true)

        val result = tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED)

        assertTrue(result)
    }


    @Test
    fun `setEventSuperProperty returns false when userJourney is disabled`() {
        setupTrackerWithUserJourney(isUserJourneyEnabled = false)

        val result = tracker.setEventSuperProperty(emptySet())

        assertFalse(result)
    }

    @Test
    fun `setEventSuperProperty returns true when userJourney is enabled`() {
        setupTrackerWithUserJourney(isUserJourneyEnabled = true)

        val result = tracker.setEventSuperProperty(emptySet())

        assertTrue(result)
    }

    @Test
    fun `trackEvent with properties returns false when userJourney is disabled`() {
        setupTrackerWithUserJourney(isUserJourneyEnabled = false)

        val result = tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED, emptySet())

        assertFalse(result)
    }

    @Test
    fun `trackEvent with properties returns true when userJourney is enabled`() {
        setupTrackerWithUserJourney(isUserJourneyEnabled = true)

        val result = tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED, emptySet())

        assertTrue(result)
    }

    private fun setupTrackerWithUserJourney(isUserJourneyEnabled: Boolean) {

        tracker.setPlatformTokens(
            AmplitudeUserAnalyticsEventTracker.AmplitudeAnalyticsApiKey(testApiKey),
            networkRequestsManager = mockk(relaxed = true),
            isUserJourneyEnabled = isUserJourneyEnabled
        )

        tracker.setUserProperty(
            setOf(
                UserAnalyticsUserProperty.CaptureSdkVersionName(testCaptureVersion)
            )
        )
    }
}
