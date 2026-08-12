package net.gini.android.capture.paymentHints

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider

internal class GetPaymentScheduleHintEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke() = giniBankConfigurationProvider.provide().isPaymentScheduleHintEnabled

}
