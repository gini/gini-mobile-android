package net.gini.android.capture.analysis.fraud

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValueList
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
internal fun IbanMismatchWarningScreen(
    args: IbanMismatchArgs,
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
        IbanMismatchWarningContent(
            content = content,
            protectionExpanded = protectionExpanded,
            onProtectionExpandedChange = { protectionExpanded = it },
        )
    }
}

/**
 * The texts and rows of one [IbanMismatchArgs], resolved against the current resources.
 * `@Immutable` so the children skip recomposition when only the expanded state changes.
 *
 * @property senderCard the sender shown on its own card above the list; `null` hides the card.
 * In the country-only case there is a single IBAN, so the design lists sender and IBAN together
 * and the sender is the first entry of [rows] instead.
 * @property rows the labelled rows of the list, in order
 */
@Immutable
private data class IbanMismatchContent(
    val title: String,
    val description: String,
    val senderCard: GiniLabeledValueItem?,
    val listTitle: String?,
    val rows: List<GiniLabeledValueItem>,
)

@Composable
private fun ibanMismatchContent(args: IbanMismatchArgs): IbanMismatchContent {
    val title = stringResource(
        when (args.type) {
            IbanMismatchType.QR_VS_PRINTED -> R.string.gc_fraud_iban_mismatch_title
            IbanMismatchType.COUNTRY -> R.string.gc_fraud_country_mismatch_title
            IbanMismatchType.COMBINED -> R.string.gc_fraud_combined_mismatch_title
        }
    )
    val description = stringResource(
        when (args.type) {
            IbanMismatchType.QR_VS_PRINTED -> R.string.gc_fraud_iban_mismatch_description
            IbanMismatchType.COUNTRY -> R.string.gc_fraud_country_mismatch_description
            IbanMismatchType.COMBINED -> R.string.gc_fraud_combined_mismatch_description
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
    val countryOnly = args.type == IbanMismatchType.COUNTRY
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
    return IbanMismatchContent(
        title = title,
        description = description,
        senderCard = sender.takeIf { args.type == IbanMismatchType.COMBINED },
        listTitle = if (countryOnly) {
            null
        } else {
            stringResource(R.string.gc_fraud_detected_ibans_title)
        },
        rows = listOfNotNull(sender.takeIf { countryOnly }, qrIban, printedIban),
    )
}

@Composable
private fun IbanMismatchWarningContent(
    content: IbanMismatchContent,
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
        GiniLabeledValueList(
            title = content.listTitle,
            items = content.rows,
        )
    }
}

private val HEADER_TOP_PADDING = 16.dp
private val SECTION_SPACING = 24.dp
private val LARGE_SECTION_SPACING = 32.dp
private val CARD_LIST_SPACING = 20.dp

// region Previews

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IbanMismatchQrVsPrintedPreview() {
    IbanMismatchContentPreview(
        IbanMismatchArgs(
            type = IbanMismatchType.QR_VS_PRINTED,
            qrIban = "PL12100001110100100012345678",
            printedIban = "DE89370400440532013000",
        )
    )
}

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun IbanMismatchCountryPreview() {
    IbanMismatchContentPreview(
        IbanMismatchArgs(
            type = IbanMismatchType.COUNTRY,
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
private fun IbanMismatchCombinedPreview() {
    IbanMismatchContentPreview(IbanMismatchDebug.sampleArgs())
}

@Preview(
    name = "Combined, both hints, expanded",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Composable
private fun IbanMismatchCombinedBothHintsPreview() {
    IbanMismatchContentPreview(
        args = IbanMismatchDebug.sampleArgs().copy(
            printedIban = "AT611904300234573201",
            printedIbanHint = "From Austria, not the sender's country",
        ),
        protectionExpanded = true,
    )
}

@Composable
private fun IbanMismatchContentPreview(
    args: IbanMismatchArgs,
    protectionExpanded: Boolean = false,
) {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                IbanMismatchWarningContent(
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
