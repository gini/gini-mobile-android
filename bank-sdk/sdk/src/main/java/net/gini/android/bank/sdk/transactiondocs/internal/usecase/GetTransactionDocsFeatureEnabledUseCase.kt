package net.gini.android.bank.sdk.transactiondocs.internal.usecase

import net.gini.android.bank.sdk.GiniBank
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider

internal class GetTransactionDocsFeatureEnabledUseCase(
    private val giniBankConfigurationProvider: GiniBankConfigurationProvider,
) {

    operator fun invoke(): Boolean =
        giniBankConfigurationProvider.provide().isTransactionDocsEnabled
                && GiniBank.getCaptureConfiguration()?.transactionDocsEnabled == true
}
