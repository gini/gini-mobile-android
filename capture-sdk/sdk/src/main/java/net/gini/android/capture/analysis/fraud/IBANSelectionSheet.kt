@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.analysis.fraud

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.components.footer.GiniActionFooter
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValue
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValueItem
import net.gini.android.capture.ui.components.radio.GiniRadioButton
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.ui.theme.modifier.tabletMaxWidth

/**
 * The IBAN selection sheet: a regular, dismissable modal sheet that wraps its content and asks
 * the user which of the IBANs of the mismatch warning (the QR IBAN and every printed IBAN) to
 * continue with. Back, scrim tap and swipe close it through [onDismiss]; "Next" with a choice
 * reports it through
 * [onContinue], "Next" without one shows the error state instead. The host removes the
 * composable after either callback.
 *
 * The sheet owns its choice and its error state in `rememberSaveable`, so both survive
 * rotation. Choosing an IBAN clears the error. Every "Next" without a choice announces the
 * error to accessibility services, since focus stays on the button and the rows only expose
 * the invalid state once they are focused again; the announcement is not repeated after
 * rotation.
 */
@Composable
internal fun IBANSelectionSheet(
    args: IBANSelectionArgs,
    onDismiss: () -> Unit,
    onContinue: (selectedIban: String) -> Unit,
    composableProviderConfig: GiniComposableStyleProviderConfig?,
) {
    var selectedIban by rememberSaveable { mutableStateOf<String?>(null) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var errorAnnouncements by remember { mutableIntStateOf(0) }
    val title = stringResource(R.string.gc_fraud_select_iban_title)
    val errorMessage = stringResource(R.string.gc_fraud_select_iban_error)
    val options = ibanSelectionOptions(args)

    // A live region on the error line would not be announced when the line first enters the
    // composition, so the error is announced explicitly, as the education messages do.
    val view = LocalView.current
    LaunchedEffect(errorAnnouncements) {
        if (errorAnnouncements > 0) {
            view.announceForAccessibility(errorMessage)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics { paneTitle = title },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Full width on tablets too, like the warning sheet it opens over.
        sheetMaxWidth = Dp.Unspecified,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GiniTheme.colorScheme.icons.secondary) },
        containerColor = GiniTheme.colorScheme.dialogs.container,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .tabletMaxWidth()
                    .padding(horizontal = SHEET_HORIZONTAL_PADDING)
                    .padding(bottom = SHEET_BOTTOM_PADDING),
            ) {
                IBANSelectionContent(
                    title = title,
                    options = options,
                    selectedIban = selectedIban,
                    errorMessage = errorMessage.takeIf { showError },
                    onSelect = { iban ->
                        selectedIban = iban
                        showError = false
                    },
                )
                Spacer(modifier = Modifier.height(CONTENT_FOOTER_SPACING))
                IBANSelectionFooter(
                    onContinue = {
                        val iban = selectedIban
                        if (iban == null) {
                            showError = true
                            errorAnnouncements++
                        } else {
                            onContinue(iban)
                        }
                    },
                    composableProviderConfig = composableProviderConfig,
                )
            }
        }
    }
}

/**
 * One selectable row of the sheet.
 *
 * @property iban the raw IBAN reported through `onContinue` when this option is chosen
 * @property item the texts of the row
 */
private data class IBANSelectionOption(
    val iban: String,
    val item: GiniLabeledValueItem,
)

@Composable
private fun ibanSelectionOptions(args: IBANSelectionArgs): List<IBANSelectionOption> {
    val countryLocale = countryNameLocale()
    val qrLabel = stringResource(R.string.gc_fraud_qr_iban_label)
    val printedLabel = stringResource(R.string.gc_fraud_printed_iban_label)
    return listOf(IBANSelectionOption(args.qrIban.iban, ibanItem(qrLabel, args.qrIban, countryLocale))) +
        args.printedIbans.map {
            IBANSelectionOption(it.iban, ibanItem(printedLabel, it, countryLocale))
        }
}

/**
 * Title, radio rows and the error line.
 *
 * @param errorMessage the error shown under the rows and attached to each row's semantics, so
 * assistive technology reads the invalid state on the rows and not only by colour; `null`
 * when there is no error
 */
