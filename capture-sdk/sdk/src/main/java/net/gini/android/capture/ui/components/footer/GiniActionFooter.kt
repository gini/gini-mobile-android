package net.gini.android.capture.ui.components.footer

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.components.GiniComposableStyleProviderConfig
import net.gini.android.capture.ui.components.button.filled.GiniButton
import net.gini.android.capture.ui.components.button.filled.GiniButtonColors
import net.gini.android.capture.ui.components.button.outlined.GiniOutlinedButton
import net.gini.android.capture.ui.components.button.outlined.GiniOutlinedButtonColors
import net.gini.android.capture.ui.compose.GiniScreenPreviewSizes
import net.gini.android.capture.ui.compose.GiniScreenPreviewUiModes
import net.gini.android.capture.ui.theme.GiniTheme

/**
 * The action area at the bottom of a sheet: a filled primary button and an optional outlined
 * secondary button. The buttons are stacked in portrait and placed side by side in landscape,
 * each taking half of the width. The split follows orientation rather than width class on
 * purpose: the design stacks the buttons on tablets in portrait and puts them side by side on
 * phones in landscape.
 *
 * @param primaryText label of the filled button
 * @param onPrimaryClick called when the filled button is tapped
 * @param composableProviderConfig the integrator's button style, forwarded to [GiniButton]
 * @param modifier applied to the footer
 * @param secondaryText label of the outlined button; `null` shows the primary button alone
 * @param onSecondaryClick called when the outlined button is tapped
 * @param horizontal whether the buttons sit side by side; defaults to landscape orientation
 * @param primaryColors colors of the filled button
 * @param secondaryColors colors of the outlined button
 */
@Composable
internal fun GiniActionFooter(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    composableProviderConfig: GiniComposableStyleProviderConfig?,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondaryClick: () -> Unit = {},
    horizontal: Boolean = isLandscape(),
    primaryColors: GiniButtonColors = GiniButtonColors.colors(),
    secondaryColors: GiniOutlinedButtonColors = GiniOutlinedButtonColors.colors(),
) {
    if (horizontal && secondaryText != null) {
        Row(
            // IntrinsicSize.Min + fillMaxHeight keep both buttons equally tall when one label
            // wraps onto more lines than the other at large font sizes.
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING),
        ) {
            GiniButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                text = primaryText,
                onClick = onPrimaryClick,
                composableProviderConfig = composableProviderConfig,
                giniButtonColors = primaryColors,
            )
            GiniOutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                text = secondaryText,
                onClick = onSecondaryClick,
                giniButtonColors = secondaryColors,
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING),
        ) {
            GiniButton(
                modifier = Modifier.fillMaxWidth(),
                text = primaryText,
                onClick = onPrimaryClick,
                composableProviderConfig = composableProviderConfig,
                giniButtonColors = primaryColors,
            )
            if (secondaryText != null) {
                GiniOutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = secondaryText,
                    onClick = onSecondaryClick,
                    giniButtonColors = secondaryColors,
                )
            }
        }
    }
}

@Composable
private fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

private val BUTTON_SPACING = 16.dp

@GiniScreenPreviewUiModes
@GiniScreenPreviewSizes
@Composable
private fun GiniActionFooterPreview() {
    GiniTheme {
        Surface(color = GiniTheme.colorScheme.dialogs.container) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                GiniActionFooter(
                    primaryText = "Cancel transfer",
                    onPrimaryClick = {},
                    secondaryText = "Proceed anyway",
                    onSecondaryClick = {},
                    composableProviderConfig = null,
                    horizontal = false,
                )
                GiniActionFooter(
                    primaryText = "Cancel transfer",
                    onPrimaryClick = {},
                    secondaryText = "Proceed anyway",
                    onSecondaryClick = {},
                    composableProviderConfig = null,
                    horizontal = true,
                )
                GiniActionFooter(
                    primaryText = "Continue",
                    onPrimaryClick = {},
                    composableProviderConfig = null,
                )
            }
        }
    }
}
