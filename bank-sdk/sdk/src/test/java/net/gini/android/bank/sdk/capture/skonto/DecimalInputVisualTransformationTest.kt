package net.gini.android.bank.sdk.capture.skonto

import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigDecimal

@RunWith(JUnit4::class)
class DecimalInputVisualTransformationTest {

    @Test
    fun testPriceFormatting() {
        val value = BigDecimal.ZERO
        val formatter = amountDecimalFormatFactory()

        val decimalFormatter = DecimalFormatter(formatter)

        assertEquals("0,00", formatter.format(value))
    }
}