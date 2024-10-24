package net.gini.android.bank.sdk.capture.extractions.skonto

import net.gini.android.capture.Amount
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import java.math.BigDecimal

class SkontoExtractionsHandler {

    private var extractions: MutableMap<String, GiniCaptureSpecificExtraction> =
        mutableMapOf()

    private var compoundExtractions: MutableMap<String, GiniCaptureCompoundExtraction> =
        mutableMapOf()

    fun initialize(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    ) {
        this.extractions = extractions.toMutableMap()
        this.compoundExtractions = compoundExtractions.toMutableMap()
    }

    fun getExtractions(): Map<String, GiniCaptureSpecificExtraction> = extractions

    fun getCompoundExtractions(): Map<String, GiniCaptureCompoundExtraction> = compoundExtractions

    fun updateExtractions(
        totalAmount: Amount,
        skontoPercentage: BigDecimal,
        skontoAmount: Amount,
        paymentInDays: Int,
        discountDueDate: String
    ) {
        extractions["amountToPay"]?.value = totalAmount.amountToPay()

        val skontoDiscountMaps = compoundExtractions["skontoDiscounts"]?.specificExtractionMaps

        skontoDiscountMaps?.map { skontoDiscountData ->
            skontoDiscountData.putDataByKeys(
                skontoPercentage.toString(),
                "skontoPercentageDiscounted",
                "skontoPercentageDiscountedCalculated",
            ) ?: throw NoSuchElementException("Data for `PercentageDiscounted` is missing")

            skontoDiscountData.putDataByKeys(
                skontoAmount.amountToPay(),
                "skontoAmountToPay",
                "skontoAmountToPayCalculated"
            )

            skontoDiscountData.putDataByKeys(
                paymentInDays.toString(),
                "skontoRemainingDays",
                "skontoRemainingDaysCalculated"
            )

            skontoDiscountData.putDataByKeys(
                discountDueDate,
                "skontoDueDate",
                "skontoDueDateCalculated"
            )

            skontoDiscountData.putDataByKeys(
                skontoPercentage.toString(),
                "skontoAmountDiscounted",
                "skontoAmountDiscountedCalculated"
            )
        }
    }

    private fun MutableMap<String, GiniCaptureSpecificExtraction>.putDataByKeys(
        value: String,
        vararg keys: String
    ) =
        keys.firstNotNullOfOrNull { this[it]?.value = value }
}
