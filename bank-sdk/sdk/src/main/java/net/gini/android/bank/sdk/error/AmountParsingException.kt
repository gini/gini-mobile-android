package net.gini.android.bank.sdk.error

/**
 * Created by Alpár Szotyori on 15.02.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

/**
 * Exception thrown when the amount string could not be parsed.
 */
class AmountParsingException(amount: String): Exception("Amount could not be parsed to double: $amount")