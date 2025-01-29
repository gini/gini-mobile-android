package net.gini.android.capture.util.compose

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Provides a padding based on keyboard state. If keyboard is opened the passed padding will be
 * returned and 0 will be returned otherwise
 *
 * In case if scrollState is passed - it will be scrolled automatically to this padding
 */
@Composable
fun keyboardPadding(
    padding: Dp,
    scrollState: ScrollState? = null
): State<Dp> {
    val keybaordState by rememberImeState()

    val keyboardPadding = remember { mutableStateOf(padding) }
    val paddingPx = with(LocalDensity.current) { padding.toPx().roundToInt() }

    LaunchedEffect(keybaordState, paddingPx) {
        if (keybaordState) {
            keyboardPadding.value = padding
            scrollState?.let { scrollState ->
                scrollState.animateScrollTo(scrollState.value + paddingPx, tween(300))
            }
        } else {
            keyboardPadding.value = 0.dp
        }
    }

    return keyboardPadding
}
