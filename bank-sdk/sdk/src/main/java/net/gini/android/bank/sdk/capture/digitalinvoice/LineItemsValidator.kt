package net.gini.android.bank.sdk.capture.digitalinvoice

import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.bank.sdk.capture.digitalinvoice.DigitalInvoiceException.*

/**
 * Created by Alpar Szotyori on 10.03.2020.
 *
 * Copyright (c) 2020 Gini GmbH.
 */

internal typealias Validate = (compoundExtractions: Map<String, GiniCaptureCompoundExtraction>) -> Unit

/**
 * It checks that the compound extractions contain valid line items which can be used to show the return assistant.
 */
class LineItemsValidator {

    companion object {

        /**
         * Checks that the compound extractions contain valid line items.
         *
         * In case it's not valid an appropriate [DigitalInvoiceException] subclass will be thrown.
         *
         * @param compoundExtractions a map of [GiniCaptureCompoundExtraction]s
         * @throws LineItemsMissingException if line items are missing from the compound extractions
         * @throws DescriptionMissingException if description is missing from at least one line item
         * @throws QuantityMissingException if quantity is missing from at least one line item
         * @throws GrossPriceMissingException if gross price is missing from at least one line item
         * @throws MixedCurrenciesException if line items contain more than one currency
         * @throws QuantityParsingException if a line item's quantity field could not be parsed
         * @throws GrossPriceParsingException if a line item's gross price field could not be parsed
         */
        @JvmStatic
        @Throws(LineItemsMissingException::class, DescriptionMissingException::class, QuantityMissingException::class,
                GrossPriceMissingException::class, MixedCurrenciesException::class,
                QuantityParsingException::class, GrossPriceParsingException::class)
        fun validate(compoundExtractions: Map<String, GiniCaptureCompoundExtraction>) = listOf(
                lineItemsAvailable,
                descriptionAvailable,
                quantityAvailable,
                grossPriceAvailable,
                quantityParcelable,
                grossPriceParcelable,
                singleCurrency
        ).forEach { it(compoundExtractions) }
    }
}

internal val lineItemsAvailable: Validate = { compoundExtractions ->
    if (!compoundExtractions.containsKey("lineItems")) {
        throw LineItemsMissingException()
    }
}

internal val descriptionAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["lineItems"]?.specificExtractionMaps?.all { it.containsKey("description") }) != true) {
        throw DescriptionMissingException()
    }
}

internal val quantityAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["lineItems"]?.specificExtractionMaps?.all { it.containsKey("quantity") }) != true) {
        throw QuantityMissingException()
    }
}

internal val grossPriceAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["lineItems"]?.specificExtractionMaps?.all { it.containsKey("baseGross") }) != true) {
        throw GrossPriceMissingException()
    }
}

internal val articleNumberAvailable: Validate = { compoundExtractions ->
    if ((compoundExtractions["lineItems"]?.specificExtractionMaps?.all { it.containsKey("artNumber") }) != true) {
        throw ArticleNumberMissingException()
    }
}

internal val quantityParcelable: Validate = { compoundExtractions ->
    if ((compoundExtractions["lineItems"]?.specificExtractionMaps?.all { it["quantity"]?.value?.toIntOrNull() != null }) != true) {
        throw QuantityParsingException()
    }
}

internal val grossPriceParcelable: Validate = { compoundExtractions ->
    compoundExtractions["lineItems"]?.specificExtractionMaps?.forEach { map ->
        map["baseGross"]?.value?.let { grossPriceString ->
            try {
                parsePriceString(grossPriceString)
            } catch (e: Exception) {
                throw GrossPriceParsingException(cause = e)
            }
        } ?: throw GrossPriceParsingException(cause = GrossPriceMissingException())
    } ?: throw GrossPriceParsingException(cause = LineItemsMissingException())
}

internal val singleCurrency: Validate = { compoundExtractions ->
    compoundExtractions["lineItems"]?.specificExtractionMaps?.let { lineItemRows ->
        if (lineItemRows.isEmpty()) {
            throw MixedCurrenciesException(cause = LineItemsMissingException())
        } else {
            val firstCurrency = try {
                lineItemRows[0]["baseGross"]?.value?.let { grossPriceString ->
                    val (_, _, currency) = parsePriceString(grossPriceString)
                    currency
                }
            } catch (e: Exception) {
                throw MixedCurrenciesException(cause = GrossPriceParsingException(cause = e))
            }
            if (firstCurrency == null) {
                throw MixedCurrenciesException(cause = GrossPriceMissingException())
            } else {
                val sameCurrency = lineItemRows.subList(1, lineItemRows.size).fold(true, { sameCurrency, row ->
                    val currency = try {
                        row["baseGross"]?.value?.let { grossPriceString ->
                            val (_, _, currency) = parsePriceString(grossPriceString)
                            currency
                        }
                    } catch (e: Exception) {
                        throw MixedCurrenciesException(cause = GrossPriceParsingException(cause = e))
                    }
                    sameCurrency && (firstCurrency == currency)
                })
                if (!sameCurrency) {
                    throw MixedCurrenciesException()
                }
            }
        }
    } ?: throw MixedCurrenciesException(cause = LineItemsMissingException())
}