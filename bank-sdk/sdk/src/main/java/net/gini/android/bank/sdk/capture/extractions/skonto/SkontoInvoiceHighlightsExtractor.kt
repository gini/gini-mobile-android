package net.gini.android.bank.sdk.capture.extractions.skonto

import net.gini.android.bank.sdk.capture.skonto.extractDataByKeys
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import kotlin.jvm.Throws

internal class SkontoInvoiceHighlightsExtractor {

    fun extract(
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
    ): List<SkontoInvoiceHighlightBoxes> {

        val skontoDiscountMaps = compoundExtractions["skontoDiscounts"]?.specificExtractionMaps
            ?: throw NoSuchElementException("Field `compoundExtractions.skontoDiscounts` is missing")

        return skontoDiscountMaps.map { skontoDiscountData ->
            val skontoPercentageDiscounted = extractPercentageDiscountedOrError(skontoDiscountData)
            val skontoPaymentMethod = skontoDiscountData.extractDataByKeys("skontoPaymentMethod")
            val skontoAmountToPay = extractAmountToPayOrError(skontoDiscountData)
            val skontoRemainingDays = extractRemainingDaysOrError(skontoDiscountData)
            val skontoDuePeriod = extractSkontoDuePeriod(skontoDiscountData)

            val skontoDueDate = extractDueDateOrError(skontoDiscountData)

            val skontoAmountDiscounted = skontoDiscountData.extractDataByKeys(
                "skontoAmountDiscounted",
                "skontoAmountDiscountedCalculated"
            )

            SkontoInvoiceHighlightBoxes(
                skontoPercentageDiscounted = skontoPercentageDiscounted.box,
                skontoPaymentMethod = skontoPaymentMethod?.box,
                skontoRemainingDays = skontoRemainingDays.box,
                skontoDueDate = skontoDueDate.box,
                skontoAmountToPay = skontoAmountToPay.box,
                skontoAmountDiscounted = skontoAmountDiscounted?.box,
                skontoDuePeriod = skontoDuePeriod?.box
            )
        }
    }

    @Throws(NoSuchElementException::class)
    private fun extractPercentageDiscountedOrError(
        skontoDiscountMap: Map<String, GiniCaptureSpecificExtraction>
    ): GiniCaptureSpecificExtraction =
        skontoDiscountMap.extractDataByKeys(
            "skontoPercentageDiscounted",
            "skontoPercentageDiscountedCalculated",
        ) ?: throw NoSuchElementException("Data for `PercentageDiscounted` is missing")

    @Throws(NoSuchElementException::class)
    private fun extractAmountToPayOrError(
        skontoDiscountMap: Map<String, GiniCaptureSpecificExtraction>
    ): GiniCaptureSpecificExtraction =
        skontoDiscountMap.extractDataByKeys(
            "skontoAmountToPay",
            "skontoAmountToPayCalculated"
        ) ?: throw NoSuchElementException("Skonto data for `AmountToPay` is missing")

    @Throws(NoSuchElementException::class)
    private fun extractRemainingDaysOrError(
        skontoDiscountMap: Map<String, GiniCaptureSpecificExtraction>
    ): GiniCaptureSpecificExtraction =
        skontoDiscountMap.extractDataByKeys(
            "skontoRemainingDays",
            "skontoRemainingDaysCalculated"
        ) ?: throw NoSuchElementException("Skonto data for `RemainingDays` is missing")

    @Throws(NoSuchElementException::class)
    private fun extractDueDateOrError(
        skontoDiscountMap: Map<String, GiniCaptureSpecificExtraction>
    ): GiniCaptureSpecificExtraction =
        skontoDiscountMap.extractDataByKeys(
            "skontoDueDate",
            "skontoDueDateCalculated"
        ) ?: throw NoSuchElementException("Skonto data for `DueDate` is missing")

    private fun extractSkontoDuePeriod(
        skontoDiscountMap: Map<String, GiniCaptureSpecificExtraction>
    ): GiniCaptureSpecificExtraction? =
        skontoDiscountMap.extractDataByKeys(
            "skontoDuePeriod",
            "skontoDuePeriodCalculated"
        )

}
