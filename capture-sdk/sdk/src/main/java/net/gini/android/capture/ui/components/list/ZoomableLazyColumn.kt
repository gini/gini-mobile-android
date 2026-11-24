package net.gini.android.capture.ui.components.list

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ZoomableLazyColumn(
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 3f,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    onScaleChanged: (Float) -> Unit = {},
    content: LazyListScope.() -> Unit,
) {

    var scale by remember { mutableFloatStateOf(minScale) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale) { onScaleChanged(scale) }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, _, _, _ ->
                        scale = maxOf(minScale, minOf(scale * zoom, maxScale))
                        val maxX = (size.width * (scale - 1)) / 2
                        val minX = -maxX
                        offsetX = maxOf(minX, minOf(maxX, offsetX + pan.x))
                        val maxY = (size.height * (scale - 1)) / 2
                        val minY = -maxY
                        offsetY = maxOf(minY, minOf(maxY, offsetY + pan.y))
                    },
                    onGestureEnd = {

                    },
                    onGestureStart = {

                    },
                    onDoubleTap = {
                        val scaleDelta = if (scale < 2f) 1.5f else -1.5f
                        scale += scaleDelta
                    }
                )
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            ),
        state = state,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        reverseLayout = reverseLayout,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled
    ) {
        content()
    }
}

private const val DOUBLE_TAP_TIMEOUT_MILLIS = 300L
private const val DEGREES_PER_HALF_TURN = 180f

private typealias TransformGestureListener = (
    centroid: Offset,
    pan: Offset,
    zoom: Float,
    rotation: Float,
    mainPointer: PointerInputChange,
    changes: List<PointerInputChange>
) -> Unit

private suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    consume: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main,
    onGestureStart: (PointerInputChange) -> Unit = {},
    onGesture: TransformGestureListener,
    onGestureEnd: (PointerInputChange) -> Unit = {},
    onDoubleTap: () -> Unit = {},
) {
    var lastTouchTime = 0L

    awaitEachGesture {
        // Wait for at least one pointer to press down, and set first contact position
        val down: PointerInputChange = awaitFirstDown(
            requireUnconsumed = false,
            pass = pass
        )

        onGestureStart(down)

        val gesture = TransformGesture(
            down = down,
            touchSlop = viewConfiguration.touchSlop,
            panZoomLock = panZoomLock,
            consume = consume,
            isDoubleTap = down.uptimeMillis - lastTouchTime < DOUBLE_TAP_TIMEOUT_MILLIS,
            onGesture = onGesture,
            onDoubleTap = onDoubleTap,
        )

        do {
            val event = awaitPointerEvent(pass = pass)

            // If any position change is consumed from another PointerInputChange
            // or pointer count requirement is not fulfilled
            val canceled = event.changes.any { it.isConsumed }

            if (!canceled) {
                gesture.onEvent(event)
            }
        } while (!canceled && event.changes.any { it.pressed })
        lastTouchTime = down.uptimeMillis
        onGestureEnd(gesture.pointer)
    }
}

/**
 * Tracks the state of a single transform gesture (from the first pointer down until all pointers
 * are up) and reports the transformations to the listener.
 */
private class TransformGesture(
    down: PointerInputChange,
    private val touchSlop: Float,
    private val panZoomLock: Boolean,
    private val consume: Boolean,
    private val isDoubleTap: Boolean,
    private val onGesture: TransformGestureListener,
    private val onDoubleTap: () -> Unit,
) {
    /** The main pointer: the one that went down first, or a replacement once it is up. */
    var pointer: PointerInputChange = down
        private set

    private var pointerId = down.id
    private var rotation = 0f
    private var zoom = 1f
    private var pan = Offset.Zero
    private var pastTouchSlop = false
    private var lockedToPanZoom = false

    fun onEvent(event: PointerEvent) {
        // Get pointer that is down, if first pointer is up
        // get another and use it if other pointers are also down
        // event.changes.first() doesn't return same order
        val pointerInputChange = event.changes.firstOrNull { it.id == pointerId }
            ?: event.changes.first()

        // Next time will check same pointer with this id
        pointerId = pointerInputChange.id
        pointer = pointerInputChange

        val zoomChange = event.calculateZoom()
        val rotationChange = event.calculateRotation()
        val panChange = event.calculatePan()

        if (!pastTouchSlop) {
            trackTouchSlop(event, zoomChange, rotationChange, panChange)
        }

        if (isDoubleTap) {
            onGesture(
                event.calculateCentroid(useCurrent = false),
                panChange,
                zoom,
                0f,
                pointer,
                event.changes
            )
            onDoubleTap()
        }

        if (pastTouchSlop) {
            dispatchTransformation(event, zoomChange, rotationChange, panChange)
            if (consume) {
                event.consumePositionChanges()
            }
        }
    }

    private fun trackTouchSlop(
        event: PointerEvent,
        zoomChange: Float,
        rotationChange: Float,
        panChange: Offset
    ) {
        zoom *= zoomChange
        rotation += rotationChange
        pan += panChange

        val centroidSize = event.calculateCentroidSize(useCurrent = false)
        val zoomMotion = abs(1 - zoom) * centroidSize
        val rotationMotion =
            abs(rotation * kotlin.math.PI.toFloat() * centroidSize / DEGREES_PER_HALF_TURN)
        val panMotion = pan.getDistance()

        if (zoomMotion > touchSlop ||
            rotationMotion > touchSlop ||
            panMotion > touchSlop
        ) {
            pastTouchSlop = true
            lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
        }
    }

    private fun dispatchTransformation(
        event: PointerEvent,
        zoomChange: Float,
        rotationChange: Float,
        panChange: Offset
    ) {
        val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
        if (effectiveRotation != 0f ||
            zoomChange != 1f ||
            panChange != Offset.Zero
        ) {
            onGesture(
                event.calculateCentroid(useCurrent = false),
                panChange,
                zoomChange,
                effectiveRotation,
                pointer,
                event.changes
            )
        }
    }
}

private fun PointerEvent.consumePositionChanges() {
    changes.forEach {
        if (it.positionChanged()) {
            it.consume()
        }
    }
}
