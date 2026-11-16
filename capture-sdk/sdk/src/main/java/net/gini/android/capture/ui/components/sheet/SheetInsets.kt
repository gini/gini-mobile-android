package net.gini.android.capture.ui.components.sheet

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Keeps a Material bottom sheet surface out of tappable system UI on the bottom and both sides.
 *
 * The sheet's window is edge-to-edge and Material pads only the content, and only at the top
 * and bottom. With three-button navigation the bar is tappable system UI, at the bottom in
 * portrait and, on most phones, at the side in landscape; with this modifier the sheet stops
 * before the bar and the scrim shows behind it. The gesture bar is not tappable and reports no
 * inset, so the sheet still runs under it as Material intends.
 *
 * Pass it in the sheet's `modifier`. It is [composed] on purpose: the insets are then read
 * where Material applies the modifier, inside the sheet's own window. Read in the caller's
 * composition they come from the view hosting the caller, and below the capture root, which
 * consumes the insets (`protectViewFromInsets`), they are zero.
 */
internal fun Modifier.sheetTappableElementPadding(): Modifier = composed {
    windowInsetsPadding(
        WindowInsets.tappableElement.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    )
}
