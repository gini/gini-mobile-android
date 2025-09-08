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
 * How do our analytics work?
 *
 * We are initializing the [BufferedUserAnalyticsEventTracker] and it adds all the trackers through
 * [BufferedUserAnalyticsEventTracker.setPlatformTokens] into a set.
 * If the config value (isUserJourneyEnabled) from the backend is false, no tracker should be
 * added to the set, and eventually no event will be tracked.
 *
 * If the value is true, the passed tracker (e.g [AmplitudeUserAnalyticsEventTracker])
 * will be added to the set and all the events will be tracked.
 *
 * This class will test this behaviour of [BufferedUserAnalyticsEventTracker]. If we pass false,
 * No tracker should be added and vice versa.
 *
 * Things to consider:
 * We are using queues to store the events, user properties and super properties.
 * We can not test by checking these queues, because they are private, we can expose them for
 * testing, but the main problem is that we are using polling for the queues
 * in [BufferedUserAnalyticsEventTracker.trySendEvents] which empties the queues, so even if we
 * expose them, it will be always empty after trackEvent is called,
 * on the other hand, the trackers serve the same thing. If the tracker is empty, it
 * already means that no events will be tracked. That's why every test of this class will check
 * the trackers set. For all positive and negative cases.
 *
 * */

@RunWith(AndroidJUnit4::class)
class BufferedUserAnalyticsEventTrackerTest {

    private lateinit var tracker: BufferedUserAnalyticsEventTracker
    private val mockContext: Context = mockk(relaxed = true)
    private val testSessionId = "test-session"
    private val testApiKey = "test-api-key"
    private val testCaptureVersion = "3.10.1"

    @Before
    fun setup() {
        tracker = BufferedUserAnalyticsEventTracker(mockContext, testSessionId)
    }

    @Test
    fun `when userJourney disabled, Initialize does not add trackers`() {
        initializeTracker(isUserJourneyEnabled = false)

        tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED)

        assertTrue(tracker.getTrackers().isEmpty())
    }


    @Test
    fun `when userJourney enabled, Initialize does add trackers`() {
        initializeTracker(isUserJourneyEnabled = true)

        tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED)

        assertFalse(tracker.getTrackers().isEmpty())
    }


    @Test
    fun `when userJourney disabled, setEventSuperProperty does not add trackers`() {
        initializeTracker(isUserJourneyEnabled = false)

        tracker.setEventSuperProperty(emptySet())

        assertTrue(tracker.getTrackers().isEmpty())
    }

    @Test
    fun `when userJourney enabled, setEventSuperProperty does add trackers`() {
        initializeTracker(isUserJourneyEnabled = true)

        tracker.setEventSuperProperty(emptySet())

        assertFalse(tracker.getTrackers().isEmpty())
    }

    @Test
    fun `when userJourney disabled, trackEvent does not add tracker`() {
        initializeTracker(isUserJourneyEnabled = false)

        tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED, emptySet())

        assertTrue(tracker.getTrackers().isEmpty())
    }

    @Test
    fun `when userJourney enabled, trackEvent does add tracker`() {
        initializeTracker(isUserJourneyEnabled = true)

        tracker.trackEvent(UserAnalyticsEvent.SDK_OPENED, emptySet())

        assertFalse(tracker.getTrackers().isEmpty())
    }

    private fun initializeTracker(isUserJourneyEnabled: Boolean) {

        tracker.setPlatformTokens(
            AmplitudeUserAnalyticsEventTracker.AmplitudeAnalyticsApiKey(testApiKey),
            networkRequestsManager = mockk(relaxed = true),
            isUserJourneyEnabled = isUserJourneyEnabled
        )

        tracker.setUserProperty(
            setOf(
                UserAnalyticsUserProperty.CaptureSdkVersionName(testCaptureVersion),
            )
        )
    }
}
