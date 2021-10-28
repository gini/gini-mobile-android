package net.gini.android.capture.camera

import android.view.MenuItem
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.R
import net.gini.android.capture.tracking.CameraScreenEvent
import net.gini.android.capture.tracking.Event
import net.gini.android.capture.tracking.EventTracker
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpar Szotyori on 02.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@RunWith(AndroidJUnit4::class)
class CameraActivityTest {

    @After
    fun after() {
        GiniCapture.cleanup(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @Test
    fun `triggers Exit event when back was pressed`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        ActivityScenario.launch(CameraActivity::class.java).use {scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            // When
            scenario.onActivity {activity ->
                activity.onBackPressed()

                // Then
                verify(eventTracker).onCameraScreenEvent(Event(CameraScreenEvent.EXIT))
            }
        }
    }

    @Test
    fun `triggers Help event when help was started`() {
        // Given
        val eventTracker = spy<EventTracker>()
        GiniCapture.Builder().setEventTracker(eventTracker).build()

        ActivityScenario.launch(CameraActivity::class.java).use {scenario ->
            scenario.moveToState(Lifecycle.State.STARTED)

            // When
            scenario.onActivity {activity ->
                val menuItem = mock<MenuItem>()
                whenever(menuItem.itemId).thenReturn(R.id.gc_action_show_onboarding)
                activity.onOptionsItemSelected(menuItem)

                // Then
                verify(eventTracker).onCameraScreenEvent(Event(CameraScreenEvent.HELP))
            }
        }
    }

}
