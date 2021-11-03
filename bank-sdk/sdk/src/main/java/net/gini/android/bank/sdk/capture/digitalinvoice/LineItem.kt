package net.gini.android.bank.sdk.capture.digitalinvoice

import android.os.Parcelable
import java.math.BigDecimal
import java.util.*
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Created by Alpar Szotyori on 11.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * The `LineItem` class contains information from a line item extraction.
 */
@Parcelize
class LineItem(
        val id: String,
        val description: String,
        val quantity: Int,
        val rawGrossPrice: String,
        private val origQuantity: Int = quantity,
        private val origRawGrossPrice: String = rawGrossPrice,
) : Parcelable {

    /**
     * The unit price.
     */
    @IgnoredOnParcel
    val grossPrice: BigDecimal

    /**
     * The total unit price. Total unit price = unit price x quantity.
     */
    @IgnoredOnParcel
    val totalGrossPrice: BigDecimal

    /**
     * The original total unit price.
     */
    @IgnoredOnParcel
    val origTotalGrossPrice: BigDecimal

    /**
     * The difference between original and current total unit price.
     */
    @IgnoredOnParcel
    val totalGrossPriceDiff: BigDecimal

    /**
     * The parsed currency.
     */
    @IgnoredOnParcel
    val currency: Currency?

    /**
     * The currency as a string in ISO 4217 format.
     */
    @IgnoredOnParcel
    val rawCurrency: String

    init {
        val (grossPrice, rawCurrency, currency) = try {
            parsePriceString(rawGrossPrice)
        } catch (e: Exception) {
            Triple(BigDecimal.ZERO, "", null)
        }
        this.grossPrice = grossPrice
        this.totalGrossPrice = grossPrice.times(BigDecimal(quantity))
        this.rawCurrency = rawCurrency
        this.currency = currency

        val (origGrossPrice, _, _) = try {
            parsePriceString(origRawGrossPrice)
        } catch (e: Exception) {
            Triple(BigDecimal.ZERO, "", null)
        }
        this.origTotalGrossPrice = origGrossPrice.times(BigDecimal(origQuantity))

        this.totalGrossPriceDiff = totalGrossPrice.subtract(origTotalGrossPrice)
    }

    override fun toString() = "LineItem(id=$id, description=$description, quantity=$quantity, rawGrossPrice=$rawGrossPrice, grossPrice=$grossPrice, totalGrossPrice=$totalGrossPrice, currency=$currency)"

    override fun equals(other: Any?) = other is LineItem
            && id == other.id
            && description == other.description
            && quantity == other.quantity
            && grossPrice == other.grossPrice
            && totalGrossPrice == other.totalGrossPrice
            && rawGrossPrice == other.rawGrossPrice
            && currency == other.currency
            && origQuantity == other.origQuantity
            && origRawGrossPrice == other.origRawGrossPrice
            && origTotalGrossPrice == other.origTotalGrossPrice


    override fun hashCode() = Objects.hash(id, description, quantity, rawGrossPrice, grossPrice,
            totalGrossPrice, currency)

    @JvmSynthetic
    fun copy(
        id: String = this.id, description: String = this.description,
        quantity: Int = this.quantity, rawGrossPrice: String = this.rawGrossPrice,
        origQuantity: Int = this.origQuantity, origRawGrossPrice: String = this.origRawGrossPrice
    ) =
        LineItem(id, description, quantity, rawGrossPrice, origQuantity, origRawGrossPrice)

}