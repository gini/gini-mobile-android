package net.gini.android.capture.ui.components.textinput.amount

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random


@RunWith(JUnit4::class)
class CustomOffsetMappingTest {

    @Test
    fun testCustomOffsetMapping() {

        val testData = listOf(
            PossibleCursorPosition("1", "0.01", 9, 1),
            PossibleCursorPosition("12", "0.12", 9, 2),
            PossibleCursorPosition("123", "1.23", 9, 3),
            PossibleCursorPosition("1234", "12.34", 9, 4),
            PossibleCursorPosition("12345", "123.45", 9, 5),
            PossibleCursorPosition("123456", "1,234.56", 9, 6),
            PossibleCursorPosition("1234567", "12,345.67", 9, 7),

            PossibleCursorPosition("1", "0.01", 8, 1),
            PossibleCursorPosition("12", "0.12", 8, 2),
            PossibleCursorPosition("123", "1.23", 8, 3),
            PossibleCursorPosition("1234", "12.34", 8, 4),
            PossibleCursorPosition("12345", "123.45", 8, 5),
            PossibleCursorPosition("123456", "1,234.56", 8, 6),
            PossibleCursorPosition("1234567", "12,345.67", 8, 7),

            PossibleCursorPosition("1", "0.01", 7, 1),
            PossibleCursorPosition("12", "0.12", 7, 2),
            PossibleCursorPosition("123", "1.23", 7, 3),
            PossibleCursorPosition("1234", "12.34", 7, 4),
            PossibleCursorPosition("12345", "123.45", 7, 5),
            PossibleCursorPosition("123456", "1,234.56", 7, 5),
            PossibleCursorPosition("1234567", "12,345.67", 7, 5),

            PossibleCursorPosition("1", "0.01", 6, 1),
            PossibleCursorPosition("12", "0.12", 6, 2),
            PossibleCursorPosition("123", "1.23", 6, 3),
            PossibleCursorPosition("1234", "12.34", 6, 4),
            PossibleCursorPosition("12345", "123.45", 6, 5),
            PossibleCursorPosition("123456", "1,234.56", 6, 4),
            PossibleCursorPosition("1234567", "12,345.67", 6, 5),
        )


        testData.forEach {

            val mapping = DecimalInputVisualTransformation.CustomOffsetMapping(
                source = it.source,
                formatted = it.formatted
            )

             /*Assert.assertEquals(
                 "Invalid result for pair: ${it.source} -> ${it.formatted}. Offset: ${it.offset}",
                 mapping.originalToTransformed(Random.nextInt()),
                 formatted.length
             )
            Assert.assertEquals(
                "Invalid result for pair: ${it.source} -> ${it.formatted}. Offset: ${it.offset}",
                it.transformedToOriginal,
                mapping.transformedToOriginal(Random.nextInt())
            )*/
            // TODO Uncomment this to enable unit testing of cursor
        }
    }

    private class PossibleCursorPosition(
        val source: String,
        val formatted: String,
        val offset: Int,
        val transformedToOriginal: Int,

        )

}