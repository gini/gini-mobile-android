package net.gini.android.capture.ui.components.labeledvalue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * A small label above a value, with an optional hint line below the value and an optional
 * leading slot (e.g. a radio button). Long values such as IBANs wrap onto further lines.
 *
 * Label, value and hint are merged into one accessibility node, so TalkBack reads
 * "In the QR code, PL12 1000 …" as a single stop. When the value should be spoken differently
 * from how it is written (an IBAN character by character, say), pass [valueContentDescription];
 * it replaces only the value in the announcement, label and hint are still read.
 *
 * @param label the caption above the value
 * @param value the value itself
 * @param modifier applied to the whole row
 * @param hint the optional line below the value, drawn in [GiniLabeledValueColors.hintColor]
 * @param valueContentDescription what accessibility services read in place of [value]
 * @param colors colors, see [GiniLabeledValueColors.colors]
 * @param leadingContent composed before the texts, vertically centred
 */
@Composable
fun GiniLabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    valueContentDescription: String? = null,
    colors: GiniLabeledValueColors = GiniLabeledValueColors.colors(),
    leadingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                // Set on the merged node, not on the value Text: a contentDescription inside a
                // merged node would replace the whole announcement and drop label and hint.
                if (valueContentDescription != null) {
                    contentDescription = listOfNotNull(label, valueContentDescription, hint)
                        .joinToString(separator = ", ")
                }
            },
        horizontalArrangement = Arrangement.spacedBy(LEADING_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TEXT_SPACING),
        ) {
            Text(
                text = label,
                style = GiniTheme.typography.caption1,
                color = colors.labelColor,
            )
            Text(
                text = value,
                style = GiniTheme.typography.subtitle1,
                color = colors.valueColor,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = GiniTheme.typography.body2,
                    color = colors.hintColor,
                )
            }
        }
    }
}

private val LEADING_SPACING = 12.dp
private val TEXT_SPACING = 4.dp

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun GiniLabeledValuePreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GiniLabeledValue(
                    label = "In the QR code",
                    value = "PL12 1000 0111 0100 1000",
                )
                GiniLabeledValue(
                    label = "Printed on the document",
                    value = "DE89 3704 0044 0532 0130 00",
                    hint = "From Germany, not the sender's country",
                    valueContentDescription = "D E 8 9, 3 7 0 4, 0 0 4 4, 0 5 3 2, 0 1 3 0, 0 0",
                )
            }
        }
    }
}
