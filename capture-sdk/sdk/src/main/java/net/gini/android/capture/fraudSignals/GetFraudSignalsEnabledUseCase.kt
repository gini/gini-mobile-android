package net.gini.android.capture.fraudSignals

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider

internal class GetFraudSignalsEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke() = giniBankConfigurationProvider.provide().isFraudSignalsEnabled
}
