package net.gini.android.capture.education

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider

class GetEducationFeatureEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke(): Boolean =
        giniBankConfigurationProvider.provide().isQrCodeEducationEnabled
}