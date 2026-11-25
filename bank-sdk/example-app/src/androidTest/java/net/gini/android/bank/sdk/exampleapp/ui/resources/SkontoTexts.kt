package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The Skonto texts of the Return Assistant screen, built the way the SDK builds them, so a test
 * compares against the SDK's own `R.string` in the device locale instead of a German or English
 * literal.
 *
 * The formatting copies `SkontoDiscountPercentageFormatter` and `SkontoRemainingDaysFormatter`,
 * which are `internal` to `bank-sdk:sdk`. Edit them together.
 */
object SkontoTexts {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    fun sdkString(resId: Int, vararg args: Any): String = context.getString(resId, *args)

    /** A percentage as the SDK writes it: "3.0" → "3%", "2.50" → "2.5%". */
    fun percent(percentage: String): String {
        val value = BigDecimal(percentage).setScale(2, RoundingMode.HALF_UP)
        return "${value.toString().trimEnd('0').trimEnd('.')}%"
    }

    /** The remaining days as the SDK writes them. */
    fun remainingDays(days: Int): String =
        if (days != 0) {
            context.resources.getQuantityString(
                net.gini.android.bank.sdk.R.plurals.days,
                days,
                days.toString()
            )
        } else {
            context.getString(net.gini.android.bank.sdk.R.string.days_zero)
        }

    /** The info text of a discount without an edge case, e.g. "Zahlung in 14 Tagen: 3% Skonto". */
    fun normalMessage(days: Int, percentage: String): String =
        sdkString(
            net.gini.android.bank.sdk.R.string.gbs_skonto_section_discount_info_banner_normal_message,
            remainingDays(days),
            percent(percentage)
        )

    /**
     * [normalMessage] with any number of days. Against the real API the remaining days come from
     * the backend and shrink every day, so only the percentage is fixed.
     */
    fun normalMessagePattern(percentage: String): Regex {
        val template = context.getString(
            net.gini.android.bank.sdk.R.string.gbs_skonto_section_discount_info_banner_normal_message
        )
        val parts = template.split("%1\$s", "%2\$s")
        require(parts.size == 3) { "Unexpected template: $template" }
        val daysFirst = template.indexOf("%1\$s") < template.indexOf("%2\$s")
        val days = ".+"
        val percent = Regex.escape(percent(percentage))
        val (first, second) = if (daysFirst) days to percent else percent to days
        return Regex(
            Regex.escape(parts[0]) + first + Regex.escape(parts[1]) + second + Regex.escape(parts[2])
        )
    }

    /** A one-argument format string with the argument removed, for "contains" checks. */
    fun templateWithoutArgument(resId: Int): String =
        context.getString(resId).replace("%1\$s", "").trim()
}
