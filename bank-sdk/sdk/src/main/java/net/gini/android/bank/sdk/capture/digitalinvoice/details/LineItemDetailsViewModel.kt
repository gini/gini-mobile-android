package net.gini.android.bank.sdk.capture.digitalinvoice.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoice
import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem
import net.gini.android.bank.sdk.capture.digitalinvoice.toPriceString
import net.gini.android.capture.network.model.GiniCaptureReturnReason
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.ParseException
import java.util.UUID

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
internal class LineItemDetailsViewModel(
    selectableLineItem: SelectableLineItem,
    val returnReasons: List<GiniCaptureReturnReason> = emptyList(),
    private val grossPriceFormat: DecimalFormat = DecimalFormat(GROSS_PRICE_FORMAT_PATTERN).apply {
        isParseBigDecimal = true
    }
) : ViewModel() {

    var selectableLineItem: SelectableLineItem = selectableLineItem
        private set

    private val originalLineItem: SelectableLineItem = selectableLineItem.copy()

    private val _uiState = MutableStateFlow(createInitialUiState(selectableLineItem))
    val uiState: StateFlow<LineItemDetailsUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<LineItemDetailsSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<LineItemDetailsSideEffect> = _sideEffects.receiveAsFlow()

    private fun createInitialUiState(selectableLineItem: SelectableLineItem): LineItemDetailsUiState {
        val (integral, fractional) = DigitalInvoice
            .lineItemTotalGrossPriceIntegralAndFractionalParts(selectableLineItem.lineItem)
        return selectableLineItem.lineItem.let { lineItem ->
            LineItemDetailsUiState(
                description = lineItem.description,
                quantity = lineItem.quantity,
                grossPriceDisplay = grossPriceFormat.format(lineItem.grossPrice),
                currencySymbol = lineItem.currency?.symbol ?: "",
                totalGrossPriceIntegralPart = integral,
                totalGrossPriceFractionalPart = fractional,
                checkboxSelected = selectableLineItem.selected,
                checkboxVisible = !selectableLineItem.addedByUser,
                inputEnabled = selectableLineItem.selected,
                saveEnabled = isSaveEnabled(selectableLineItem, lineItem.grossPrice > BigDecimal.ZERO),
            )
        }
    }

    fun selectLineItem() {
        if (selectableLineItem.selected) {
            return
        }
        selectableLineItem.selected = true
        selectableLineItem.reason = null
        updateUiState(inputEnabled = true)
    }

    fun deselectLineItem() {
        if (!selectableLineItem.selected) {
            return
        }
        if (canShowReturnReasonsDialog()) {
            sendSideEffect(LineItemDetailsSideEffect.ShowReturnReasonDialog(returnReasons))
        } else {
            selectableLineItem.selected = false
            selectableLineItem.reason = null
            updateUiState(inputEnabled = false)
        }
    }

    fun onReturnReasonSelected(selectedReason: GiniCaptureReturnReason?) {
        if (selectedReason != null) {
            selectableLineItem.selected = false
            selectableLineItem.reason = selectedReason
            updateUiState(inputEnabled = false)
        } else {
            selectableLineItem.selected = true
            selectableLineItem.reason = null
            updateUiState(inputEnabled = true)
        }
    }

    private fun canShowReturnReasonsDialog() =
        GiniBank.enableReturnReasons && returnReasons.isNotEmpty()

    fun setDescription(description: String) {
        if (selectableLineItem.lineItem.description == description) {
            return
        }
        selectableLineItem = selectableLineItem.copy(
            lineItem = selectableLineItem.lineItem.copy(description = description)
        )
        updateUiState(isPriceValid = selectableLineItem.lineItem.grossPrice > BigDecimal.ZERO)
    }

    fun setQuantity(quantity: Int) {
        if (selectableLineItem.lineItem.quantity == quantity) {
            return
        }

        val validQuantity = quantity.coerceAtLeast(MIN_QUANTITY).coerceAtMost(MAX_QUANTITY)

        selectableLineItem = selectableLineItem.copy(
            lineItem = selectableLineItem.lineItem.copy(quantity = validQuantity)
        )
        updateUiState(isPriceValid = selectableLineItem.lineItem.grossPrice > BigDecimal.ZERO)

        if (validQuantity != quantity) {
            sendSideEffect(LineItemDetailsSideEffect.UpdateQuantityField(validQuantity))
        }
    }

    fun setGrossPrice(displayedGrossPrice: String) {
        val grossPrice = try {
            grossPriceFormat.parse(displayedGrossPrice) as BigDecimal
        } catch (_: ParseException) {
            updateUiState(isPriceValid = false)
            return
        }
        if (selectableLineItem.lineItem.grossPrice == grossPrice) {
            return
        }
        selectableLineItem = selectableLineItem.copy(
            lineItem = selectableLineItem.lineItem.copy(
                rawGrossPrice = grossPrice.toPriceString(selectableLineItem.lineItem.rawCurrency)
            )
        )
        updateUiState(isPriceValid = grossPrice > BigDecimal.ZERO)
    }

    fun save() {
        val savedLineItem = when {
            selectableLineItem.lineItem.id.isBlank() -> {
                selectableLineItem.copy(
                    lineItem = selectableLineItem.lineItem.copy(UUID.randomUUID().toString())
                )
            }

            else -> selectableLineItem
        }
        sendSideEffect(LineItemDetailsSideEffect.Save(savedLineItem))
    }

    fun onQuantityInputFieldFocusLoss(inputFieldText: String) {
        val quantity = inputFieldText.toIntOrNull()
        if (quantity == null || (selectableLineItem.lineItem.quantity != quantity)) {
            sendSideEffect(
                LineItemDetailsSideEffect.UpdateQuantityField(selectableLineItem.lineItem.quantity)
            )
        }
    }

    fun validateLineItemName(name: String): Boolean {
        return name.isNotBlank()
    }

    fun validateLineItemGrossPrice(displayedGrossPrice: String): Boolean {
        return try {
            val grossPrice = grossPriceFormat.parse(displayedGrossPrice) as BigDecimal
            return grossPrice.stripTrailingZeros() != BigDecimal.ZERO
        } catch (_: ParseException) {
            false
        }
    }

    private fun updateUiState(
        inputEnabled: Boolean = _uiState.value.inputEnabled,
        isPriceValid: Boolean = selectableLineItem.lineItem.grossPrice > BigDecimal.ZERO,
    ) {
        val (integral, fractional) = DigitalInvoice
            .lineItemTotalGrossPriceIntegralAndFractionalParts(selectableLineItem.lineItem)
        _uiState.value = _uiState.value.copy(
            description = selectableLineItem.lineItem.description,
            quantity = selectableLineItem.lineItem.quantity,
            totalGrossPriceIntegralPart = integral,
            totalGrossPriceFractionalPart = fractional,
            checkboxSelected = selectableLineItem.selected,
            checkboxVisible = !selectableLineItem.addedByUser,
            inputEnabled = inputEnabled,
            saveEnabled = isSaveEnabled(selectableLineItem, isPriceValid),
        )
    }

    private fun isSaveEnabled(new: SelectableLineItem, isPriceValid: Boolean): Boolean =
        new != originalLineItem && isPriceValid

    private fun sendSideEffect(sideEffect: LineItemDetailsSideEffect) {
        viewModelScope.launch { _sideEffects.send(sideEffect) }
    }
}

/**
 * Internal use only.
 *
 * @suppress
 */
internal data class LineItemDetailsUiState(
    val description: String,
    val quantity: Int,
    val grossPriceDisplay: String,
    val currencySymbol: String,
    val totalGrossPriceIntegralPart: String,
    val totalGrossPriceFractionalPart: String,
    val checkboxSelected: Boolean,
    val checkboxVisible: Boolean,
    val inputEnabled: Boolean,
    val saveEnabled: Boolean,
)

/**
 * Internal use only.
 *
 * @suppress
 */
internal sealed interface LineItemDetailsSideEffect {
    data class ShowReturnReasonDialog(val reasons: List<GiniCaptureReturnReason>) :
        LineItemDetailsSideEffect

    data class UpdateQuantityField(val quantity: Int) : LineItemDetailsSideEffect
    data class Save(val selectableLineItem: SelectableLineItem) : LineItemDetailsSideEffect
}
