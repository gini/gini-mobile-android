@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.analysis.fraud

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * The IBAN selection sheet of the PP-3941 spike: a Material 3 [ModalBottomSheet] listing the
 * two IBANs of [state]. Opens its own window on top of whatever hosts it.
 *
 * Swipe-down, the back button and the back gesture all arrive through [onDismiss]; tapping a row
 * animates the sheet away and then reports the choice through [onSelect]. The sheet's own
 * "Continue" ([onContinue]) exists so the spike can exercise "Continue with the sheet open"
 * (spec R6): the sheet's scrim covers the modal's CTAs while it is up.
 */
@Composable
internal fun IbanSelectionSheet(
    state: IbanMismatchUiState,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GiniTheme.colorScheme.dialogs.container,
    ) {
        IbanSelectionSheetContent(
            state = state,
            onSelect = { iban ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onSelect(iban)
                }
            },
            onContinue = onContinue,
        )
    }
}

@Composable
internal fun IbanSelectionSheetContent(
    state: IbanMismatchUiState,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
            .selectableGroup()
    ) {
        Text(
            text = stringResource(R.string.gc_iban_mismatch_sheet_title),
            style = GiniTheme.typography.headline6,
            color = GiniTheme.colorScheme.text.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        IbanRow(
            label = stringResource(R.string.gc_iban_mismatch_qr_iban_label),
            iban = state.qrIban,
            selected = state.selectedIban == state.qrIban,
            onClick = { onSelect(state.qrIban) },
        )
        IbanRow(
            label = stringResource(R.string.gc_iban_mismatch_printed_iban_label),
            iban = state.printedIban,
            selected = state.selectedIban == state.printedIban,
            onClick = { onSelect(state.printedIban) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        GiniButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.gc_iban_mismatch_continue),
            onClick = onContinue,
            composableProviderConfig = null,
        )
    }
}

@Composable
private fun IbanRow(
    label: String,
    iban: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = GiniTheme.colorScheme.text.accent,
                unselectedColor = GiniTheme.colorScheme.text.secondary,
            ),
        )
        Spacer(modifier = Modifier.width(12.dp))
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
}

@Preview
@Composable
private fun IbanSelectionSheetContentPreviewLight() {
    IbanSelectionSheetContentPreview()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IbanSelectionSheetContentPreviewDark() {
    IbanSelectionSheetContentPreview()
}

@Composable
private fun IbanSelectionSheetContentPreview() {
    GiniTheme {
        IbanSelectionSheetContent(
            state = IbanMismatchUiState(
                qrIban = IbanMismatchSpike.SAMPLE_QR_IBAN,
                printedIban = IbanMismatchSpike.SAMPLE_PRINTED_IBAN,
                selectedIban = IbanMismatchSpike.SAMPLE_QR_IBAN,
                isSheetVisible = true,
            ),
            onSelect = {},
            onContinue = {},
        )
    }
}
