package net.gini.android.capture.wero

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider

/**
 * Internal use only.
 *
 * @suppress
 */
class GetWeroQRCodeScanningEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {

    operator fun invoke(): Boolean =
        giniBankConfigurationProvider.provide().isWeroQRCodeScanningEnabled
}
