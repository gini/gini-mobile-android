package net.gini.android.capture.ui.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.gini.android.capture.R
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * The centred header of a sheet or dialog: an icon on a tinted rounded square, a title and an
 * optional description, all centred. The defaults are neutral; a warning passes the error tint
 * through [colors], see [GiniHeaderBlockColors.colors].
 *
 * The icon is decorative and hidden from accessibility services; the title is announced as a
 * heading.
 *
 * @param title the headline
 * @param icon the icon drawn on the tinted square
 * @param modifier applied to the whole block
 * @param description the secondary text below the title
 * @param colors colors of the block, see [GiniHeaderBlockColors.colors]
 */
@Composable
fun GiniHeaderBlock(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    description: String? = null,
    colors: GiniHeaderBlockColors = GiniHeaderBlockColors.colors(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ICON_TEXT_SPACING),
    ) {
        Icon(
            modifier = Modifier
                .background(colors.iconContainerColor, RoundedCornerShape(ICON_CORNER_RADIUS))
                .padding(ICON_PADDING),
            painter = icon,
            contentDescription = null,
            tint = colors.iconTint,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TITLE_DESCRIPTION_SPACING),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                text = title,
                style = GiniTheme.typography.headline5,
                color = colors.titleColor,
                textAlign = TextAlign.Center,
            )
            if (description != null) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = description,
                    style = GiniTheme.typography.subtitle1,
                    color = colors.descriptionColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val ICON_PADDING = 8.dp
private val ICON_CORNER_RADIUS = 12.dp
private val ICON_TEXT_SPACING = 24.dp
private val TITLE_DESCRIPTION_SPACING = 12.dp

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun GiniHeaderBlockPreview() {
    GiniTheme {
        GiniHeaderBlock(
            modifier = Modifier.padding(24.dp),
            title = "Different IBANs found on this document",
            description = "The IBAN in the QR code is different from the printed one",
            icon = painterResource(R.drawable.gc_fraud_alert_icon),
            colors = GiniHeaderBlockColors.colors(
                iconContainerColor = GiniTheme.colorScheme.errorHint.container,
                iconTint = GiniTheme.colorScheme.errorHint.content,
            ),
        )
    }
}
