package net.gini.android.capture.ui.components.textinput.amount

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormatSymbols

class DecimalInputVisualTransformation(
    private val currencyCode: String,
    private val isCurrencyCodeDisplay: Boolean,
    private val decimalFormatter: DecimalFormatter,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        var formatted = decimalFormatter.formatDigits(source).trim()
        if (isCurrencyCodeDisplay) {
            formatted += " $currencyCode"
        }

        val offsetMapping = CustomOffsetMapping(
            source = source,
            formatted = formatted,
        )

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    class CustomOffsetMapping(
        private val formattingSymbols: DecimalFormatSymbols = DecimalFormatSymbols.getInstance(),
        private val source: String,
        private val formatted: String,
    ) : OffsetMapping {


        /*
         *   1       ->  0.01      | 1 -> 4
         *   12      ->  0.12      | 2 -> 4
         *   123     ->  1.23      | 3 -> 4
         *   1234    ->  12.34     | 4 -> 5
         *   12345   ->  123.45    | 5 -> 6
         *   123456  ->  1,234.56  | 6 -> 8
         *   1234567 ->  12,345.67 | 7 -> 9
         */

        override fun originalToTransformed(offset: Int): Int {
            val thousandSeparatorCount =
                formatted.count { it == formattingSymbols.groupingSeparator }

            val lastIndex = when (source.length) {
                in 1..3 -> formatted.length
                4 -> 5
                5 -> 6
                6 -> 8
                7 -> 9
                else -> formatted.length
            }

            return lastIndex
        }

        /*
         *    0.01      ->  1        | 4 -> 1
         *    0.12      ->  12       | 4 -> 2
         *    1.23      ->  123      | 4 -> 3
         *    12.34     ->  1234     | 5 -> 4
         *    123.45    ->  12345    | 6 -> 5
         *    1,234.56  ->  123456   | 8 -> 6
         *    12,345.67 ->  1234567  | 9 -> 7
         */


        override fun transformedToOriginal(offset: Int): Int {
            val thousandSeparatorCount =
                formatted.count { it == formattingSymbols.groupingSeparator }

            val lastIndex = when (formatted.length) {
                in 1..4 -> source.length
                5 -> 4
                6 -> 5
                8 -> 6
                9 -> 7
                else -> source.length
            }
            return lastIndex
        }
    }
}