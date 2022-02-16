package net.gini.android.bank.sdk.util

import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.sdk.error.AmountParsingException

/**
 * Created by Alpár Szotyori on 15.02.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

internal fun ResolvePaymentInput.parseAmountToBackendFormat() =
    amount.toDoubleOrNull()?.toString()?.let { "$it:EUR" } ?: throw AmountParsingException(amount)