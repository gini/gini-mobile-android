package net.gini.android.capture.ui.components.textinput

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniTextInput(
    text: String,
    label: String,
    trailingContent: @Composable () -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    giniTextInputColors: GiniTextInputColors = GiniTextInputColors.colors(),
) {
    GiniTextInput(
        modifier = modifier,
        text = text,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        label = {
            Text(
                text = label,
                style = GiniTheme.typography.caption1,
            )
        },
        onValueChange = onValueChange,
        trailingContent = trailingContent,
        giniTextInputColors = giniTextInputColors,
    )
}

@Composable
fun GiniTextInput(
    text: String,
    label: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    giniTextInputColors: GiniTextInputColors = GiniTextInputColors.colors(),
) {
    TextField(
        modifier = modifier,
        value = text,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        textStyle = GiniTheme.typography.subtitle1,
        label = label,
        colors = with(giniTextInputColors) {
            TextFieldDefaults.colors(
                focusedContainerColor = containerFocused,
                unfocusedContainerColor = containerUnfocused,
                disabledContainerColor = containerDisabled,
                focusedTextColor = textFocused,
                unfocusedTextColor = textUnfocused,
                disabledTextColor = textDisabled,
                errorTextColor = textError,
                focusedIndicatorColor = indicatorFocused,
                unfocusedIndicatorColor = indicatorUnfocused,
                disabledIndicatorColor = indicatorDisabled,
                errorIndicatorColor = indicatorError,
                focusedTrailingIconColor = trailingContentFocused,
                unfocusedTrailingIconColor = trailingContentUnfocused,
                disabledTrailingIconColor = trailingContentDisabled,
                errorTrailingIconColor = trailingContentError,
            )
        },
        onValueChange = onValueChange,
        trailingIcon = trailingContent
    )
}

@Preview(showBackground = true)
@Composable
private fun GiniTextInputPreviewLight() {
    GiniTextInputPreview()
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun GiniTextInputPreviewDark() {
    GiniTextInputPreview()
}

@Composable
private fun GiniTextInputPreview() {
    GiniTheme {
        GiniTextInput(
            modifier = Modifier.padding(16.dp),
            text = "Some text here",
            label = "Label Text",
            trailingContent = { },
            onValueChange = {}
        )
    }
}