@Composable
private fun IBANSelectionContent(
    title: String,
    options: List<IBANSelectionOption>,
    selectedIban: String?,
    errorMessage: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            text = title,
            style = GiniTheme.typography.headline6,
            color = GiniTheme.colorScheme.text.primary,
        )
        Spacer(modifier = Modifier.height(TITLE_ROWS_SPACING))
        Column(modifier = Modifier.selectableGroup()) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(color = GiniTheme.colorScheme.dialogs.borderColor)
                }
                val selected = option.iban == selectedIban
                GiniLabeledValue(
                    modifier = Modifier
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(option.iban) },
                        )
                        .semantics {
                            if (errorMessage != null) {
                                error(errorMessage)
                            }
                        }
                        .padding(vertical = ROW_VERTICAL_PADDING),
                    label = option.item.label,
                    value = option.item.value,
                    hint = option.item.hint,
                    valueContentDescription = option.item.valueContentDescription,
                    leadingContent = {
                        GiniRadioButton(
                            selected = selected,
                            onClick = null,
                            isError = errorMessage != null,
                        )
                    },
                )
            }
        }
        if (errorMessage != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ERROR_TOP_SPACING, start = ERROR_START_INSET),
                text = errorMessage,
                style = GiniTheme.typography.caption1,
                color = GiniTheme.colorScheme.errorHint.content,
            )
        }
    }
}

@Composable
private fun IBANSelectionFooter(
    onContinue: () -> Unit,
    composableProviderConfig: GiniComposableStyleProviderConfig?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FOOTNOTE_BUTTON_SPACING),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.gc_fraud_select_iban_footnote),
            style = GiniTheme.typography.body2,
            color = GiniTheme.colorScheme.text.secondary,
            textAlign = TextAlign.Center,
        )
        GiniActionFooter(
            primaryText = stringResource(R.string.gc_next),
            onPrimaryClick = onContinue,
            composableProviderConfig = composableProviderConfig,
        )
    }
}

private val SHEET_HORIZONTAL_PADDING = 24.dp
private val SHEET_BOTTOM_PADDING = 24.dp
private val TITLE_ROWS_SPACING = 20.dp
private val ROW_VERTICAL_PADDING = 12.dp
private val ERROR_TOP_SPACING = 4.dp
private val ERROR_START_INSET = 8.dp
private val CONTENT_FOOTER_SPACING = 24.dp
private val FOOTNOTE_BUTTON_SPACING = 8.dp

// region Previews

private const val PREVIEW_QR_IBAN = "PL12100001110100100012345678"
private const val PREVIEW_PRINTED_IBAN = "DE89370400440532013000"

private val previewArgs = IBANSelectionArgs(
    qrIban = IBANMismatchIban(PREVIEW_QR_IBAN),
    printedIbans = listOf(IBANMismatchIban(PREVIEW_PRINTED_IBAN)),
)

private val combinedPreviewArgs = IBANSelectionArgs(
    qrIban = IBANMismatchIban(PREVIEW_QR_IBAN, hintCountryCode = "PL"),
    printedIbans = listOf(
        IBANMismatchIban(PREVIEW_PRINTED_IBAN),
        IBANMismatchIban("AT611904300234573201", hintCountryCode = "AT"),
    ),
)

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IBANSelectionInitialPreview() {
    IBANSelectionSheetPreview(
        args = previewArgs,
    )
}

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IBANSelectionErrorPreview() {
    IBANSelectionSheetPreview(
        args = previewArgs,
        showError = true,
    )
}

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IBANSelectionSelectedPreview() {
    IBANSelectionSheetPreview(
        args = previewArgs,
        selectedIban = PREVIEW_QR_IBAN,
    )
}

@Preview(name = "Combined, selected", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Combined, selected, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun IBANSelectionCombinedSelectedPreview() {
    IBANSelectionSheetPreview(
        args = combinedPreviewArgs,
        selectedIban = PREVIEW_QR_IBAN,
    )
}

@Preview(name = "Combined, error", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun IBANSelectionCombinedErrorPreview() {
    IBANSelectionSheetPreview(
        args = combinedPreviewArgs,
        showError = true,
    )
}

@Composable
private fun IBANSelectionSheetPreview(
    args: IBANSelectionArgs,
    selectedIban: String? = null,
    showError: Boolean = false,
) {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            Column(
                modifier = Modifier
                    .padding(horizontal = SHEET_HORIZONTAL_PADDING)
                    .padding(bottom = SHEET_BOTTOM_PADDING),
            ) {
                IBANSelectionContent(
                    title = stringResource(R.string.gc_fraud_select_iban_title),
                    options = ibanSelectionOptions(args),
                    selectedIban = selectedIban,
                    errorMessage = stringResource(R.string.gc_fraud_select_iban_error)
                        .takeIf { showError },
                    onSelect = {},
                )
                Spacer(modifier = Modifier.height(CONTENT_FOOTER_SPACING))
                IBANSelectionFooter(onContinue = {}, composableProviderConfig = null)
            }
        }
    }
}

// endregion
