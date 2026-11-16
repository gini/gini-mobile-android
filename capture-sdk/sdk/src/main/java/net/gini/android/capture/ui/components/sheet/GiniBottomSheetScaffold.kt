@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.ui.components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.modifier.tabletMaxWidth

/**
 * A full-height, non-dismissable modal bottom sheet in the Gini style for screens the user must
 * answer, such as the fraud warnings: it fills the window below the status bar, is full width
 * on phones and tablets, and keeps its [content] column at the tablet reading width. The
 * [footer] sits at the bottom of the sheet while the content fits and scrolls away with the
 * content once it does not.
 *
 * The sheet's window is edge-to-edge, and Material pads only the content, and only at the top
 * and bottom. With three-button navigation the bar is tappable system UI, at the bottom in
 * portrait and, on most phones, at the side in landscape, so the sheet itself keeps out of it
 * on the bottom and both sides and the scrim shows behind the bar. The gesture bar is not
 * tappable and reports no inset, so the sheet still runs under it as Material intends.
 *
 * The sheet refuses every dismissal path: the back button, the back gesture, taps on the scrim
 * and swipes. It shows no drag handle. The host removes the composable when the user has chosen
 * one of the sheet's own actions.
 *
 * Another [ModalBottomSheet] composed inside [content] opens its own window on top of this one.
 *
 * @param modifier applied to the sheet surface
 * @param paneTitle announced by accessibility services when the sheet opens; falls back to the
 * generic Material 3 "Bottom Sheet" when `null`
 * @param colors colors of the sheet, see [GiniBottomSheetScaffoldColors.colors]
 * @param footer composed below the scrollable content, full width of the content column
 * @param content the scrollable body of the sheet
 */
@Composable
fun GiniBottomSheetScaffold(
    modifier: Modifier = Modifier,
    paneTitle: String? = null,
    colors: GiniBottomSheetScaffoldColors = GiniBottomSheetScaffoldColors.colors(),
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> target != SheetValue.Hidden },
    )
    ModalBottomSheet(
        // Never called: every dismissal path is disabled below.
        onDismissRequest = {},
        modifier = modifier
            .statusBarsPadding()
            .windowInsetsPadding(
                WindowInsets.tappableElement.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
            .then(
                if (paneTitle != null) {
                    Modifier.semantics { this.paneTitle = paneTitle }
                } else {
                    Modifier
                }
            ),
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified,
        sheetGesturesEnabled = false,
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false,
        ),
        containerColor = colors.containerColor,
    ) {
        GiniBottomSheetScaffoldBody(
            footer = footer,
            content = content,
        )
    }
}

/**
 * The layout inside the sheet: a scrolling column that is at least as tall as the sheet, so the
 * footer is pushed to the bottom while the content is short and scrolls with it otherwise.
 */
@Composable
private fun GiniBottomSheetScaffoldBody(
    footer: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
        val minHeight = if (constraints.hasBoundedHeight) maxHeight else 0.dp
        // verticalScroll must come before heightIn: the scroll gives its content unbounded
        // height, and heightIn then raises the content's minimum to the viewport, which is what
        // lets the weighted spacer below push the footer to the bottom while the content fits.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .tabletMaxWidth()
                    .padding(
                        start = SHEET_HORIZONTAL_PADDING,
                        end = SHEET_HORIZONTAL_PADDING,
                        top = SHEET_TOP_PADDING,
                    ),
                content = content,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (footer != null) {
                Column(
                    modifier = Modifier
                        .tabletMaxWidth()
                        .padding(
                            start = SHEET_HORIZONTAL_PADDING,
                            end = SHEET_HORIZONTAL_PADDING,
                            top = FOOTER_TOP_PADDING,
                            bottom = FOOTER_BOTTOM_PADDING,
                        ),
                    content = footer,
                )
            }
        }
    }
}

private val SHEET_HORIZONTAL_PADDING = 24.dp
private val SHEET_TOP_PADDING = 32.dp
private val FOOTER_TOP_PADDING = 32.dp
private val FOOTER_BOTTOM_PADDING = 16.dp

@GiniScreenPreviewUiModes
@Composable
private fun GiniBottomSheetScaffoldBodyPreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            Column(modifier = Modifier.width(360.dp)) {
                GiniBottomSheetScaffoldBody(
                    footer = {
                        Text(text = "Footer", color = GiniTheme.colorScheme.text.primary)
                    },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Content line 1", color = GiniTheme.colorScheme.text.primary)
                        Text(text = "Content line 2", color = GiniTheme.colorScheme.text.primary)
                    }
                }
            }
        }
    }
}
