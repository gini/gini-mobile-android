@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.ui.components.switcher

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    giniSwitchColors: GiniSwitchColors = GiniSwitchColors.colors(),
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {

        Switch(modifier = modifier.scale(0.7f),
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = with(giniSwitchColors) {
                SwitchDefaults.colors(
                    uncheckedTrackColor = uncheckedTrackColor,
                    checkedTrackColor = checkedTrackColor,
                    uncheckedThumbColor = uncheckedThumbColor,
                    checkedThumbColor = checkedThumbColor,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                )
            })
    }
}

@Preview(showBackground = true)
@Composable
private fun GiniSwitchPreview() {
    var checked by remember { mutableStateOf(false) }
    GiniTheme {
        GiniSwitch(checked = checked, onCheckedChange = { checked = !checked })
    }
}