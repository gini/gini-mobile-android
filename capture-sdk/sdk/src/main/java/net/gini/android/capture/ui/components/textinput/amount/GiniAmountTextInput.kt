package net.gini.android.capture.ui.components.textinput.amount

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.components.textinput.GiniTextInput
import net.gini.android.capture.ui.components.textinput.GiniTextInputColors
import net.gini.android.capture.ui.theme.GiniTheme
import java.math.BigDecimal

@Composable
fun GiniAmountTextInput(
    amount: BigDecimal,
    currencyCode: String,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (BigDecimal) -> Unit,
    trailingContent: @Composable () -> Unit = {},
    enabled: Boolean = true,
    isError: Boolean = false,
    decimalFormatter: DecimalFormatter = DecimalFormatter(),
    colors: GiniTextInputColors = GiniTextInputColors.colors(),
    supportingText: String? = null,
    shouldFieldShowKeyboard: Boolean = false,
    onNewValue: (String) -> Unit = {},
    isPhoneInLandscape: Boolean
) {
    val parsedAmount = decimalFormatter.parseAmount(amount)

    var text by remember { mutableStateOf(parsedAmount) }

    text = parsedAmount

    GiniTextInput(
        modifier = modifier,
        text = text,
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        label = label,
        onValueChange = {
            onNewValue(it)
            val newText = decimalFormatter.textToDigits(it) // take only 7 digits
            if (newText != text) {
                text = newText
                onValueChange(decimalFormatter.parseDigits(text))
            }
        },
        trailingContent = trailingContent,
        colors = colors,
        visualTransformation = DecimalInputVisualTransformation(
            decimalFormatter = decimalFormatter,
            currencyCode = currencyCode,
            isCurrencyCodeDisplay = !enabled,
        ),
        supportingText = supportingText?.let {
            {
                Text(
                    text = supportingText,
                    color = colors.textError,
                    style = GiniTheme.typography.caption1,
                )
            }
        },
        shouldFieldShowKeyboard = shouldFieldShowKeyboard,
        isPhoneInLandscape = isPhoneInLandscape
    )
}


@Preview(showBackground = true)
@Composable
private fun GiniTextInputPreviewLight() {
    GiniTextInputPreview()
    GiniTextInputPreviewError()
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun GiniTextInputPreviewDark() {
    GiniTextInputPreview()
    GiniTextInputPreviewError()
}

@Composable
private fun GiniTextInputPreview() {
    GiniTheme {
        GiniAmountTextInput(
            amount = BigDecimal("1234"),
            currencyCode = "EUR",
            label = "Label Text",
            modifier = Modifier.padding(16.dp),
            onValueChange = {},
            trailingContent = { },
            onNewValue = {
            },
            isPhoneInLandscape = false,
        )
    }
}

@Composable
private fun GiniTextInputPreviewError() {
    GiniTheme {
        GiniAmountTextInput(
            amount = BigDecimal("1234"),
            currencyCode = "EUR",
            label = "Label Text",
            modifier = Modifier.padding(16.dp),
            onValueChange = {},
            trailingContent = { },
            isError = true,
            supportingText = "Error text",
            onNewValue = {},
            isPhoneInLandscape = false
        )
    }
}

