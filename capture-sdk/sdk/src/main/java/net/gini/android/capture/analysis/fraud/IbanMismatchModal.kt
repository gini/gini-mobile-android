@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.analysis.fraud

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.button.outlined.GiniOutlinedButton
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * The IBAN mismatch modal of the PP-3941 spike: a non-dismissable modal over the Analysis
 * screen, hosting the [IbanSelectionSheet] in its own content so the sheet's window stacks on
 * top of the modal's window.
 *
 * It is the same full-height bottom sheet on phones and tablets (unlike
 * [net.gini.android.capture.analysis.warning.WarningBottomSheet], which switches to a card
 * dialog on tablets).
 *
 * Both layers are driven by one [rememberSaveable] [IbanMismatchUiState], which is what lets a
 * rotation or a process death restore the modal, the open sheet and the chosen IBAN together.
 *
 * - "Continue" reports the selected IBAN through [onContinue]; the host then removes this
 *   composable, which tears down both windows in the same frame.
 * - "Cancel transfer" calls [onCancel].
 * - Back, the back gesture, swipe-down and the scrim never dismiss the modal: the sheet
 *   refuses the [SheetValue.Hidden] target and ignores back presses and scrim taps.
 *
 * The sheet announces the modal's title as its accessibility pane title, as
 * `WarningBottomSheet` does with `setAccessibilityPaneTitle`.
 */
@Composable
internal fun IbanMismatchModal(
    args: IbanMismatchArgs,
    onContinue: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var state by rememberSaveable(args) { mutableStateOf(IbanMismatchUiState.initial(args)) }

    val content: @Composable () -> Unit = {
        IbanMismatchModalContent(
            state = state,
            onChooseIban = { state = state.copy(isSheetVisible = true) },
            onContinue = { onContinue(state.selectedIban) },
            onCancel = onCancel,
        )
        if (state.isSheetVisible) {
            IbanSelectionSheet(
                state = state,
                onSelect = { iban ->
                    state = state.copy(selectedIban = iban, isSheetVisible = false)
                },
                onContinue = { onContinue(state.selectedIban) },
                onDismiss = { state = state.copy(isSheetVisible = false) },
            )
        }
    }

    IbanMismatchSheet(content)
}

/**
 * A Material 3 [ModalBottomSheet] that cannot be dismissed. The sheet state
 * refuses every transition to [SheetValue.Hidden] (the safety net for any dismissal path), and
 * [ModalBottomSheetProperties] disables both the back button / back gesture and scrim taps, so
 * neither path calls `hide()` and the scrim exposes no dead "Close sheet" action to TalkBack.
 * Sheet gestures are disabled and the drag handle is hidden, so the sheet neither moves with
 * the finger nor advertises a gesture it rejects (the `isDraggable = false` of
 * `WarningBottomSheet`).
 *
 * The sheet fills the window below the status bar: [statusBarsPadding] on the sheet's own
 * modifier insets the sheet surface (Material 3 applies that modifier first, before it
 * measures the drag anchors), and [fillMaxHeight] on the content slot stretches the surface to
 * the remaining height. The status bar area shows the scrim. The sheet is full width on every
 * device class: Material 3's default 640 dp `sheetMaxWidth` is lifted so tablets look like
 * phones.
 */
@Composable
private fun IbanMismatchSheet(content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> target != SheetValue.Hidden },
    )
    val paneTitle = stringResource(R.string.gc_iban_mismatch_title)
    ModalBottomSheet(
        onDismissRequest = { /* unreachable: back press and scrim taps are disabled */ },
        modifier = Modifier
            .statusBarsPadding()
            .semantics { this.paneTitle = paneTitle },
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified,
        sheetGesturesEnabled = false,
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false,
        ),
        containerColor = GiniTheme.colorScheme.dialogs.container,
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            content()
        }
    }
}


@Composable
internal fun IbanMismatchModalContent(
    state: IbanMismatchUiState,
    onChooseIban: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.gc_iban_mismatch_title),
            modifier = Modifier.semantics { heading() },
            style = GiniTheme.typography.headline5,
            color = GiniTheme.colorScheme.text.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.gc_iban_mismatch_description),
            style = GiniTheme.typography.body2,
            color = GiniTheme.colorScheme.text.secondary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        LabelledIban(
            label = stringResource(R.string.gc_iban_mismatch_qr_iban_label),
            iban = state.qrIban,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LabelledIban(
            label = stringResource(R.string.gc_iban_mismatch_printed_iban_label),
            iban = state.printedIban,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = GiniTheme.colorScheme.card.container,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(16.dp),
        ) {
            LabelledIban(
                label = stringResource(R.string.gc_iban_mismatch_selected_iban_label),
                iban = state.selectedIban,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        GiniOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.gc_iban_mismatch_choose_iban),
            onClick = onChooseIban,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GiniButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.gc_iban_mismatch_continue),
            onClick = onContinue,
            composableProviderConfig = null,
        )
        Spacer(modifier = Modifier.height(12.dp))
        GiniOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.gc_cancel_transfer),
            onClick = onCancel,
        )
    }
}

@Composable
private fun LabelledIban(label: String, iban: String) {
    Column {
        Text(
            text = label,
            style = GiniTheme.typography.caption1,
            color = GiniTheme.colorScheme.text.secondary,
        )
        Text(
            text = iban,
            style = GiniTheme.typography.body1,
            color = GiniTheme.colorScheme.text.primary,
        )
    }
}

@Preview
@Composable
private fun IbanMismatchModalContentPreviewLight() {
    IbanMismatchModalContentPreview()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IbanMismatchModalContentPreviewDark() {
    IbanMismatchModalContentPreview()
}

@Composable
private fun IbanMismatchModalContentPreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.background.primary) {
            IbanMismatchModalContent(
                state = IbanMismatchUiState(
                    qrIban = IbanMismatchSpike.SAMPLE_QR_IBAN,
                    printedIban = IbanMismatchSpike.SAMPLE_PRINTED_IBAN,
                    selectedIban = IbanMismatchSpike.SAMPLE_PRINTED_IBAN,
                    isSheetVisible = false,
                ),
                onChooseIban = {},
                onContinue = {},
                onCancel = {},
            )
        }
    }
}
