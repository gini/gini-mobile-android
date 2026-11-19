package net.gini.android.capture.ui.components.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders [ZoomableLazyColumn] under Robolectric and drives its transform gestures with injected
 * touches. The scale reported through `onScaleChanged` is the observable outcome.
 */
@RunWith(AndroidJUnit4::class)
class ZoomableLazyColumnTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val reportedScales = mutableListOf<Float>()

    private val currentScale: Float
        get() {
            composeRule.waitForIdle()
            return reportedScales.last()
        }

    @Test
    fun `shows its items and reports the minimum scale initially`() {
        setList()

        composeRule.onNodeWithText("Item 0").assertIsDisplayed()
        assertThat(currentScale).isEqualTo(MIN_SCALE)
    }

    @Test
    fun `pinching out zooms in up to the maximum scale`() {
        setList()

        pinchOut()
        assertThat(currentScale).isGreaterThan(MIN_SCALE)

        repeat(PINCH_REPETITIONS) { pinchOut() }
        assertThat(currentScale).isEqualTo(MAX_SCALE)
    }

    @Test
    fun `pinching in does not zoom out below the minimum scale`() {
        setList()
        pinchOut()

        repeat(PINCH_REPETITIONS) { pinchIn() }

        assertThat(currentScale).isEqualTo(MIN_SCALE)
    }

    @Test
    fun `double tap zooms in and a second double tap zooms back out`() {
        setList()

        doubleTap()
        assertThat(currentScale).isEqualTo(MIN_SCALE + DOUBLE_TAP_SCALE_DELTA)

        doubleTap()
        assertThat(currentScale).isEqualTo(MIN_SCALE)
    }

    @Test
    fun `panning a zoomed list keeps the scale`() {
        setList()
        doubleTap()
        val zoomedScale = currentScale

        composeRule.mainClock.advanceTimeBy(PAUSE_BETWEEN_GESTURES_MILLIS)
        list().performTouchInput { swipeLeft() }

        assertThat(currentScale).isEqualTo(zoomedScale)
        list().assertIsDisplayed()
    }

    @Test
    fun `keeps zooming with the remaining pointer after the first one was lifted`() {
        setList()
        composeRule.mainClock.advanceTimeBy(PAUSE_BETWEEN_GESTURES_MILLIS)

        list().performTouchInput {
            down(FIRST_POINTER, center - Offset(POINTER_DISTANCE, 0f))
            down(SECOND_POINTER, center + Offset(POINTER_DISTANCE, 0f))
            repeat(MOVE_STEPS) {
                updatePointerBy(FIRST_POINTER, Offset(-MOVE_DISTANCE, 0f))
                updatePointerBy(SECOND_POINTER, Offset(MOVE_DISTANCE, 0f))
                move()
            }
            up(FIRST_POINTER)
            repeat(MOVE_STEPS) { moveBy(SECOND_POINTER, Offset(MOVE_DISTANCE, 0f)) }
            up(SECOND_POINTER)
        }

        assertThat(currentScale).isGreaterThan(MIN_SCALE)
    }

    private fun setList() {
        composeRule.setContent {
            ZoomableLazyColumn(
                modifier = Modifier.testTag(LIST_TAG),
                minScale = MIN_SCALE,
                maxScale = MAX_SCALE,
                onScaleChanged = { reportedScales.add(it) },
            ) {
                items(ITEM_COUNT) { index ->
                    Text(
                        text = "Item $index",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ITEM_HEIGHT_DP.dp)
                    )
                }
            }
        }
    }

    private fun list() = composeRule.onNodeWithTag(LIST_TAG)

    private fun pinchOut() {
        composeRule.mainClock.advanceTimeBy(PAUSE_BETWEEN_GESTURES_MILLIS)
        list().performTouchInput {
            pinch(
                start0 = center - Offset(POINTER_DISTANCE, 0f),
                end0 = centerLeft,
                start1 = center + Offset(POINTER_DISTANCE, 0f),
                end1 = centerRight,
            )
        }
    }

    private fun pinchIn() {
        composeRule.mainClock.advanceTimeBy(PAUSE_BETWEEN_GESTURES_MILLIS)
        list().performTouchInput {
            pinch(
                start0 = centerLeft,
                end0 = center - Offset(POINTER_DISTANCE, 0f),
                start1 = centerRight,
                end1 = center + Offset(POINTER_DISTANCE, 0f),
            )
        }
    }

    private fun doubleTap() {
        // Keep the taps of two double taps apart, otherwise they would count as one sequence
        composeRule.mainClock.advanceTimeBy(PAUSE_BETWEEN_GESTURES_MILLIS)
        list().performTouchInput { doubleClick() }
    }

    private companion object {
        const val LIST_TAG = "zoomableList"
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 3f
        const val DOUBLE_TAP_SCALE_DELTA = 1.5f
        const val ITEM_COUNT = 30
        const val ITEM_HEIGHT_DP = 48
        const val PINCH_REPETITIONS = 5
        const val PAUSE_BETWEEN_GESTURES_MILLIS = 1_000L
        const val POINTER_DISTANCE = 20f
        const val MOVE_DISTANCE = 15f
        const val MOVE_STEPS = 5
        const val FIRST_POINTER = 0
        const val SECOND_POINTER = 1
    }
}
