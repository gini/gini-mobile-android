package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceException.*
import net.gini.android.bank.sdk.capture.skonto.SkontoException.*
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction

internal typealias Validate = (compoundExtractions: Map<String, GiniCaptureCompoundExtraction>) -> Unit

/**
 * It checks that the compound extractions contain valid skonto objects which can be used to show the Skonto screen.
 */
class SkontoValidator {

    companion object {

        /**
         * Checks that the compound extractions contain valid line items.
         *
         * In case it's not valid an appropriate [SkontoException] subclass will be thrown.
         *
         * @param compoundExtractions a map of [GiniCaptureCompoundExtraction]s
         * @throws SkontoMissingException if skontoDiscounts are missing from the compound extractions
         * @throws DueDateMissingException if skontoDueDate is missing from skontoDiscaounts object
         * @throws AmountToPayMissingException if skontoAmountToPay is missing from skontoDiscaounts object
         * @throws RemainingDaysMissingException if skontoRemainingDays is missing from skontoDiscaounts object
         * @throws PercentageDiscountedMissingException if skontoPercentageDiscounted is missing from skontoDiscaounts object
         */
        @JvmStatic
        @Throws(Exception::class)
        fun validate(compoundExtractions: Map<String, GiniCaptureCompoundExtraction>) = listOf(
            skontoDiscountsAvailable,
            skontoDueDateAvailable,
            skontoAmountToPayAvailable,
            skontoRemainingDaysAvailable,
            skontoPercentageDiscountedAvailable
        ).forEach { it(compoundExtractions) }
    }
}

//mandatory
internal val skontoDiscountsAvailable: Validate = { compoundExtractions ->
    if (!compoundExtractions.containsKey("skontoDiscounts")) {
        throw SkontoMissingException()
    }
}
//mandatory
internal val skontoDueDateAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["skontoDiscounts"]?.specificExtractionMaps?.all { it.containsKey("skontoDueDate") || it.containsKey("skontoDueDateCalculated") }) != true) {
        throw DueDateMissingException()
    }
}
//not mandatory
internal val skontoAmountDiscountedAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["skontoDiscounts"]?.specificExtractionMaps?.all { it.containsKey("skontoAmountDiscounted") || it.containsKey("skontoAmountDiscountedCalculated") }) != true) {
        throw AmountDiscountedMissingException()
    }
}
//mandatory
internal val skontoAmountToPayAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["skontoDiscounts"]?.specificExtractionMaps?.all { it.containsKey("skontoAmountToPay") || it.containsKey("skontoAmountToPayCalculated") }) != true) {
        throw AmountToPayMissingException()
    }
}

//mandatory
internal val skontoRemainingDaysAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["skontoDiscounts"]?.specificExtractionMaps?.all { it.containsKey("skontoRemainingDays") || it.containsKey("skontoRemainingDaysCalculated") }) != true) {
        throw RemainingDaysMissingException()
    }
}

//not mandatory
internal val skontoPaymentMethodAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["skontoDiscounts"]?.specificExtractionMaps?.all { it.containsKey("skontoPaymentMethod") }) != true) {
        throw PaymentMethodMissingException()
    }
}

//mandatory
internal val skontoPercentageDiscountedAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["skontoDiscounts"]?.specificExtractionMaps?.all { it.containsKey("skontoPercentageDiscounted") || it.containsKey("skontoPercentageDiscountedCalculated") }) != true) {
        throw PercentageDiscountedMissingException()
    }
}

