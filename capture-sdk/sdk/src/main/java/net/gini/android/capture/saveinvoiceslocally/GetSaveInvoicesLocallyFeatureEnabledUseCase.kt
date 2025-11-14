package net.gini.android.capture.saveinvoiceslocally

import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider

internal class GetSaveInvoicesLocallyFeatureEnabledUseCase (
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {
    operator fun invoke(): Boolean =
        giniBankConfigurationProvider.provide().isSavePhotosLocallyEnabled
}
