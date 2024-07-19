package net.gini.android.capture.ui.components.textinput

import android.content.res.Configuration
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.theme.GiniTheme

@Composable
fun GiniTextInput(
    text: String,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    trailingContent: @Composable () -> Unit = {},
    enabled: Boolean = true,
    isError: Boolean = false,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    colors: GiniTextInputColors = GiniTextInputColors.colors(),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    GiniTextInput(
        modifier = modifier,
        text = text,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        label = {
            Text(
                text = label,
                style = GiniTheme.typography.caption1,
            )
        },
        onValueChange = onValueChange,
        trailingContent = trailingContent,
        colors = colors,
        visualTransformation = visualTransformation,
    )
}

@Composable
fun GiniTextInput(
    text: String,
    label: @Composable () -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {},
    enabled: Boolean = true,
    isError: Boolean = false,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    colors: GiniTextInputColors = GiniTextInputColors.colors(),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    TextField(
        modifier = modifier,
        value = text,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        textStyle = GiniTheme.typography.subtitle1,
        label = label,
        isError = isError,
        readOnly = readOnly,
        interactionSource = interactionSource,
        visualTransformation = visualTransformation,
        colors = with(colors) {
            TextFieldDefaults.colors(
                focusedContainerColor = containerFocused,
                unfocusedContainerColor = containerUnfocused,
                disabledContainerColor = containerUnfocused,
                focusedTextColor = textFocused,
                unfocusedTextColor = textUnfocused,
                disabledTextColor = textDisabled,
                errorTextColor = textError,
                unfocusedLabelColor = labelUnfocused,
                errorLabelColor = labelError,
                focusedLabelColor = labelFocused,
                disabledLabelColor = labelDisabled,
                focusedIndicatorColor = indicatorFocused,
                unfocusedIndicatorColor = indicatorUnfocused,
                disabledIndicatorColor = indicatorDisabled,
                errorIndicatorColor = indicatorError,
                focusedTrailingIconColor = trailingContentFocused,
                unfocusedTrailingIconColor = trailingContentUnfocused,
                disabledTrailingIconColor = trailingContentDisabled,
                errorTrailingIconColor = trailingContentError,
                errorContainerColor = containerUnfocused,
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