package net.gini.android.capture.ui.components.textinput

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.gini.android.capture.ui.theme.GiniTheme
import net.gini.android.capture.util.getSpokenDateForTalkBack


/**
 * Delay duration (in milliseconds) used to allow the view to settle down before requesting focus.
 *
 * A value of 500ms was chosen based on observed behaviour on Android 10 devices and below, where
 * immediately requesting keyboard focus after view creation can result in the keyboard not
 * appearing.
 * This delay helps ensure that the keyboard is reliably shown when the field requests focus.
 */
private const val VIEW_SETTLE_DOWN_DELAY_FOR_FOCUS = 500L

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
    supportingText: @Composable (() -> Unit)? = null,
    shouldFieldShowKeyboard: Boolean = false,
    isDate: Boolean = false,
    isPhoneInLandscape: Boolean
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
        supportingText = supportingText,
        shouldFieldShowKeyboard = shouldFieldShowKeyboard,
        isDate = isDate,
        isPhoneInLandscape = isPhoneInLandscape,
        labelText = label
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
    supportingText: @Composable (() -> Unit)? = null,
    shouldFieldShowKeyboard: Boolean = false,
    isDate: Boolean = false,
    labelText : String = "",
    isPhoneInLandscape: Boolean
) {

    val focusRequester = remember { FocusRequester() }
    val baseModifier = modifier.then(Modifier.focusRequester(focusRequester))
    if (isDate) {
        val extendedModifier = baseModifier
            .then(
                Modifier.clearAndSetSemantics {
                    // we need to use clear semantics here, if we don't, it will read the date twice.
                    contentDescription = getSpokenDateForTalkBack(text) + labelText
                }
            )

        // In date, the trailing content will be displayed separately in landscape mode.
        // So, we need to use an empty composable to avoid TextField trailing icon.
        val emptyComposable: @Composable () -> Unit = {}

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GiniBaseTextField(
                text = text,
                label = label,
                onValueChange = onValueChange,
                modifier = extendedModifier.then(Modifier.weight(1f)),
                enabled = enabled,
                isError = isError,
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                colors = colors,
                trailingContent = if (isPhoneInLandscape) emptyComposable else trailingContent,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                supportingText = supportingText,
                shouldFieldShowKeyboard = shouldFieldShowKeyboard,
                focusRequester = focusRequester
            )
            if (isPhoneInLandscape) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 8.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    trailingContent()
                }
            }
        }
    } else  {
        GiniBaseTextField(
            text = text,
            label = label,
            onValueChange = onValueChange,
            modifier = baseModifier,
            trailingContent = trailingContent,
            enabled = enabled,
            isError = isError,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            colors = colors,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            supportingText = supportingText,
            shouldFieldShowKeyboard = shouldFieldShowKeyboard,
            focusRequester = focusRequester
        )
    }
}

@Composable
fun GiniBaseTextField(
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
    supportingText: @Composable (() -> Unit)? = null,
    shouldFieldShowKeyboard: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = TextFieldValue(
            text = text,
            selection = TextRange(text.length)
        ),
        modifier = modifier,
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
        onValueChange = { newValue ->
            onValueChange(newValue.text)
        },
        trailingIcon = trailingContent,
        supportingText = supportingText,
    )

    LaunchedEffect(shouldFieldShowKeyboard) {
        if (shouldFieldShowKeyboard) {
            focusRequester.requestFocus()
            /** keyboardController?.show() fails in Android 10 & below if the focus is not
             * in place, That's why delay is added, so the window is already settled before showing
             * the keyboard. */
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                delay(VIEW_SETTLE_DOWN_DELAY_FOR_FOCUS)
            }
            keyboardController?.show()
        }
    }
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
            onValueChange = {},
            isPhoneInLandscape = false,
        )
    }
}