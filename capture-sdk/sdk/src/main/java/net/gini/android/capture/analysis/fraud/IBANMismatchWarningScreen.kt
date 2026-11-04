package net.gini.android.capture.analysis.fraud

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.internal.iban.formatIbanForDisplay
import net.gini.android.capture.internal.iban.formatIbanForSpeech
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.components.card.GiniInfoCard
import net.gini.android.capture.ui.components.expandable.GiniExpandableInfoRow
import net.gini.android.capture.ui.components.footer.GiniActionFooter
import net.gini.android.capture.ui.components.header.GiniHeaderBlock
import net.gini.android.capture.ui.components.header.GiniHeaderBlockColors
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValue
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValueItem
import net.gini.android.capture.ui.components.sheet.GiniBottomSheetScaffold
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * The IBAN mismatch warning: a non-dismissable sheet over the Analysis screen that explains
 * which fraud signals were found and shows the IBANs involved. The user leaves it only through
 * "Cancel transfer" ([onCancel]) or "Proceed anyway" ([onProceed]); the host removes the
 * composable afterwards.
 *
 * The only state the screen owns is whether the "How do I protect myself?" row is expanded,
 * kept in `rememberSaveable` so it survives rotation.
 */
@Composable
internal fun IBANMismatchWarningScreen(
    args: IBANMismatchArgs,
    onCancel: () -> Unit,
    onProceed: () -> Unit,
    composableProviderConfig: GiniComposableStyleProviderConfig?,
) {
    var protectionExpanded by rememberSaveable { mutableStateOf(false) }
    val content = ibanMismatchContent(args)

    GiniBottomSheetScaffold(
        onDismissRequest = {},
        dismissable = false,
        paneTitle = content.title,
        footer = {
            GiniActionFooter(
                primaryText = stringResource(R.string.gc_cancel_transfer),
                onPrimaryClick = onCancel,
                secondaryText = stringResource(R.string.gc_proceed_anyway),
                onSecondaryClick = onProceed,
                composableProviderConfig = composableProviderConfig,
            )
        },
    ) {
        IBANMismatchWarningContent(
            content = content,
            protectionExpanded = protectionExpanded,
            onProtectionExpandedChange = { protectionExpanded = it },
        )
    }
}

/**
 * The texts and rows of one [IBANMismatchArgs], resolved against the current resources.
 * `@Immutable` so the children skip recomposition when only the expanded state changes.
 *
 * @property senderCard the sender shown on its own card above the list; `null` hides the card.
 * In the country-only case there is a single IBAN, so the design lists sender and IBAN together
 * and the sender is the first entry of [rows] instead.
 * @property listTitle the heading above [rows]; `null` in the country-only case
 * @property rows the labelled rows of the list, in order
 */
@Immutable
private data class IBANMismatchContent(
    val title: String,
    val description: String,
    val senderCard: GiniLabeledValueItem?,
    val listTitle: String?,
    val rows: List<GiniLabeledValueItem>,
)

@Composable
private fun ibanMismatchContent(args: IBANMismatchArgs): IBANMismatchContent {
    val title = stringResource(
        when (args.type) {
            IBANMismatchType.QR_VS_PRINTED -> R.string.gc_fraud_iban_mismatch_title
            IBANMismatchType.COUNTRY -> R.string.gc_fraud_country_mismatch_title
            IBANMismatchType.COMBINED -> R.string.gc_fraud_combined_mismatch_title
        }
    )
    val description = stringResource(
        when (args.type) {
            IBANMismatchType.QR_VS_PRINTED -> R.string.gc_fraud_iban_mismatch_description
            IBANMismatchType.COUNTRY -> R.string.gc_fraud_country_mismatch_description
            IBANMismatchType.COMBINED -> R.string.gc_fraud_combined_mismatch_description
        }
    )
    val sender = args.sender?.let {
        GiniLabeledValueItem(label = stringResource(R.string.gc_fraud_sender_label), value = it)
    }
    val qrIban = args.qrIban?.let {
        GiniLabeledValueItem(
            label = stringResource(R.string.gc_fraud_qr_iban_label),
            value = formatIbanForDisplay(it),
            hint = args.qrIbanHint,
            valueContentDescription = formatIbanForSpeech(it),
        )
    }
    // The layout follows the type alone, so an inconsistent args instance cannot mix the
    // title of one case with the layout of another.
    val countryOnly = args.type == IBANMismatchType.COUNTRY
    val printedIban = GiniLabeledValueItem(
        label = stringResource(
            if (countryOnly) {
                R.string.gc_fraud_document_iban_label
            } else {
                R.string.gc_fraud_printed_iban_label
            }
        ),
        value = formatIbanForDisplay(args.printedIban),
        hint = args.printedIbanHint,
        valueContentDescription = formatIbanForSpeech(args.printedIban),
    )
    return IBANMismatchContent(
        title = title,
        description = description,
        senderCard = sender.takeIf { args.type == IBANMismatchType.COMBINED },
        listTitle = if (countryOnly) {
            null
        } else {
            stringResource(R.string.gc_fraud_detected_ibans_title)
        },
        rows = listOfNotNull(sender.takeIf { countryOnly }, qrIban, printedIban),
    )
}

