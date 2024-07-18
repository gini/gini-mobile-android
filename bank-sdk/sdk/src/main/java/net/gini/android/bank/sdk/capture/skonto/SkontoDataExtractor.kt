package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData.Amount
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData.SkontoPaymentMethod
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import java.math.BigDecimal
import java.time.LocalDate


internal class SkontoDataExtractor {

    companion object {

        fun extractSkontoData(
            extractions: Map<String, GiniCaptureExtraction>,
            compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
        ): SkontoData {
            val totalAmountToPay = extractions["amountToPay"]
                ?: throw NoSuchElementException("Field `extractions.amountToPay` is missing")

            val skontoDiscountMaps = compoundExtractions["skontoDiscounts"]?.specificExtractionMaps
                ?: throw NoSuchElementException("Field `compoundExtractions.skontoDiscounts` is missing")

            return skontoDiscountMaps.map { skontoDiscountData ->
                val skontoPercentageDiscounted = skontoDiscountData.extractDataByKeys(
                    "skontoPercentageDiscounted",
                    "skontoPercentageDiscountedCalculated",
                ) ?: throw NoSuchElementException("Data for `PercentageDiscounted` is missing")

                val skontoPaymentMethod =
                    skontoDiscountData.extractDataByKeys("skontoPaymentMethod")


                val skontoAmountToPay = skontoDiscountData.extractDataByKeys(
                    "skontoAmountToPay",
                    "skontoAmountToPayCalculated"
                ) ?: throw NoSuchElementException("Skonto data for `AmountToPay` is missing")

                val skontoRemainingDays = skontoDiscountData.extractDataByKeys(
                    "skontoRemainingDays",
                    "skontoRemainingDaysCalculated"
                ) ?: throw NoSuchElementException("Skonto data for `RemainingDays` is missing")

                val skontoDueDate = skontoDiscountData.extractDataByKeys(
                    "skontoDueDate",
                    "skontoDueDateCalculated"
                ) ?: throw NoSuchElementException("Skonto data for `DueDate` is missing")

                val skontoAmountDiscounted = skontoDiscountData.extractDataByKeys(
                    "skontoAmountDiscounted",
                    "skontoAmountDiscountedCalculated"
                )

                SkontoData(
                    skontoPercentageDiscounted = BigDecimal(skontoPercentageDiscounted.value),
                    skontoAmountToPay = Amount.parse(skontoAmountToPay.value),
                    fullAmountToPay = Amount.parse(totalAmountToPay.value),
                    skontoRemainingDays = skontoRemainingDays.value.toInt(),
                    skontoDueDate = skontoDueDate.value.let(LocalDate::parse),
                    skontoPaymentMethod = skontoPaymentMethod?.let { SkontoPaymentMethod.valueOf(it.value) }
                )
            }.first()
        }
    }
}

fun Map<String, GiniCaptureSpecificExtraction>.extractDataByKeys(vararg keys: String) =
    keys.firstNotNullOfOrNull { this[it] }

