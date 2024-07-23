package net.gini.android.capture.ui.components.textinput.amount

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.gini.android.capture.ui.components.textinput.GiniTextInput
import net.gini.android.capture.ui.components.textinput.GiniTextInputColors
import net.gini.android.capture.ui.theme.GiniTheme
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat

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
) {
    val parsedAmount = decimalFormatter.parseAmount(amount)

    var text by remember { mutableStateOf(parsedAmount) }

    LaunchedEffect(key1 = parsedAmount) { // we need to reset text if amount was changed only
        text = parsedAmount
    }

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
            text = decimalFormatter.textToDigits(it) // take only 7 digits
            onValueChange(decimalFormatter.parseDigits(text))
        },
        trailingContent = trailingContent,
        colors = colors,
        visualTransformation = DecimalInputVisualTransformation(
            decimalFormatter = decimalFormatter,
            currencyCode = currencyCode,
            isCurrencyCodeDisplay = !enabled,
        ),
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
        GiniAmountTextInput(
            modifier = Modifier.padding(16.dp),
            amount = BigDecimal("1234"),
            label = "Label Text",
            trailingContent = { },
            currencyCode = "EUR",
            onValueChange = {}
        )
    }
}
