package net.gini.android.capture.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.components.labeledvalue.GiniLabeledValue
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * A quiet, rounded container on the placeholder background that sets a piece of information
 * apart from the surrounding text, e.g. the sender of a document.
 *
 * @param modifier applied to the card
 * @param colors colors, see [GiniInfoCardColors.colors]
 * @param content the card body
 */
@Composable
fun GiniInfoCard(
    modifier: Modifier = Modifier,
    colors: GiniInfoCardColors = GiniInfoCardColors.colors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.containerColor, RoundedCornerShape(CORNER_RADIUS))
            .padding(CONTENT_PADDING),
        content = content,
    )
}

private val CORNER_RADIUS = 8.dp
private val CONTENT_PADDING = 12.dp

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun GiniInfoCardPreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            GiniInfoCard(modifier = Modifier.padding(24.dp)) {
                GiniLabeledValue(label = "Sender", value = "Elektro Brandt GmbH · Germany")
            }
        }
    }
}
