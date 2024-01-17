package net.gini.android.bank.sdk.capture.digitalinvoice.details

import android.app.Activity
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoice
import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsScreenContract.Presenter
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsScreenContract.View
import net.gini.android.bank.sdk.capture.digitalinvoice.toPriceString
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.ParseException
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import java.util.*

/**
 * Created by Alpar Szotyori on 17.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

@JvmSynthetic
internal val GROSS_PRICE_FORMAT_PATTERN = "#,##0.00"

internal const val MIN_QUANTITY = 1
internal const val MAX_QUANTITY = 99_999

/**
 * Internal use only.
 *
 * @suppress
 */
internal class LineItemDetailsScreenPresenter(
    activity: Activity, view: View,
    var selectableLineItem: SelectableLineItem,
    val returnReasons: List<GiniCaptureReturnReason> = emptyList(),
    private val grossPriceFormat: DecimalFormat = DecimalFormat(GROSS_PRICE_FORMAT_PATTERN).apply {
        isParseBigDecimal = true
    }
) :
    Presenter(activity, view) {

    private val originalLineItem: SelectableLineItem = selectableLineItem.copy()

    init {
        view.setPresenter(this)
    }

    override fun selectLineItem() {
        if (selectableLineItem.selected) {
            return
        }
        selectableLineItem.selected = true
        selectableLineItem.reason = null
        view.apply {
            enableInput()
            updateCheckboxAndSaveButton()
        }
    }

    override fun deselectLineItem() {
        if (!selectableLineItem.selected) {
            return
        }
        if (canShowReturnReasonsDialog()) {
            view.showReturnReasonDialog(returnReasons) { selectedReason ->
                if (selectedReason != null) {
                    selectableLineItem.selected = false
                    selectableLineItem.reason = selectedReason
                    view.disableInput()
                } else {
                    selectableLineItem.selected = true
                    selectableLineItem.reason = null
                    view.enableInput()
                }
                updateCheckboxAndSaveButton()
            }
        } else {
            selectableLineItem.selected = false
            selectableLineItem.reason = null
            view.disableInput()
            updateCheckboxAndSaveButton()
        }
    }

    private fun canShowReturnReasonsDialog() = GiniBank.enableReturnReasons && returnReasons.isNotEmpty()

    private fun updateCheckboxAndSaveButton() = selectableLineItem.let {
        view.apply {
            showCheckbox(it.selected, it.lineItem.quantity, !it.addedByUser)
            updateSaveButton(it, originalLineItem, it.lineItem.grossPrice > BigDecimal.ZERO)
        }
    }

    override fun setDescription(description: String) {
        if (selectableLineItem.lineItem.description == description) {
            return
        }
        selectableLineItem = selectableLineItem.copy(
            lineItem = selectableLineItem.lineItem.copy(description = description)
        ).also {
            view.updateSaveButton(it, originalLineItem, it.lineItem.grossPrice > BigDecimal.ZERO)
        }
    }

    override fun setQuantity(quantity: Int) {
        if (selectableLineItem.lineItem.quantity == quantity) {
            return
        }

        val validQuantity = quantity.coerceAtLeast(MIN_QUANTITY).coerceAtMost(MAX_QUANTITY)

        selectableLineItem = selectableLineItem.copy(
            lineItem = selectableLineItem.lineItem.copy(quantity = validQuantity)
        )
        view.showTotalGrossPrice(selectableLineItem)
        updateCheckboxAndSaveButton()

        if (validQuantity != quantity) {
            view.showQuantity(validQuantity)
        }
    }

    override fun setGrossPrice(displayedGrossPrice: String) {
        val grossPrice = try {
            grossPriceFormat.parse(displayedGrossPrice) as BigDecimal
        } catch (_: ParseException) {
            view.apply {
                updateSaveButton(selectableLineItem, originalLineItem, false)
            }
            return
        }
        if (selectableLineItem.lineItem.grossPrice == grossPrice) {
            return
        }
        selectableLineItem = selectableLineItem.copy(
            lineItem = selectableLineItem.lineItem.copy(
                rawGrossPrice = grossPrice.toPriceString(selectableLineItem.lineItem.rawCurrency)
            )
        ).also {
            view.apply {
                showTotalGrossPrice(it)
                updateSaveButton(it, originalLineItem, grossPrice > BigDecimal.ZERO)
            }
        }
    }

    override fun save() {
        when {
            selectableLineItem.lineItem.id.isBlank() -> {
                val lineItem = selectableLineItem.lineItem.copy(UUID.randomUUID().toString())
                view.onSave(selectableLineItem.copy(lineItem = lineItem))
//                listener?.onSave(selectableLineItem.copy(lineItem = lineItem))
            }
            else -> {
                view.onSave(selectableLineItem)
//                listener?.onSave(selectableLineItem)
            }
        }
    }

    override fun onQuantityInputFieldFocusLoss(inputFieldText: String) {
        val quantity = inputFieldText.toIntOrNull()
        if (quantity == null || (selectableLineItem.lineItem.quantity != quantity)) {
            view.showQuantity(selectableLineItem.lineItem.quantity)
        }
    }


    override fun validateLineItemName(name: String): Boolean {
        return name.isNotBlank()
    }

    override fun validateLineItemGrossPrice(displayedGrossPrice: String): Boolean {
        return try {
            val grossPrice = grossPriceFormat.parse(displayedGrossPrice) as BigDecimal
            return grossPrice.stripTrailingZeros() != BigDecimal.ZERO
        } catch (_: ParseException) {
            false
        }
    }

    override fun start() {
        view.apply {
            selectableLineItem.run {
                lineItem.run {
                    showDescription(description)
                    showQuantity(quantity)
                    showGrossPrice(grossPriceFormat.format(grossPrice), currency?.symbol ?: "")
                }
                showTotalGrossPrice(this)
            }
            updateCheckboxAndSaveButton()
        }
    }

    override fun stop() {
    }
}

private fun View.showTotalGrossPrice(selectableLineItem: SelectableLineItem) {
    DigitalInvoice.lineItemTotalGrossPriceIntegralAndFractionalParts(
        selectableLineItem.lineItem
    ).let { (integral, fractional) ->
        showTotalGrossPrice(integral, fractional)
    }
}

private fun View.updateSaveButton(new: SelectableLineItem, old: SelectableLineItem, isPriceValid: Boolean) {
    if (new == old || !isPriceValid) {
        disableSaveButton()
    } else {
        enableSaveButton()
    }
}