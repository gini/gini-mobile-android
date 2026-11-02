package net.gini.android.capture.ui.components.labeledvalue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * A vertical list of [GiniLabeledValue] rows separated by hairlines, with an optional title.
 *
 * @param items the rows, in order
 * @param modifier applied to the whole list
 * @param title the text above the list, e.g. "The following IBANs were detected"
 * @param colors colors, see [GiniLabeledValueColors.colors]
 * @param leadingContent composed before the texts of the row at the given index
 */
@Composable
fun GiniLabeledValueList(
    items: List<GiniLabeledValueItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    colors: GiniLabeledValueColors = GiniLabeledValueColors.colors(),
    leadingContent: (@Composable (index: Int) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TITLE_LIST_SPACING),
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.semantics { heading() },
                text = title,
                style = GiniTheme.typography.body1,
                color = colors.titleColor,
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = colors.dividerColor)
                }
                GiniLabeledValue(
                    modifier = Modifier.padding(vertical = ITEM_VERTICAL_PADDING),
                    label = item.label,
                    value = item.value,
                    hint = item.hint,
                    valueContentDescription = item.valueContentDescription,
                    colors = colors,
                    leadingContent = leadingContent?.let { slot -> { slot(index) } },
                )
            }
        }
    }
}

private val TITLE_LIST_SPACING = 4.dp
private val ITEM_VERTICAL_PADDING = 12.dp

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun GiniLabeledValueListPreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            GiniLabeledValueList(
                modifier = Modifier.padding(24.dp),
                title = "The following IBANs were detected",
                items = listOf(
                    GiniLabeledValueItem(
                        label = "In the QR code",
                        value = "PL12 1000 0111 0100 1000",
                        hint = "From Poland, not the sender's country",
                    ),
                    GiniLabeledValueItem(
                        label = "Printed on the document",
                        value = "DE89 3704 0044 0532 0130 00",
                    ),
                ),
            )
        }
    }
}