@Composable
private fun IBANMismatchWarningContent(
    content: IBANMismatchContent,
    protectionExpanded: Boolean,
    onProtectionExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GiniHeaderBlock(
            modifier = Modifier.padding(top = HEADER_TOP_PADDING),
            title = content.title,
            description = content.description,
            icon = painterResource(R.drawable.gc_fraud_alert_icon),
            colors = GiniHeaderBlockColors.colors(
                iconContainerColor = GiniTheme.colorScheme.errorHint.container,
                iconTint = GiniTheme.colorScheme.errorHint.content,
            ),
        )
        Spacer(modifier = Modifier.height(SECTION_SPACING))
        GiniExpandableInfoRow(
            title = stringResource(R.string.gc_fraud_protection_title),
            body = stringResource(R.string.gc_fraud_protection_body),
            expanded = protectionExpanded,
            onExpandedChange = onProtectionExpandedChange,
            icon = painterResource(R.drawable.gc_shield_icon),
        )
        Spacer(modifier = Modifier.height(LARGE_SECTION_SPACING))
        if (content.senderCard != null) {
            GiniInfoCard {
                GiniLabeledValue(label = content.senderCard.label, value = content.senderCard.value)
            }
            Spacer(modifier = Modifier.height(CARD_LIST_SPACING))
        }
        IBANMismatchRows(
            title = content.listTitle,
            rows = content.rows,
        )
    }
}

/**
 * The detected IBANs as [GiniLabeledValue] rows separated by hairlines, under an optional title.
 */
@Composable
private fun IBANMismatchRows(
    title: String?,
    rows: List<GiniLabeledValueItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TITLE_ROWS_SPACING),
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.semantics { heading() },
                text = title,
                style = GiniTheme.typography.body1,
                color = GiniTheme.colorScheme.text.primary,
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = GiniTheme.colorScheme.dialogs.borderColor)
                }
                GiniLabeledValue(
                    modifier = Modifier.padding(vertical = ROW_VERTICAL_PADDING),
                    label = row.label,
                    value = row.value,
                    hint = row.hint,
                    valueContentDescription = row.valueContentDescription,
                )
            }
        }
    }
}

private val HEADER_TOP_PADDING = 16.dp
private val SECTION_SPACING = 24.dp
private val LARGE_SECTION_SPACING = 32.dp
private val CARD_LIST_SPACING = 20.dp
private val TITLE_ROWS_SPACING = 4.dp
private val ROW_VERTICAL_PADDING = 12.dp

// region Previews

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IBANMismatchQrVsPrintedPreview() {
    IBANMismatchContentPreview(
        IBANMismatchArgs(
            type = IBANMismatchType.QR_VS_PRINTED,
            qrIban = "PL12100001110100100012345678",
            printedIban = "DE89370400440532013000",
        )
    )
}

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IBANMismatchCountryPreview() {
    IBANMismatchContentPreview(
        IBANMismatchArgs(
            type = IBANMismatchType.COUNTRY,
            qrIban = null,
            printedIban = "PL12100001110100100012345678",
            sender = "Elektro Brandt GmbH · Germany",
            printedIbanHint = "From Poland, not the sender's country",
        )
    )
}

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IBANMismatchCombinedPreview() {
    IBANMismatchContentPreview(IBANMismatchDebug.sampleArgs())
}

@Preview(
    name = "Combined, both hints, expanded",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Composable
private fun IBANMismatchCombinedBothHintsPreview() {
    IBANMismatchContentPreview(
        args = IBANMismatchDebug.sampleArgs().copy(
            printedIban = "AT611904300234573201",
            printedIbanHint = "From Austria, not the sender's country",
        ),
        protectionExpanded = true,
    )
}

@Composable
private fun IBANMismatchContentPreview(
    args: IBANMismatchArgs,
    protectionExpanded: Boolean = false,
) {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                IBANMismatchWarningContent(
                    content = ibanMismatchContent(args),
                    protectionExpanded = protectionExpanded,
                    onProtectionExpandedChange = {},
                )
                GiniActionFooter(
                    primaryText = stringResource(R.string.gc_cancel_transfer),
                    onPrimaryClick = {},
                    secondaryText = stringResource(R.string.gc_proceed_anyway),
                    onSecondaryClick = {},
                    composableProviderConfig = null,
                    horizontal = false,
                )
            }
        }
    }
}

// endregion
