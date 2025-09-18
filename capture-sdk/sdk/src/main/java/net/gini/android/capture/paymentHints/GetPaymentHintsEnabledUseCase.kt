package net.gini.android.capture.paymentHints

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider


class GetPaymentHintsEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke() = giniBankConfigurationProvider.provide().paymentHintsEnabled

}
