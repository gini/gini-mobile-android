package net.gini.android.capture.ui.components.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.tooling.preview.Preview
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * Implements infinite loading indicator by using some [Char] as a step of progress
 * @param symbol symbol for drawing progress. `.` by default
 * @param initialCount start count of symbols. 0 by default
 * @param targetCount target count of symbols. 4 by default
 * @param duration duration animation cycle.
 */
@Composable
fun animatedCharsLoadingIndicatorAsState(
    symbol: Char = '.',
    initialCount: Int = 0,
    targetCount: Int = 3,
    label: String = "DotsAnimation",
    duration: Int = 1_600,
): State<String> {
    val transition = rememberInfiniteTransition(label = "Dots Transition")

    val count by transition.animateValue(
        initialValue = initialCount,
        targetValue = targetCount + 1,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = duration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = label
    )

    val placeholderCount = targetCount + 1 - count

    return produceState(initialValue = "", count) {
        value = "$symbol".repeat(count) + " ".repeat((placeholderCount - 1).coerceAtLeast(0))
    }
}

@Preview
@Composable
private fun AnimationPreview() {
    val dots by animatedCharsLoadingIndicatorAsState()

    GiniTheme {
        Text(
            text = "Analyzing$dots",
        )
    }
}
