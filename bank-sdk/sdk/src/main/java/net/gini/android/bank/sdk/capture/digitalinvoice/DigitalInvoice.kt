package net.gini.android.bank.sdk.capture.digitalinvoice

import androidx.annotation.VisibleForTesting
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoAmountUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoDefaultSelectionStateUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoEdgeCaseUseCase
import net.gini.android.bank.sdk.capture.skonto.usecase.GetSkontoSavedAmountUseCase
import net.gini.android.capture.Amount
import net.gini.android.capture.AmountCurrency
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Currency

/**
 * Created by Alpar Szotyori on 11.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

@JvmSynthetic
internal val INTEGRAL_FORMAT = DecimalFormat("#,###")

@JvmSynthetic
internal val FRACTION_FORMAT = DecimalFormat(".00").apply { roundingMode = RoundingMode.DOWN }

/**
 * Internal use only.
 *
 * @suppress
 */
internal class DigitalInvoice(
    extractions: Map<String, GiniCaptureSpecificExtraction>,
    compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
    savedSelectableItems: List<SelectableLineItem>? = null,
    var skontoData: SkontoData? = null,
    private val getSkontoAmountUseCase: GetSkontoAmountUseCase,
    private val getSkontoDefaultSelectionStateUseCase: GetSkontoDefaultSelectionStateUseCase,
    private val getSkontoEdgeCaseUseCase: GetSkontoEdgeCaseUseCase,
    private val getSkontoSavedAmountUseCase: GetSkontoSavedAmountUseCase,
) {

    private var _extractions: Map<String, GiniCaptureSpecificExtraction> = extractions
    val extractions
        get() = _extractions

    private var _compoundExtractions: Map<String, GiniCaptureCompoundExtraction> =
        compoundExtractions
    val compoundExtractions
        get() = _compoundExtractions

    private var _selectableLineItems: List<SelectableLineItem>
    val selectableLineItems
        get() = _selectableLineItems

    private var _addons: List<DigitalInvoiceAddon>

    val addons
        get() = _addons

    private val amountToPay: BigDecimal


    var skontoEnabled: Boolean = getDefaultSkontoEnabledState()
        private set

    init {
        _selectableLineItems = when (savedSelectableItems) {
            null -> {
                lineItemsFromCompoundExtractions(compoundExtractions).map {
                    SelectableLineItem(
                        lineItem = it
                    )
                }
            }

            else -> {
                savedSelectableItems
            }
        }

        _addons = extractions.mapNotNull { (_, extraction) ->
            DigitalInvoiceAddon.createFromOrNull(extraction)
        }

        if (_addons.isEmpty())
            _addons = listOf(
                DigitalInvoiceAddon(
                    BigDecimal.ZERO,
                    Currency.getInstance(AmountCurrency.EUR.name),
                    AddonExtraction.OTHER_CHARGES
                )
            )

        amountToPay =
            extractions["amountToPay"]?.let { parsePriceString(it.value).first } ?: BigDecimal.ZERO
    }

    fun getSkontoSavedAmount(): Amount? = skontoData?.let { data ->
        val amount = getSkontoSavedAmountUseCase.execute(
            data.skontoAmountToPay.value,
            data.fullAmountToPay.value
        )
        data.skontoAmountToPay.copy(value = amount)
    }

    private fun getDefaultSkontoEnabledState(): Boolean = skontoData?.let { data ->
        val edgeCase =
            getSkontoEdgeCaseUseCase.execute(data.skontoDueDate, data.skontoPaymentMethod)
        getSkontoDefaultSelectionStateUseCase.execute(edgeCase)
    } ?: false

    companion object {
        fun lineItemTotalGrossPriceIntegralAndFractionalParts(lineItem: LineItem): Pair<String, String> {
            return lineItem.run {
                Pair(
                    priceIntegralPartWithCurrencySymbol(totalGrossPrice, currency),
                    totalGrossPrice.fractionalPart(FRACTION_FORMAT)
                )
            }
        }

        fun lineItemUnitPriceIntegralAndFractionalParts(lineItem: LineItem): Pair<String, String> {
            return lineItem.run {
                Pair(
                    priceIntegralPartWithCurrencySymbol(grossPrice, currency),
                    grossPrice.fractionalPart(FRACTION_FORMAT)
                )
            }
        }

        fun addonPriceIntegralAndFractionalParts(addon: DigitalInvoiceAddon): Pair<String, String> {
            return addon.run {
                Pair(
                    priceIntegralPartWithCurrencySymbol(price, currency),
                    price.fractionalPart(FRACTION_FORMAT)
                )
            }
        }

        @VisibleForTesting
        fun priceIntegralPartWithCurrencySymbol(price: BigDecimal, currency: Currency?) =
            currency?.let { c ->
                price.integralPartWithCurrency(c, INTEGRAL_FORMAT)
            } ?: price.integralPart(INTEGRAL_FORMAT)
    }


    private fun lineItemsFromCompoundExtractions(compoundExtractions: Map<String, GiniCaptureCompoundExtraction>): List<LineItem> =
        compoundExtractions["lineItems"]?.run {
            specificExtractionMaps.mapIndexed { index, lineItem ->
                LineItem(
                    index.toString(),
                    lineItem["description"]?.value ?: "",
                    lineItem["quantity"]?.value?.toIntOrNull() ?: 0,
                    lineItem["baseGross"]?.value ?: "0.00:EUR"
                )
            }
        } ?: emptyList()

    fun updateLineItem(selectableLineItem: SelectableLineItem) {
        val index =
            selectableLineItems.indexOfFirst { it.lineItem.id == selectableLineItem.lineItem.id }
        _selectableLineItems = when {
            index >= 0 -> {
                selectableLineItems.map { sli -> if (sli.lineItem.id == selectableLineItem.lineItem.id) selectableLineItem else sli }
            }

            else -> {
                selectableLineItems.toMutableList().apply {
                    add(selectableLineItem)
                }
            }
        }
        recalculateSkontoData()
    }

    private fun recalculateSkontoData() = skontoData?.apply {
        updateSkontoData(
            this.copy(
                skontoAmountToPay = this.skontoAmountToPay.copy(
                    value = getSkontoAmountUseCase.execute(
                        totalPrice(),
                        this.skontoPercentageDiscounted
                    )
                ),
                fullAmountToPay = this.fullAmountToPay.copy(
                    value = totalPrice(),
                )
            )
        )
    }

    fun removeLineItem(selectableLineItem: SelectableLineItem) {
        val index =
            selectableLineItems.indexOfFirst { it.lineItem.id == selectableLineItem.lineItem.id }
        if (index >= 0) {
            _selectableLineItems = selectableLineItems.toMutableList().apply {
                removeAt(index)
            }
        }
    }

    fun selectLineItem(selectableLineItem: SelectableLineItem) {
        selectableLineItems.find { sli -> sli.lineItem.id == selectableLineItem.lineItem.id }
            ?.let { sli ->
                sli.selected = true
                sli.reason = null
            }
        recalculateSkontoData()
    }

    fun updateSkontoData(skontoData: SkontoData?) {
        this.skontoData = skontoData
    }

    fun updateSkontoEnabled(enabled: Boolean) {
        this.skontoEnabled = enabled
    }

    fun deselectLineItem(selectableLineItem: SelectableLineItem, reason: GiniCaptureReturnReason?) {
        selectableLineItems.find { sli -> sli.lineItem.id == selectableLineItem.lineItem.id }
            ?.let { sli ->
                sli.selected = false
                sli.reason = reason
            }
        recalculateSkontoData()
    }

    fun totalPriceIntegralAndFractionalParts(): Pair<String, String> {
        val price = getAmountToPay()
        val currency = lineItemsCurency()
        return Pair(
            priceIntegralPartWithCurrencySymbol(price, currency),
            price.fractionalPart(FRACTION_FORMAT)
        )
    }

    @VisibleForTesting
    fun lineItemsCurency(): Currency? =
        selectableLineItems.firstOrNull()?.lineItem?.currency

    private fun deselectedLineItemsTotalGrossPriceSum(): BigDecimal =
        selectableLineItems.fold<SelectableLineItem, BigDecimal>(BigDecimal.ZERO) { sum, sli ->
            if (!sli.selected) sum.add(sli.lineItem.totalGrossPrice) else sum
        }

    private fun lineItemsTotalGrossPriceDiffs(): BigDecimal =
        selectableLineItems.fold<SelectableLineItem, BigDecimal>(BigDecimal.ZERO) { sum, sli ->
            if (!sli.addedByUser) sum.add(sli.lineItem.totalGrossPriceDiff) else sum
        }

    private fun userAddedLineItemsTotalGrossPriceSum(): BigDecimal =
        selectableLineItems.fold<SelectableLineItem, BigDecimal>(BigDecimal.ZERO) { sum, sli ->
            if (sli.addedByUser) sum.add(sli.lineItem.totalGrossPrice) else sum
        }

    fun newLineItem(): LineItem {
        val currency = lineItemsCurency() ?: Currency.getInstance("EUR")
        return LineItem(
            "",
            "",
            0,
            "0.00:${currency.currencyCode}"
        )
    }

    private fun totalPrice(): BigDecimal =
        if (amountToPay > BigDecimal.ZERO) {
            amountToPay
                .subtract(deselectedLineItemsTotalGrossPriceSum())
                .add(lineItemsTotalGrossPriceDiffs())
                .add(userAddedLineItemsTotalGrossPriceSum())
                .max(BigDecimal.ZERO)
        } else {
            amountToPay
        }

    internal fun getAmountToPay(): BigDecimal {
        return if (skontoEnabled) {
            skontoData?.skontoAmountToPay?.value!!
        } else {
            totalPrice()
        }
    }

    fun selectedAndTotalLineItemsCount(): Pair<Int, Int> =
        Pair(selectedLineItemsCount(), totalLineItemsCount())

    private fun selectedLineItemsCount(): Int =
        selectableLineItems.fold(0) { c, sli -> if (sli.selected) c + sli.lineItem.quantity else c }

    private fun totalLineItemsCount(): Int =
        selectableLineItems.fold(0) { c, sli -> c + sli.lineItem.quantity }

    fun updateLineItemExtractionsWithReviewedLineItems() {
        _compoundExtractions = compoundExtractions.mapValues { (name, extraction) ->
            when (name) {
                "lineItems" -> {
                    val cameraExtractions =
                        extraction.specificExtractionMaps.mapIndexed { index, lineItemExtractions ->
                            selectableLineItems.find { it.lineItem.id.toInt() == index }
                                ?.let { sli ->
                                    val extractions =
                                        lineItemExtractions.mapValues { (name, lineItemExtraction) ->
                                            when (name) {
                                                "description" -> copyGiniCaptureSpecificExtraction(
                                                    lineItemExtraction,
                                                    sli.lineItem.description
                                                )

                                                "baseGross" -> copyGiniCaptureSpecificExtraction(
                                                    lineItemExtraction,
                                                    sli.lineItem.rawGrossPrice
                                                )

                                                "quantity" -> copyGiniCaptureSpecificExtraction(
                                                    lineItemExtraction,
                                                    if (sli.selected) {
                                                        sli.lineItem.quantity.toString()
                                                    } else {
                                                        "0"
                                                    }
                                                )

                                                else -> lineItemExtraction
                                            }
                                        }.toMutableMap()
                                    sli.reason?.let { returnReason ->
                                        extractions.put(
                                            "returnReason", GiniCaptureSpecificExtraction(
                                                "returnReason",
                                                returnReason.id,
                                                "",
                                                null,
                                                emptyList()
                                            )
                                        )
                                    }
                                    extractions
                                }
                        }.filterNotNull().toMutableList()


                    val userAddedExtractions = selectableLineItems.filter { it.addedByUser }
                        .map {
                            it.lineItem.asGiniExtractionMap()
                        }
                    cameraExtractions.addAll(userAddedExtractions)


                    return@mapValues GiniCaptureCompoundExtraction(
                        name,
                        cameraExtractions
                    )
                }

                else -> return@mapValues extraction
            }
        }
    }

    fun updateAmountToPayExtractionWithTotalPrice() {
        val totalPrice = getAmountToPay().toPriceString(
            selectableLineItems.firstOrNull()?.lineItem?.rawCurrency ?: "EUR"
        )

        _extractions = if (extractions.containsKey("amountToPay")) {
            extractions.mapValues { (name, extraction) ->
                when (name) {
                    "amountToPay" -> copyGiniCaptureSpecificExtraction(extraction, totalPrice)
                    else -> extraction
                }
            }
        } else {
            extractions.toMutableMap().apply {
                put(
                    "amountToPay",
                    GiniCaptureSpecificExtraction(
                        "amountToPay",
                        totalPrice,
                        "amount",
                        null,
                        emptyList()
                    )
                )
            }
        }
    }

    @JvmSynthetic
    private fun copyGiniCaptureSpecificExtraction(
        other: GiniCaptureSpecificExtraction,
        value: String
    ) =
        GiniCaptureSpecificExtraction(other.name, value, other.entity, other.box, other.candidates)
}

fun LineItem.asGiniExtractionMap(): MutableMap<String, GiniCaptureSpecificExtraction> {
    return mutableMapOf(
        "description" to GiniCaptureSpecificExtraction(
            "description",
            this.description,
            "",
            null,
            emptyList()
        ),
        "baseGross" to GiniCaptureSpecificExtraction(
            "baseGross",
            this.rawGrossPrice,
            "",
            null,
            emptyList()
        ),
        "quantity" to GiniCaptureSpecificExtraction(
            "quantity",
            this.quantity.toString(),
            "",
            null,
            emptyList()
        ),
    )
}