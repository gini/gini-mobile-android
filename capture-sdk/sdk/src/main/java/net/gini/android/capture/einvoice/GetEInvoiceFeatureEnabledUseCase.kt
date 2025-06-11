package net.gini.android.capture.einvoice

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
/**
 * Internal use only.
 *
 */
class GetEInvoiceFeatureEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {

    operator fun invoke(): Boolean =
        giniBankConfigurationProvider.provide().isEInvoiceEnabled
}
